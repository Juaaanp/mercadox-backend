package com.IngSoftwarelll.mercadox.mappers;


import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketDetailResponse;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketMessageResponse;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketSummaryResponse;
import com.IngSoftwarelll.mercadox.models.SupportTicket;
import com.IngSoftwarelll.mercadox.models.TicketMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SupportTicketMapper {

    // ─────────────────────────────────────────────────────────────────────────
    // SupportTicket → TicketDetailResponse
    // ─────────────────────────────────────────────────────────────────────────

    @Mapping(target = "purchaseId",
            source = "purchase.id")

    @Mapping(target = "purchaseReferenceId",
            source = "purchase.referenceId")

    @Mapping(target = "purchaseItemId",
            source = "purchaseItem.id")

    @Mapping(target = "productName",
            source = "purchaseItem.product.name")          // ← assignedProductStock no existe en PurchaseItem para nombre

    @Mapping(target = "deliveredCode",
            source = "purchaseItem.deliveredCode")         // ← campo directo en PurchaseItem

    @Mapping(target = "priceAtPurchase",
            source = "purchaseItem.priceAtPurchase")

    @Mapping(target = "newDeliveredCode",
            source = "newAssignedStock.code")

    @Mapping(target = "messages",
            source = "messages",
            qualifiedByName = "toMessageResponseList")
    TicketDetailResponse toDetailResponse(SupportTicket ticket);

    // ─────────────────────────────────────────────────────────────────────────
    // SupportTicket → TicketSummaryResponse
    // ─────────────────────────────────────────────────────────────────────────

    @Mapping(target = "purchaseReferenceId",
            source = "purchase.referenceId")

    @Mapping(target = "productName",
            source = "purchaseItem.product.name")          // ← igual que arriba

    TicketSummaryResponse toSummaryResponse(SupportTicket ticket);

    // ─────────────────────────────────────────────────────────────────────────
    // TicketMessage → TicketMessageResponse
    // ─────────────────────────────────────────────────────────────────────────

    @Mapping(target = "senderName",
            source = "sender.email")                       // ← User no tiene name, tiene email

    TicketMessageResponse toMessageResponse(TicketMessage message);

    @Named("toMessageResponseList")
    default List<TicketMessageResponse> toMessageResponseList(List<TicketMessage> messages) {
        if (messages == null) return List.of();
        return messages.stream()
                .map(this::toMessageResponse)
                .toList();
    }
}