package com.IngSoftwarelll.mercadox.dtos.ticket.responses;

import com.IngSoftwarelll.mercadox.models.enums.MessageSenderRole;

import java.time.LocalDateTime;

public record TicketMessageResponse(
        Long id,
        String senderName,
        MessageSenderRole senderRole,
        String message,
        LocalDateTime createdAt
) {}
