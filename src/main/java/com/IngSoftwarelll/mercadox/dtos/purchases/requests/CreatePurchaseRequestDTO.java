package com.IngSoftwarelll.mercadox.dtos.purchases.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePurchaseRequestDTO {
    
    // ===== ITEMS DE LA COMPRA =====
    @Valid
    @NotEmpty(message = "Purchase must have at least one item")
    private List<CreatePurchaseItemRequestDTO> items;

}
