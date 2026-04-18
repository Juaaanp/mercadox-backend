package com.IngSoftwarelll.mercadox.controllers;


import com.IngSoftwarelll.mercadox.dtos.payment.PaymentRequest;
import com.IngSoftwarelll.mercadox.dtos.payment.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.IngSoftwarelll.mercadox.security.CustomUserDetails;
import com.IngSoftwarelll.mercadox.services.interfaces.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<PaymentResponse> pay(
            @AuthenticationPrincipal CustomUserDetails user,
            @Validated @RequestBody PaymentRequest request) {

        log.info("Recarga solicitada por usuario {} — monto: {}", user.getId(), request.amount());
        return ResponseEntity.ok(paymentService.pay(user.getId(), request));
    }

    /**
     * GET /api/payments/history
     * Historial de recargas del usuario autenticado.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<Page<PaymentResponse>> getHistory(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(paymentService.getHistory(user.getId(), pageable));
    }
}