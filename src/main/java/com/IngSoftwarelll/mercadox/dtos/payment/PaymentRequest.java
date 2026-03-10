package com.IngSoftwarelll.mercadox.dtos.payment;

import java.math.BigDecimal;

import com.IngSoftwarelll.mercadox.models.enums.PaymentMethod;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Petición que llega desde el frontend para iniciar un pago */
public record PaymentRequest(

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "1000.00", message = "El monto mínimo es $1.000 COP")
    @DecimalMax(value = "10000000.00", message = "El monto máximo es $10.000.000 COP")
    BigDecimal amount,

    @NotNull(message = "El método de pago es obligatorio")
    PaymentMethod paymentMethod,

    /** Solo últimos 4 dígitos — nunca almacenamos datos sensibles */
    @Size(min = 4, max = 4, message = "Deben ser exactamente 4 dígitos")
    @Pattern(regexp = "\\d{4}", message = "Solo se permiten dígitos")
    String cardLastFour,

    /** Nombre en la tarjeta (solo para simulación, no se persiste) */
    @NotBlank(message = "El nombre del titular es obligatorio")
    String cardHolder,

    /** MM/YY — solo validación básica, no se persiste */
    @NotBlank
    @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{2}$", message = "Formato MM/YY inválido")
    String expiryDate
) {}
