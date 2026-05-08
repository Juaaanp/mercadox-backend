package com.IngSoftwarelll.mercadox.mappers;

import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.IngSoftwarelll.mercadox.dtos.products.requests.CreateProductRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.products.requests.CreateProductStockRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductSummaryResponseDTO;
import com.IngSoftwarelll.mercadox.models.Product;
import com.IngSoftwarelll.mercadox.models.ProductStock;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "admin", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "stock", ignore = true)
    @Mapping(target = "productCategory", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toProduct (CreateProductRequestDTO request);

    List<ProductStock> toProductStockList(List<CreateProductStockRequestDTO> stockRequests);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "purchaseItem", ignore = true)
    @Mapping(target = "soldAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductStock toProductStock (CreateProductStockRequestDTO request);

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
