package com.IngSoftwarelll.mercadox.dtos.products.requests;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProductRequestDTO {
    
    @NotBlank(message = "Name is required")
    private String name;

    private BigDecimal price;
    private List<CreateProductStockRequestDTO> stockItems;
    private Long productCategoryId;
    private String description;
}
