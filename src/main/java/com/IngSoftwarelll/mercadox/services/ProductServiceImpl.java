package com.IngSoftwarelll.mercadox.services;

import java.math.BigDecimal;
import java.util.Set;

import org.hibernate.ObjectNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductSummaryResponseDTO;
import com.IngSoftwarelll.mercadox.mappers.ProductMapper;
import com.IngSoftwarelll.mercadox.models.Product;
import com.IngSoftwarelll.mercadox.repositories.ProductRepository;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private final ProductMapper productMapper;

    @Override
    public Product getById(Long productId) {

        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Product id must be positive");
        }

        return productRepository.findById(productId).orElseThrow(
                () -> new ObjectNotFoundException("Product with id: " + productId + " not found", Product.class));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponseDTO> getAllProducts(Integer page) {
        Pageable pageable = PageRequest.of(page, 20, Direction.DESC, "createdAt");
        Page<Product> products = productRepository.findAllProducts(pageable);
        return products.map(productMapper::toProductSummaryResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponseDTO> filterProducts(String searchQuery, Long categoryId,
            BigDecimal maxPrice, Integer page,
            String sortBy, String sortDirection) {

        String sortField = validateAndGetSortField(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortField);

        int indexPage = (page != null && page >= 0) ? page : 0;
        Pageable pageable = PageRequest.of(indexPage, 20, sort);

        Page<Product> productPage = productRepository.searchProducts(searchQuery, categoryId, maxPrice,
                        pageable);

        return productPage.map(productMapper::toProductSummaryResponseDTO);

    }

    private String validateAndGetSortField(String sortBy) {
        Set<String> allowedFields = Set.of(
                "price",
                "createdAt");

        if (sortBy != null && allowedFields.contains(sortBy)) {
            return sortBy;
        }

        return "createdAt";
    }

    @Override
    public ProductResponseDTO detailProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("the product id must be positive");
        }
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new ObjectNotFoundException("the product with id: " + productId + " not found", Product.class));
        ProductResponseDTO response = productMapper.toProductResponseDTO(product);
        return response;
    }

}
