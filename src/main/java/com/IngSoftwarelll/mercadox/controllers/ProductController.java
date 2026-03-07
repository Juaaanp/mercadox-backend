package com.IngSoftwarelll.mercadox.controllers;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductSummaryResponseDTO;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

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
