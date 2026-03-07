package com.IngSoftwarelll.mercadox.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.IngSoftwarelll.mercadox.dtos.purchases.responses.PurchaseItemResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.purchases.responses.PurchaseResponseDTO;
import com.IngSoftwarelll.mercadox.models.Purchase;
import com.IngSoftwarelll.mercadox.models.PurchaseItem;


@Mapper(componentModel = "spring")
public interface PurchaseMapper {
    
    @Mapping(target = "totalItems", expression = "java(getTotalItems(purchase))")
    PurchaseResponseDTO toPurchaseResponseDTO (Purchase purchase);

    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "imageUrl", source = "product.imageUrl")
    @Mapping(target = "productId", source = "product.id")
    PurchaseItemResponseDTO toPurchaseItemResponseDTO(PurchaseItem purchaseItem);

    default Integer getTotalItems (Purchase purchase){
        return purchase.getItems().size();
    }
}
