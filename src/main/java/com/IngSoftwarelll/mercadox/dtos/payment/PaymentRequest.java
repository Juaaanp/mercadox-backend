package com.IngSoftwarelll.mercadox.dtos.payment;

import com.IngSoftwarelll.mercadox.models.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "1000", message = "El monto mínimo es $1.000 COP")
        BigDecimal amount,

        @NotNull(message = "El método de pago es obligatorio")
        PaymentMethod paymentMethod,

        @NotBlank(message = "Los últimos 4 dígitos son obligatorios")
        String cardLastFour,

        @NotBlank(message = "El nombre del titular es obligatorio")
        String cardHolder,

        @NotBlank(message = "La fecha de vencimiento es obligatoria")
        String expiryDate
) {}