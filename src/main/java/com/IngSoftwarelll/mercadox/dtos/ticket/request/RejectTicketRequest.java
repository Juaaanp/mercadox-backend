package com.IngSoftwarelll.mercadox.dtos.ticket.request;

import jakarta.validation.constraints.NotBlank;

public record RejectTicketRequest(
        @NotBlank(message = "La justificación es obligatoria")
        String justification,

        String adminNotes
) {}
