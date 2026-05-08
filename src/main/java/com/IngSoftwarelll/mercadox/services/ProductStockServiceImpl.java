package com.IngSoftwarelll.mercadox.services;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.IngSoftwarelll.mercadox.dtos.products.requests.CreateProductStockRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.BulkStockResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductStockResponseDTO;
import com.IngSoftwarelll.mercadox.mappers.ProductStockMapper;
import com.IngSoftwarelll.mercadox.models.Product;
import com.IngSoftwarelll.mercadox.models.ProductStock;
import com.IngSoftwarelll.mercadox.models.enums.StockStatus;
import com.IngSoftwarelll.mercadox.repositories.ProductRepository;
import com.IngSoftwarelll.mercadox.repositories.ProductStockRepository;
import com.IngSoftwarelll.mercadox.exceptions.ResourceNotFoundException;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductStockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStockServiceImpl implements ProductStockService {

    private final ProductStockRepository productStockRepository;
    private final ProductRepository productRepository;
    private final ProductStockMapper productStockMapper;

    @Override
    public Page<ProductStockResponseDTO> getProductStock(Long productId, Long adminId, String search,
            StockStatus status, Pageable pageable) {

        productRepository.findByIdAndAdminId(productId, adminId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id: " + productId + " not found for this admin"));

        Page<ProductStock> stockPage;

        if (search != null && !search.isEmpty() && status != null) {
            stockPage = productStockRepository.findByProductIdAndCodeContainingIgnoreCaseAndStatus(
                    productId, search, status, pageable);
        } else if (search != null && !search.isEmpty()) {
            stockPage = productStockRepository.findByProductIdAndCodeContainingIgnoreCase(
                    productId, search, pageable);
        } else if (status != null) {
            stockPage = productStockRepository.findByProductIdAndStatus(
                    productId, status, pageable);
        } else {
            stockPage = productStockRepository.findByProductId(productId, pageable);
        }

        return stockPage.map(productStockMapper::toProductStockResponseDTO);
    }


    @Override
    public void deleteStockItem(Long productId, Long stockId, Long adminId) {

        ProductStock stock = productStockRepository
            .findByIdAndProductIdAndProductAdminId(stockId, productId, adminId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Stock item with id: " + stockId + " not found for this product and admin"));

        // No permitir eliminar si ya está vendido
        if (stock.getStatus() == StockStatus.SOLD) {
            throw new IllegalStateException("Cannot delete a sold stock item");
        }

        productStockRepository.delete(stock);
    }

    @Override
    public BulkStockResponseDTO addBulkStockItems(Long productId, Long adminId,
            List<CreateProductStockRequestDTO> requests) {
        
        Product product = productRepository.findByIdAndAdminId(productId, adminId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Product with id: " + productId + " not found for this admin"));

        int successful = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (CreateProductStockRequestDTO request : requests) {
            try {
                if (productStockRepository.existsByProductIdAndCode(productId, request.getCode())) {
                    errors.add("Code '" + request.getCode() + "' already exists");
                    failed++;
                    continue;
                }

                ProductStock stock = productStockMapper.toProductStock(request);
                stock.setProduct(product);
                stock.setStatus(StockStatus.AVAILABLE);
                stock.setCreatedAt(ZonedDateTime.now());
                productStockRepository.save(stock);
                successful++;

            } catch (Exception e) {
                errors.add("Error adding code '" + request.getCode() + "': " + e.getMessage());
                failed++;
            }
        }

        return new BulkStockResponseDTO(
            requests.size(),
            successful,
            failed,
            errors,
            ZonedDateTime.now()
        );
    }
}
