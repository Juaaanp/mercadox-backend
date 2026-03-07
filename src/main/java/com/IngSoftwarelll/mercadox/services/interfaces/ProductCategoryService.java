package com.IngSoftwarelll.mercadox.services.interfaces;

import java.util.List;

import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductCategoryResponseDTO;

public interface ProductCategoryService {

    List<ProductCategoryResponseDTO> getProductCategories();
    
}
