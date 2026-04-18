package com.IngSoftwarelll.mercadox.dtos.payment;

import com.IngSoftwarelll.mercadox.models.enums.PaymentMethod;
import com.IngSoftwarelll.mercadox.models.enums.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PurchaseStatus status,
        String cardLastFour,
        String cardHolder,
        String gatewayReference,
        LocalDateTime createdAt
) {}