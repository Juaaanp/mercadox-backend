package com.IngSoftwarelll.mercadox.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.IngSoftwarelll.mercadox.dtos.EntityCreatedResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.purchases.requests.CreatePurchaseItemRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.purchases.requests.CreatePurchaseRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.purchases.responses.PurchaseResponseDTO;
import com.IngSoftwarelll.mercadox.exceptions.BusinessException;
import com.IngSoftwarelll.mercadox.exceptions.InsufficientFundsException;
import com.IngSoftwarelll.mercadox.exceptions.InsufficientStockException;
import com.IngSoftwarelll.mercadox.mappers.PurchaseMapper;
import com.IngSoftwarelll.mercadox.models.Product;
import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.models.ProductStock;
import com.IngSoftwarelll.mercadox.models.Purchase;
import com.IngSoftwarelll.mercadox.models.PurchaseItem;
import com.IngSoftwarelll.mercadox.models.enums.PurchaseItemStatus;
import com.IngSoftwarelll.mercadox.models.enums.PurchaseStatus;
import com.IngSoftwarelll.mercadox.repositories.ProductRepository;
import com.IngSoftwarelll.mercadox.repositories.ProductStockRepository;
import com.IngSoftwarelll.mercadox.repositories.PurchaseRepository;
import com.IngSoftwarelll.mercadox.services.interfaces.EmailService;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductService;
import com.IngSoftwarelll.mercadox.services.interfaces.PurchaseService;
import com.IngSoftwarelll.mercadox.services.interfaces.UserService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final ProductStockRepository productStockRepository;
    private final EmailService emailService;
    private final PurchaseMapper purchaseMapper;

    private static final Logger log = LoggerFactory.getLogger(PurchaseServiceImpl.class);

    @Override
    public Purchase getPurchaseById(Long purchaseId) {
        if (purchaseId == null || purchaseId <= 0) {
            throw new IllegalArgumentException("Purchase id must be positive");
        }

        return purchaseRepository.findById(purchaseId).orElseThrow(
                () -> new ObjectNotFoundException("Purchase with id:" + purchaseId + " not found", Purchase.class));
    }

    @Override
    @SuppressWarnings("null")
    public EntityCreatedResponseDTO createPurchase(Long userId, CreatePurchaseRequestDTO request) {

        log.info("Validating request items size...");
        if (request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Purchase must have at least one item");
        }

        String referenceId = UUID.randomUUID().toString();

        log.info("Creating purchase for user: {}", userId);

        // 1. Crear compra
        Purchase basePurchase = createBasePurchase(referenceId, userId, request);

        // 2. guardamos el cuerpo de la compra para generar ID
        Purchase savedBasePurchase = purchaseRepository.save(basePurchase);

        // 3. Adicionar items
        Purchase purchaseWithItems = addPurchaseItems(savedBasePurchase, request);

        // 4. Calcular total de la compra
        purchaseWithItems.calculateTotals();

        // 5. Validar saldo del comprador
        User user = userService.getUserById(userId);

        if (user.getBalance().compareTo(purchaseWithItems.getTotal()) < 0) {
            releaseCodes(purchaseWithItems);
            throw new InsufficientFundsException();
        }

        // 6. Procesar pago
        try {

            log.debug("Debiting {} from buyer balance {}", purchaseWithItems.getTotal(), user.getId());
            user.subtractBalance(purchaseWithItems.getTotal());
            purchaseWithItems.setStatus(PurchaseStatus.COMPLETED);

        } catch (Exception e) {
            purchaseWithItems.setStatus(PurchaseStatus.FAILED);

            // ⭐ LIBERAR LOS CÓDIGOS
            releaseCodes(purchaseWithItems);

            throw e;
        }

        // Registrar la fecha en la que la compra se completó
        purchaseWithItems.setCompletedAt(LocalDateTime.now());
        
        Purchase finalPurchase = purchaseRepository.save(purchaseWithItems);

        log.info("Contact Email (DTO sent):" + request.getContactEmail());

        // Envio de codigos de productos por correo al comprador (emailService)
        emailService.sendPurchaseConfirmation(finalPurchase, request.getContactEmail());

        log.info("Purchase created succesfully. id: {}, Total: {}", finalPurchase.getId(),
                finalPurchase.getTotal());

        return new EntityCreatedResponseDTO(finalPurchase.getId(), "Purchase registered succesfully", Instant.now());
    }

    // Metodos privados para crear una compra
    private Purchase createBasePurchase(String referenceId, Long userId, CreatePurchaseRequestDTO request) {
        return Purchase.builder()
                .referenceId(referenceId)
                .user(userService.getUserById(userId))
                .status(PurchaseStatus.PENDING)
                .contactEmail(request.getContactEmail())
                .build();
    }

    private Purchase addPurchaseItems(Purchase savedPurchase, CreatePurchaseRequestDTO request) {

        for (CreatePurchaseItemRequestDTO itemRequest : request.getItems()) {
            Product product = productService.getById(itemRequest.getProductId());

            // Validar stock
            if (product.getAvailableStock() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(product.getName());
            }

            for (int i = 0; i < itemRequest.getQuantity(); i++) {
                ProductStock availableCode = productStockRepository.findNextAvailableForProduct(product.getId())
                        .orElseThrow(() -> new InsufficientStockException(product.getName()));

                availableCode.markAsReserved();
                productStockRepository.saveAndFlush(availableCode);
                // Crear item
                PurchaseItem purchaseItem = PurchaseItem.builder()
                        .purchase(savedPurchase)
                        .product(product)
                        .quantity(1)
                        .priceAtPurchase(product.getPrice())
                        .status(PurchaseItemStatus.PENDING)
                        .build();

                // Asignar código
                purchaseItem.assignProductStock(availableCode);

                // Agregar a la compra (esto lo agrega a la lista)
                savedPurchase.addItem(purchaseItem);
            }

            product.updateStockCount();
            productRepository.save(product);

        }

        int expectedItems = request.getItems().stream().mapToInt(CreatePurchaseItemRequestDTO::getQuantity).sum();

        if (savedPurchase.getItems().size() != expectedItems) {
            throw new BusinessException("Purchase incompleted");
        }

        savedPurchase = purchaseRepository.save(savedPurchase);

        for (PurchaseItem item : savedPurchase.getItems()) {
            if (item.getAssignedProductStock() != null) {
                ProductStock stock = item.getAssignedProductStock();
                stock.markAsSold(item);
                productStockRepository.save(stock);
            }
        }

        return savedPurchase;
    }

    private void releaseCodes(Purchase purchaseWithItems) {
        for (PurchaseItem item : purchaseWithItems.getItems()) {
            if (item.getAssignedProductStock() != null) {
                item.getAssignedProductStock().markAsAvailable();
                productStockRepository.save(Objects.requireNonNull(item.getAssignedProductStock()));
            }

            // Restaurar stock del producto
            Product product = item.getProduct();
            product.updateStockCount();
            productRepository.save(product);
        }
    }

    // Fin de metodos para procesar una compra
    // Fin de metodos para crear una compra

    @Override
    public Page<PurchaseResponseDTO> getUserPurchases(Long userId, Pageable pageable) {
        Page<Purchase> purchases = purchaseRepository.findByUserId(userId, pageable);
        Page<PurchaseResponseDTO> purchasesDtos = purchases.map(purchaseMapper::toPurchaseResponseDTO);
        return purchasesDtos;
    }

}
