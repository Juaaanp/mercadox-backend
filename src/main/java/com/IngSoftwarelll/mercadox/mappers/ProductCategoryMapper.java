package com.IngSoftwarelll.mercadox.mappers;

import org.mapstruct.Mapper;

import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductCategoryResponseDTO;
import com.IngSoftwarelll.mercadox.models.ProductCategory;


@Mapper(componentModel = "spring")
public interface ProductCategoryMapper {
    
    ProductCategoryResponseDTO toProductCategoryResponseDTO (ProductCategory productCategory);
    
}
