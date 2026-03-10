package com.IngSoftwarelll.mercadox.dtos.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.IngSoftwarelll.mercadox.models.Payment;
import com.IngSoftwarelll.mercadox.models.enums.PaymentMethod;
import com.IngSoftwarelll.mercadox.models.enums.PaymentStatus;

public record PaymentResponse(
    Long id,
    BigDecimal amount,
    PaymentStatus status,
    PaymentMethod paymentMethod,
    String cardLastFour,
    String gatewayReference,
    LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
            p.getId(),
            p.getAmount(),
            p.getStatus(),
            p.getPaymentMethod(),
            p.getCardLastFour(),
            p.getGatewayReference(),
            p.getCreatedAt()
        );
    }
}
