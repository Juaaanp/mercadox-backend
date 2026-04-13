package com.IngSoftwarelll.mercadox.dtos.ticket.request;

import jakarta.validation.constraints.NotNull;

public record ResolveWithReplacementRequest(
        @NotNull(message = "El ID del nuevo stock es obligatorio")
        Long newProductStockId,

        String adminNotes
) {}
