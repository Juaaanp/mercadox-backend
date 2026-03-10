package com.IngSoftwarelll.mercadox.controllers;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.IngSoftwarelll.mercadox.dtos.payment.PaymentRequest;
import com.IngSoftwarelll.mercadox.dtos.payment.PaymentResponse;
import com.IngSoftwarelll.mercadox.security.CustomUserDetails;
import com.IngSoftwarelll.mercadox.services.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments
     * Inicia y procesa un pago (recarga de saldo).
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> pay(
        @AuthenticationPrincipal CustomUserDetails user,
        @Valid @RequestBody PaymentRequest request) {
            
        PaymentResponse response = paymentService.processPayment(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/payments/history?page=0&size=10
     * Historial de transacciones del usuario autenticado.
     */
    @GetMapping("/history")
    public ResponseEntity<Page<PaymentResponse>> history(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(paymentService.getHistory(user.getId(), page, size));
    }
}