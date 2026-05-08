package com.IngSoftwarelll.mercadox.controllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.IngSoftwarelll.mercadox.dtos.EntityCreatedResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.requests.CreateProductRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.products.requests.CreateProductStockRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.BulkStockResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductSummaryResponseDTO;
import com.IngSoftwarelll.mercadox.security.CustomUserDetails;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductService;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductStockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ProductStockService productStockService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EntityCreatedResponseDTO> createProduct(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart("product") @Valid CreateProductRequestDTO request,
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(productService.createProduct(user.getId(), request, image));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long productId) {
        productService.deleteProduct(user.getId(), productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Eliminar un código de stock específico
     */
    @DeleteMapping("/{productId}/stock/{stockId}")
    public ResponseEntity<Void> deleteStockItem(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long productId,
            @PathVariable Long stockId) {
        productStockService.deleteStockItem(productId, stockId, user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Agregar múltiples códigos de stock de una vez
     */
    @PostMapping("/{productId}/stock/bulk")
    public ResponseEntity<BulkStockResponseDTO> addBulkStockItems(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long productId,
            @RequestBody @Valid List<CreateProductStockRequestDTO> requests) {

        BulkStockResponseDTO response = productStockService.addBulkStockItems(productId, user.getId(), requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cargar productos
     */
    @GetMapping
    public ResponseEntity<Page<ProductSummaryResponseDTO>> loadProducts(
            @RequestParam(defaultValue = "0") Integer page) {
        return ResponseEntity.ok(productService.getAllProducts(page));
    }

    /**
     * Buscar productos con filtros
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<ProductSummaryResponseDTO>> searchProducts(
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal maxPrice, @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Page<ProductSummaryResponseDTO> response = productService.filterProducts(searchQuery, categoryId,
                maxPrice, page, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener la vista detallada de un producto
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDTO> getProductDetail(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.detailProduct(productId));
    }

}
