package com.IngSoftwarelll.mercadox.controllers;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.IngSoftwarelll.mercadox.dtos.EntityCreatedResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.purchases.requests.CreatePurchaseRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.purchases.responses.PurchaseResponseDTO;
import com.IngSoftwarelll.mercadox.security.CustomUserDetails;
import com.IngSoftwarelll.mercadox.services.interfaces.PurchaseService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping("/buy")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<EntityCreatedResponseDTO> createPurchase(@AuthenticationPrincipal CustomUserDetails user,
            @RequestBody CreatePurchaseRequestDTO request) {
        return ResponseEntity.ok(purchaseService.createPurchase(user.getId(), request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<Page<PurchaseResponseDTO>> getPurchases(@AuthenticationPrincipal CustomUserDetails user, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(purchaseService.getUserPurchases(user.getId(), pageable));
    }

}
