package com.IngSoftwarelll.mercadox.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.IngSoftwarelll.mercadox.dtos.products.requests.CreateProductStockRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductStockResponseDTO;
import com.IngSoftwarelll.mercadox.models.ProductStock;


@Mapper(componentModel = "spring")
public interface ProductStockMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "purchaseItem", ignore = true)
    @Mapping(target = "soldAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductStock toProductStock (CreateProductStockRequestDTO request);

    
    ProductStockResponseDTO toProductStockResponseDTO (ProductStock product);
}
