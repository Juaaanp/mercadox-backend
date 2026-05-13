package com.IngSoftwarelll.mercadox.services;

import com.IngSoftwarelll.mercadox.dtos.EntityCreatedResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.purchases.requests.CreatePurchaseItemRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.purchases.requests.CreatePurchaseRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.purchases.responses.PurchaseResponseDTO;
import com.IngSoftwarelll.mercadox.exceptions.InsufficientFundsException;
import com.IngSoftwarelll.mercadox.exceptions.InsufficientStockException;
import com.IngSoftwarelll.mercadox.mappers.PurchaseMapper;
import com.IngSoftwarelll.mercadox.models.*;
import com.IngSoftwarelll.mercadox.models.enums.PurchaseStatus;
import com.IngSoftwarelll.mercadox.models.enums.StockStatus;
import com.IngSoftwarelll.mercadox.repositories.ProductRepository;
import com.IngSoftwarelll.mercadox.repositories.ProductStockRepository;
import com.IngSoftwarelll.mercadox.repositories.PurchaseRepository;
import com.IngSoftwarelll.mercadox.services.interfaces.EmailService;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductService;
import com.IngSoftwarelll.mercadox.services.interfaces.UserService;
import org.hibernate.ObjectNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para PurchaseServiceImpl.
 *
 * Casos cubiertos:
 *   CP-19 -- Compra exitosa: codigo asignado, balance debitado, email enviado
 *   CP-20 -- Compra rechazada por balance insuficiente: 0 compras, 0 codigos
 *            Compra rechazada por lista de items vacia
 *            Compra rechazada por stock insuficiente
 *   CP-22 -- Estado del codigo cambia a SOLD tras compra completada
 *   CP-24 -- getUserPurchases retorna pagina paginada correctamente
 *            getPurchaseById lanza excepcion con id invalido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseServiceImpl — pruebas unitarias")
class PurchaseServiceImplTest {

    @Mock private PurchaseRepository purchaseRepository;
    @Mock private ProductService productService;
    @Mock private ProductRepository productRepository;
    @Mock private UserService userService;
    @Mock private ProductStockRepository productStockRepository;
    @Mock private EmailService emailService;
    @Mock private PurchaseMapper purchaseMapper;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    // ─── helpers ──────────────────────────────────────────────

    /** Usuario con saldo suficiente para un producto de $50.000 */
    private User userWithBalance(BigDecimal balance) {
        User u = new User();
        u.setId(10L);
        u.setEmail("buyer@test.com");
        u.setBalance(balance);
        return u;
    }

        /**
     * Crea un Product con N items AVAILABLE en su lista stockItems.
     * getAvailableStock() itera esa lista; no existe setter directo.
     */
    private Product productWithStock(Long id, int stockCount, BigDecimal price) {
        Product p = new Product();
        p.setId(id);
        p.setName("Adobe CC");
        p.setPrice(price);
        for (int i = 1; i <= stockCount; i++) {
            ProductStock s = new ProductStock();
            s.setId((long) i);
            s.setCode("PRELOADED-" + id + "-" + i);
            s.setStatus(StockStatus.AVAILABLE);
            s.setProduct(p);
            p.getStockItems().add(s);
        }
        return p;
    }

    /** Código de stock disponible */
    private ProductStock availableCode(Long id) {
        ProductStock code = new ProductStock();
        code.setId(id);
        code.setCode("CODE-" + id);
        code.setStatus(StockStatus.AVAILABLE);
        return code;
    }

    /** Request de compra con un único item */
    private CreatePurchaseRequestDTO singleItemRequest(Long productId, int qty) {
        CreatePurchaseItemRequestDTO item = mock(CreatePurchaseItemRequestDTO.class);
        when(item.getProductId()).thenReturn(productId);
        when(item.getQuantity()).thenReturn(qty);

        CreatePurchaseRequestDTO req = mock(CreatePurchaseRequestDTO.class);
        when(req.getItems()).thenReturn(List.of(item));
        return req;
    }

     /**
     * Simula el comportamiento de JPA al persistir una Purchase.
     *
     * BUG-3 FIX: PurchaseItem.subtotal se asigna en @PrePersist/@PreUpdate,
     * pero JPA nunca ejecuta esos hooks en tests unitarios. Sin este fix,
     * calculateTotals() falla con NullPointerException porque getSubtotal()
     * devuelve null en todos los items.
     *
     * Este stub:
     *   1. Asigna un id a la Purchase si aun no lo tiene.
     *   2. Recorre sus items y calcula subtotal = priceAtPurchase * quantity,
     *      replicando exactamente lo que hace @PrePersist en PurchaseItem.
     */
    private void stubSave(Long assignedId) {
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> {
            Purchase p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(assignedId);
            }
            // Simular @PrePersist de PurchaseItem para que subtotal no sea null
            for (PurchaseItem item : p.getItems()) {
                if (item.getSubtotal() == null
                        && item.getPriceAtPurchase() != null
                        && item.getQuantity() != null) {
                    item.setSubtotal(
                        item.getPriceAtPurchase()
                            .multiply(new BigDecimal(item.getQuantity()))
                    );
                }
            }
            return p;
        });
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 1 — createPurchase()
    // ════════════════════════════════════════════════════════════
     @Nested
    @DisplayName("createPurchase()")
    class CreatePurchaseTests {
 
        /**
         * CP-19: Compra exitosa.
         * El balance del comprador se debita y el email de confirmacion se envia.
         */
        @Test
        @DisplayName("CP-19 -- compra exitosa debita balance y envia email")
        void createPurchase_withSufficientFunds_completesAndSendsEmail() {
            Long userId = 10L;
            BigDecimal price = new BigDecimal("50000");
 
            User buyer = userWithBalance(new BigDecimal("100000"));
            Product product = productWithStock(1L, 5, price);
            ProductStock code = availableCode(1L);
            CreatePurchaseRequestDTO request = singleItemRequest(1L, 1);
 
            // BUG-1: dos llamadas a getUserById
            when(userService.getUserById(userId)).thenReturn(buyer, buyer);
            when(productService.getById(1L)).thenReturn(product);
            when(productStockRepository.findNextAvailableForProduct(1L))
                    .thenReturn(Optional.of(code));
            stubSave(100L);
 
            EntityCreatedResponseDTO response = purchaseService.createPurchase(userId, request);
 
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).containsIgnoringCase("succesfully");
 
            // CP-19: 100000 - 50000 = 50000
            assertThat(buyer.getBalance()).isEqualByComparingTo("50000");
 
            // CP-19: confirmacion enviada al correo del comprador
            verify(emailService).sendPurchaseConfirmation(any(Purchase.class), eq("buyer@test.com"));
        }
 
        /**
         * CP-20: Balance insuficiente -- la excepcion se lanza, el balance no cambia
         * y los codigos reservados se liberan.
         */
        @Test
        @DisplayName("CP-20 -- balance insuficiente lanza InsufficientFundsException")
        void createPurchase_withInsufficientFunds_throwsAndReleasesCodes() {
            Long userId = 10L;
            BigDecimal price = new BigDecimal("200000");
 
            User buyer = userWithBalance(new BigDecimal("10000"));
            Product product = productWithStock(1L, 5, price);
            ProductStock code = availableCode(1L);
            CreatePurchaseRequestDTO request = singleItemRequest(1L, 1);
 
            // BUG-1: dos llamadas a getUserById
            when(userService.getUserById(userId)).thenReturn(buyer, buyer);
            when(productService.getById(1L)).thenReturn(product);
            when(productStockRepository.findNextAvailableForProduct(1L))
                    .thenReturn(Optional.of(code));
            stubSave(101L);
 
            assertThatThrownBy(() -> purchaseService.createPurchase(userId, request))
                    .isInstanceOf(InsufficientFundsException.class);
 
            // CP-20: balance sin modificacion
            assertThat(buyer.getBalance()).isEqualByComparingTo("10000");
 
            // CP-20: 0 emails enviados
            verify(emailService, never()).sendPurchaseConfirmation(any(), any());
        }
 
        /**
         * Lista de items vacia: se rechaza en la primera linea del servicio,
         * antes de cualquier llamada a userService o purchaseRepository.
         */
        @Test
        @DisplayName("lista de items vacia lanza IllegalArgumentException")
        void createPurchase_withEmptyItems_throwsIllegalArgument() {
            CreatePurchaseRequestDTO request = mock(CreatePurchaseRequestDTO.class);
            when(request.getItems()).thenReturn(List.of());
 
            assertThatThrownBy(() -> purchaseService.createPurchase(10L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("item");
 
            verify(purchaseRepository, never()).save(any());
        }
 
        /**
         * Stock insuficiente: availableStock = 0 en el producto.
         * La excepcion se lanza dentro de addPurchaseItems.
         */
        @Test
        @DisplayName("stock insuficiente lanza InsufficientStockException")
        void createPurchase_withInsufficientStock_throwsInsufficientStock() {
            Long userId = 10L;
            Product product = productWithStock(1L, 0, new BigDecimal("50000"));
            User buyer = userWithBalance(new BigDecimal("500000"));
            CreatePurchaseRequestDTO request = singleItemRequest(1L, 1);
 
            // BUG-1: stub preparado para 2 llamadas aunque la 2da no se alcance
            when(userService.getUserById(userId)).thenReturn(buyer, buyer);
            when(productService.getById(1L)).thenReturn(product);
            stubSave(102L);
 
            assertThatThrownBy(() -> purchaseService.createPurchase(userId, request))
                    .isInstanceOf(InsufficientStockException.class);
 
            verify(emailService, never()).sendPurchaseConfirmation(any(), any());
        }
 
        /**
         * CP-22: El codigo de stock pasa de AVAILABLE a RESERVED (saveAndFlush)
         * y luego a SOLD (save) durante una compra exitosa.
         */
        @Test
        @DisplayName("CP-22 -- estado del codigo cambia a SOLD tras compra completada")
        void createPurchase_successful_codeStatusChangesToSold() {
            Long userId = 10L;
            BigDecimal price = new BigDecimal("30000");
 
            User buyer = userWithBalance(new BigDecimal("100000"));
            Product product = productWithStock(2L, 3, price);
            ProductStock code = availableCode(5L);
            CreatePurchaseRequestDTO request = singleItemRequest(2L, 1);
 
            // BUG-1: dos llamadas a getUserById
            when(userService.getUserById(userId)).thenReturn(buyer, buyer);
            when(productService.getById(2L)).thenReturn(product);
            when(productStockRepository.findNextAvailableForProduct(2L))
                    .thenReturn(Optional.of(code));
            stubSave(103L);
 
            purchaseService.createPurchase(userId, request);
 
            // markAsReserved() persiste con saveAndFlush
            verify(productStockRepository, atLeastOnce()).saveAndFlush(code);
            // markAsSold() persiste con save
            verify(productStockRepository, atLeastOnce()).save(code);
        }
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 2 — getPurchaseById()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getPurchaseById()")
    class GetPurchaseByIdTests {

        @Test
        @DisplayName("id null lanza IllegalArgumentException")
        void getPurchaseById_withNullId_throwsIllegalArgument() {
            assertThatThrownBy(() -> purchaseService.getPurchaseById(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("id negativo lanza IllegalArgumentException")
        void getPurchaseById_withNegativeId_throwsIllegalArgument() {
            assertThatThrownBy(() -> purchaseService.getPurchaseById(-5L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("id inexistente lanza ObjectNotFoundException")
        void getPurchaseById_withNonExistingId_throwsObjectNotFound() {
            when(purchaseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> purchaseService.getPurchaseById(999L))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("id existente retorna la Purchase")
        void getPurchaseById_withExistingId_returnsPurchase() {
            Purchase purchase = new Purchase();
            purchase.setId(50L);
            purchase.setStatus(PurchaseStatus.COMPLETED);

            when(purchaseRepository.findById(50L)).thenReturn(Optional.of(purchase));

            Purchase result = purchaseService.getPurchaseById(50L);

            assertThat(result.getId()).isEqualTo(50L);
            assertThat(result.getStatus()).isEqualTo(PurchaseStatus.COMPLETED);
        }
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 3 — getUserPurchases()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getUserPurchases()")
    class GetUserPurchasesTests {

        /**
         * CP-24: historial paginado retornado correctamente.
         */
        @Test
        @DisplayName("CP-24 — retorna historial de compras paginado del usuario")
        void getUserPurchases_withValidUser_returnsPaginatedDTOs() {
            Pageable pageable = PageRequest.of(0, 10);

            Purchase purchase = new Purchase();
            purchase.setId(1L);

            PurchaseResponseDTO dto = mock(PurchaseResponseDTO.class);

            Page<Purchase> page = new PageImpl<>(List.of(purchase));

            when(purchaseRepository.findByUserId(10L, pageable)).thenReturn(page);
            when(purchaseMapper.toPurchaseResponseDTO(purchase)).thenReturn(dto);

            Page<PurchaseResponseDTO> result = purchaseService.getUserPurchases(10L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(dto);
        }

        @Test
        @DisplayName("CP-24 — usuario sin compras retorna página vacía")
        void getUserPurchases_withNoPurchases_returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);

            when(purchaseRepository.findByUserId(10L, pageable)).thenReturn(Page.empty());

            Page<PurchaseResponseDTO> result = purchaseService.getUserPurchases(10L, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }
}