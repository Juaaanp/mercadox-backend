package com.IngSoftwarelll.mercadox.mappers;


import java.util.List;

import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketDetailResponse;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketMessageResponse;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketSummaryResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.IngSoftwarelll.mercadox.models.SupportTicket;
import com.IngSoftwarelll.mercadox.models.TicketMessage;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    // ── TicketSummaryResponse ─────────────────────────────────────────────────
    // MapStruct mapea records usando el constructor canónico

    @Mapping(target = "purchaseReferenceId", source = "purchase.referenceId")
    @Mapping(target = "productName",         source = "purchaseItem.product.name")
    TicketSummaryResponse toSummaryResponse(SupportTicket ticket);

    // ── TicketDetailResponse ──────────────────────────────────────────────────
    // Usa @Builder de Lombok — MapStruct detecta el builder automáticamente

    @Mapping(target = "purchaseId",          source = "purchase.id")
    @Mapping(target = "purchaseReferenceId", source = "purchase.referenceId")
    @Mapping(target = "purchaseItemId",      source = "purchaseItem.id")
    @Mapping(target = "productName",         source = "purchaseItem.product.name")
    @Mapping(target = "deliveredCode",       source = "purchaseItem.deliveredCode")
    @Mapping(target = "priceAtPurchase",     source = "purchaseItem.priceAtPurchase")
    @Mapping(target = "messages",            source = "messages")
    @Mapping(target = "newDeliveredCode",    ignore = true) // set en @AfterMapping
    TicketDetailResponse toDetailResponse(SupportTicket ticket);

    @AfterMapping
    default void setNewDeliveredCode(SupportTicket ticket, @MappingTarget TicketDetailResponse dto) {
        if (ticket.getNewAssignedStock() != null) {
            dto.setNewDeliveredCode(ticket.getNewAssignedStock().getCode());
        }
    }

    // ── TicketMessageResponse ─────────────────────────────────────────────────
    // Record: MapStruct usa constructor canónico (id, senderName, senderRole, message, createdAt)

    @Mapping(target = "senderName", source = "sender.email")
    TicketMessageResponse toMessageResponse(TicketMessage message);

    List<TicketMessageResponse> toMessageResponseList(List<TicketMessage> messages);
}
