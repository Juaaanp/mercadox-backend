package com.IngSoftwarelll.mercadox.services.interfaces;

import java.util.List;

import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductCategoryResponseDTO;
import com.IngSoftwarelll.mercadox.models.ProductCategory;

public interface ProductCategoryService {

    ProductCategory getById (Long productCategoryId);
    List<ProductCategoryResponseDTO> getProductCategories();
    
}
