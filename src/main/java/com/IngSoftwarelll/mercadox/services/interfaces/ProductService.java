package com.IngSoftwarelll.mercadox.services.interfaces;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.IngSoftwarelll.mercadox.dtos.EntityCreatedResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.requests.CreateProductRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductSummaryResponseDTO;
import com.IngSoftwarelll.mercadox.models.Product;

public interface ProductService {
    Product getById(Long productId);
    EntityCreatedResponseDTO createProduct(Long adminId, CreateProductRequestDTO request, MultipartFile image);
    void deleteProduct (Long adminId, Long productId);
    Page<ProductSummaryResponseDTO> getAllProducts(Integer page);
    Page<ProductSummaryResponseDTO> filterProducts(String searchQuery, Long categoryId,
            BigDecimal maxPrice, Integer page,
            String sortBy, String sortDirection);
    ProductResponseDTO detailProduct(Long productId);
}
