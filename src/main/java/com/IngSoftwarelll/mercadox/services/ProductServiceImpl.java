package com.IngSoftwarelll.mercadox.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import com.IngSoftwarelll.mercadox.exceptions.ResourceNotFoundException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.IngSoftwarelll.mercadox.dtos.EntityCreatedResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.requests.CreateProductRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductSummaryResponseDTO;
import com.IngSoftwarelll.mercadox.exceptions.UnauthorizedException;
import com.IngSoftwarelll.mercadox.mappers.ProductMapper;
import com.IngSoftwarelll.mercadox.models.Product;
import com.IngSoftwarelll.mercadox.models.ProductStock;
import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.models.enums.UserRole;
import com.IngSoftwarelll.mercadox.repositories.ProductRepository;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductCategoryService;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductService;
import com.IngSoftwarelll.mercadox.services.interfaces.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final UserService userService;
    private final ProductCategoryService productCategoryService;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    public EntityCreatedResponseDTO createProduct(Long adminId, CreateProductRequestDTO request, MultipartFile image) {
        User admin = userService.getUserById(adminId);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Only admins can create products");
        }

        String imageUrl = cloudinaryService.uploadImage(image);

        Product product = productMapper.toProduct(request);
        product.setAdmin(admin);
        product.setImageUrl(imageUrl);
        product.setProductCategory(productCategoryService.getById(request.getProductCategoryId()));

        for (ProductStock code : product.getStockItems()) {
            code.setProduct(product);
        }

        productRepository.save(product);

        return new EntityCreatedResponseDTO(product.getId(), "Product created Succesfully", Instant.now());
    }

    @Override
    public void deleteProduct(Long adminId, Long productId) {
        User admin = userService.getUserById(adminId);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Only admins can delete products");
        }

        productRepository.deleteById(productId);
    }

    @Override
    public Product getById(Long productId) {

        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Product id must be positive");
        }

        return productRepository.findById(productId).orElseThrow(
                () -> new ResourceNotFoundException("Product with id: " + productId + " not found"));
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
                () -> new ResourceNotFoundException("Product with id: " + productId + " not found"));
        ProductResponseDTO response = productMapper.toProductResponseDTO(product);
        return response;
    }

   

}
