package com.IngSoftwarelll.mercadox.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductCategoryResponseDTO;
import com.IngSoftwarelll.mercadox.mappers.ProductCategoryMapper;
import com.IngSoftwarelll.mercadox.models.ProductCategory;
import com.IngSoftwarelll.mercadox.repositories.ProductCategoryRepository;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductCategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService{

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductCategoryMapper productCategoryMapper;

    @Override
    public List<ProductCategoryResponseDTO> getProductCategories() {
        List<ProductCategory> categories = productCategoryRepository.findProductCategories();
        return categories.stream().map(productCategoryMapper::toProductCategoryResponseDTO).toList();
    }
    
}
