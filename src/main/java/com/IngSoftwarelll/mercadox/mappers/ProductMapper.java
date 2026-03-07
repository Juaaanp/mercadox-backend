package com.IngSoftwarelll.mercadox.mappers;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductSummaryResponseDTO;
import com.IngSoftwarelll.mercadox.models.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // ===== MAPPING to ProductResponseDTO (completed) =====
    @Mapping(target = "categoryName", source = "productCategory.name")
    ProductResponseDTO toProductResponseDTO(Product product);

    @Mapping(target = "categoryName", source = "productCategory.name")
    ProductSummaryResponseDTO toProductSummaryResponseDTO(Product product);


    @AfterMapping
    default void calculateStock(@MappingTarget ProductSummaryResponseDTO dto, Product product) {
        dto.setStock(product.getAvailableStock());
    }

    @AfterMapping
    default void calculateStock(@MappingTarget ProductResponseDTO dto, Product product) {
        dto.setStock(product.getAvailableStock());
    }

    default Integer getAvailableStock (Product product) {
        return product.getAvailableStock();
    }

}
