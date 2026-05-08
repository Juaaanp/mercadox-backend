package com.IngSoftwarelll.mercadox.dtos.products.responses;

import java.time.ZonedDateTime;

import com.IngSoftwarelll.mercadox.models.enums.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductStockResponseDTO {
    private Long id;
    private String code;
    private StockStatus status;
    private ZonedDateTime createdAt;
    private ZonedDateTime soldAt;
}
