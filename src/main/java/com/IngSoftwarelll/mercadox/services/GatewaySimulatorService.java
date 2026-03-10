package com.IngSoftwarelll.mercadox.services;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Pasarela de pagos simulada.
 * En producción reemplazarías esto por el SDK real (Wompi, PayU, Stripe, etc.)
 */
@Service
public class GatewaySimulatorService {

    /**
     * Simula el procesamiento de un pago.
     * - Montos que terminan en 000 → DECLINED (para pruebas de rechazo)
     * - Todo lo demás → APPROVED
     *
     * @return referencia única generada por la "pasarela"
     * @throws PaymentDeclinedException si el pago es rechazado
     */
    public String process(BigDecimal amount, String cardLastFour) {
        // Simular rechazo: monto múltiplo exacto de 100 000
        if (amount.remainder(BigDecimal.valueOf(100_000)).compareTo(BigDecimal.ZERO) == 0) {
            throw new PaymentDeclinedException("Pago rechazado por la pasarela (simulación de rechazo).");
        }
        return "GW-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    public static class PaymentDeclinedException extends RuntimeException {
        public PaymentDeclinedException(String msg) { super(msg); }
    }
}
