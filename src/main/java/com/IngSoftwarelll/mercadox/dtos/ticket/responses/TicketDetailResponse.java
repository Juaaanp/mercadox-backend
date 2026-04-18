package com.IngSoftwarelll.mercadox.dtos.ticket.responses;

import com.IngSoftwarelll.mercadox.models.enums.TicketResolution;
import com.IngSoftwarelll.mercadox.models.enums.TicketStatus;
import com.IngSoftwarelll.mercadox.models.enums.TicketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailResponse {
    private Long id;
    private Long purchaseId;
    private String purchaseReferenceId;
    private Long purchaseItemId;
    private String productName;
    private String deliveredCode;
    private BigDecimal priceAtPurchase;
    private TicketType type;
    private TicketStatus status;
    private TicketResolution resolution;
    private String reason;
    private String adminNotes;
    private String rejectionJustification;
    private String newDeliveredCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private List<TicketMessageResponse> messages;
}
