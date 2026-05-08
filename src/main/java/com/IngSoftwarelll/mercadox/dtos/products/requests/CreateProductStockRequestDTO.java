package com.IngSoftwarelll.mercadox.dtos.products.requests;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProductStockRequestDTO {

    @Valid
    @NotBlank(message = "Code cannot be empty")
    @Size(max = 50)
    private String code;

    private LocalDate expirationDate;
}
