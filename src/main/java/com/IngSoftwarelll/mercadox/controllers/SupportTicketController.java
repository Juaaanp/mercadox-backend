package com.IngSoftwarelll.mercadox.controllers;

import com.IngSoftwarelll.mercadox.dtos.ticket.request.*;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketDetailResponse;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.IngSoftwarelll.mercadox.security.CustomUserDetails;
import com.IngSoftwarelll.mercadox.services.interfaces.SupportTicketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService ticketService;

    // ─────────────────────────────────────────────────────────────────────────
    // CLIENTE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /tickets
     * Cliente abre un ticket de postventa.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<TicketDetailResponse> createTicket(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Validated @RequestBody CreateTicketRequest request) {

        TicketDetailResponse response = ticketService.createTicket(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /tickets/my
     * Cliente lista sus propios tickets.
     */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<Page<TicketSummaryResponse>> getMyTickets(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(ticketService.getMyTickets(userDetails.getId(), pageable));
    }

    /**
     * GET /tickets/{id}
     * Cliente o admin ve el detalle de un ticket.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSUMER') or hasAuthority('ADMIN')")
    public ResponseEntity<TicketDetailResponse> getTicketDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        return ResponseEntity.ok(ticketService.getTicketDetail(userDetails.getId(), id, isAdmin));
    }

    /**
     * POST /tickets/{id}/messages
     * Cliente o admin envía un mensaje al hilo del ticket.
     */
    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAuthority('CONSUMER') or hasAuthority('ADMIN')")
    public ResponseEntity<TicketDetailResponse> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Validated @RequestBody SendMessageRequest request) {

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        return ResponseEntity.ok(
                ticketService.sendMessage(userDetails.getId(), id, request, isAdmin)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /tickets/admin
     * Admin lista todos los tickets con filtro opcional por status.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<TicketSummaryResponse>> getAllTickets(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(ticketService.getAllTickets(status, pageable));
    }

    /**
     * PUT /tickets/{id}/validate
     * Admin marca el ticket como en validación.
     */
    @PutMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TicketDetailResponse> validateTicket(
            @AuthenticationPrincipal CustomUserDetails adminDetails,
            @PathVariable Long id,
            @RequestBody ValidateTicketRequest request) {

        return ResponseEntity.ok(ticketService.validateTicket(adminDetails.getId(), id, request));
    }

    /**
     * PUT /tickets/{id}/resolve/replacement
     * Admin resuelve asignando un nuevo código.
     */
    @PutMapping("/{id}/resolve/replacement")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TicketDetailResponse> resolveWithReplacement(
            @AuthenticationPrincipal CustomUserDetails adminDetails,
            @PathVariable Long id,
            @Validated @RequestBody ResolveWithReplacementRequest request) {

        return ResponseEntity.ok(
                ticketService.resolveWithReplacement(adminDetails.getId(), id, request)
        );
    }

    /**
     * PUT /tickets/{id}/resolve/refund
     * Admin resuelve con reembolso al balance del usuario.
     */
    @PutMapping("/{id}/resolve/refund")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TicketDetailResponse> resolveWithRefund(
            @AuthenticationPrincipal CustomUserDetails adminDetails,
            @PathVariable Long id,
            @RequestBody ResolveWithRefundRequest request) {

        return ResponseEntity.ok(
                ticketService.resolveWithRefund(adminDetails.getId(), id, request)
        );
    }

    /**
     * PUT /tickets/{id}/reject
     * Admin rechaza el ticket con justificación.
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TicketDetailResponse> rejectTicket(
            @AuthenticationPrincipal CustomUserDetails adminDetails,
            @PathVariable Long id,
            @Validated @RequestBody RejectTicketRequest request) {

        return ResponseEntity.ok(
                ticketService.rejectTicket(adminDetails.getId(), id, request)
        );
    }

    /**
     * PUT /tickets/{id}/close-invalid
     * Admin cierra el ticket como incidencia inválida.
     */
    @PutMapping("/{id}/close-invalid")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TicketDetailResponse> closeAsInvalid(
            @AuthenticationPrincipal CustomUserDetails adminDetails,
            @PathVariable Long id) {

        return ResponseEntity.ok(ticketService.closeAsInvalid(adminDetails.getId(), id));
    }
}
