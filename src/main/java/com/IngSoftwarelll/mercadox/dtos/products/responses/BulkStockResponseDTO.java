package com.IngSoftwarelll.mercadox.dtos.products.responses;

import java.time.ZonedDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkStockResponseDTO {
    private Integer totalProcessed;
    private Integer successfullyAdded;
    private Integer failed;
    private List<String> errors;
    private ZonedDateTime processedAt;
}
