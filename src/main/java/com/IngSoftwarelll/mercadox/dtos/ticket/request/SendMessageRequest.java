package com.IngSoftwarelll.mercadox.dtos.ticket.request;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        String message
) {}