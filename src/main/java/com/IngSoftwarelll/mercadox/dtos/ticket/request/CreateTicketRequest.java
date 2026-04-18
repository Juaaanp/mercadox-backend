package com.IngSoftwarelll.mercadox.dtos.ticket.request;

import com.IngSoftwarelll.mercadox.models.enums.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
        @NotNull(message = "El ID de la compra es obligatorio")
        Long purchaseId,

        @NotNull(message = "El ID del ítem es obligatorio")
        Long purchaseItemId,

        @NotNull(message = "El tipo de solicitud es obligatorio")
        TicketType type,

        @NotBlank(message = "La descripción del problema es obligatoria")
        String reason
) {}
