package com.IngSoftwarelll.mercadox.dtos.products.responses;

import java.math.BigDecimal;

import lombok.Data;
@Data
public class ProductSummaryResponseDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private BigDecimal price;
    private String categoryName;
    private Integer stock;
}
