package com.IngSoftwarelll.mercadox.dtos.ticket.responses;

import com.IngSoftwarelll.mercadox.models.enums.TicketResolution;
import com.IngSoftwarelll.mercadox.models.enums.TicketStatus;
import com.IngSoftwarelll.mercadox.models.enums.TicketType;

import java.time.LocalDateTime;

public record TicketSummaryResponse(
        Long id,
        String purchaseReferenceId,
        String productName,
        TicketType type,
        TicketStatus status,
        TicketResolution resolution,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {}