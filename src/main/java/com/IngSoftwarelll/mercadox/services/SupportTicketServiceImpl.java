package com.IngSoftwarelll.mercadox.services;



import java.util.List;

import com.IngSoftwarelll.mercadox.dtos.ticket.request.*;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketDetailResponse;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketSummaryResponse;
import com.IngSoftwarelll.mercadox.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.IngSoftwarelll.mercadox.mappers.TicketMapper;
import com.IngSoftwarelll.mercadox.models.ProductStock;
import com.IngSoftwarelll.mercadox.models.Purchase;
import com.IngSoftwarelll.mercadox.models.PurchaseItem;
import com.IngSoftwarelll.mercadox.models.SupportTicket;
import com.IngSoftwarelll.mercadox.models.TicketMessage;
import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.models.enums.MessageSenderRole;
import com.IngSoftwarelll.mercadox.models.enums.PurchaseItemStatus;
import com.IngSoftwarelll.mercadox.models.enums.StockStatus;
import com.IngSoftwarelll.mercadox.models.enums.TicketStatus;
import com.IngSoftwarelll.mercadox.services.interfaces.SupportTicketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final PurchaseRepository      purchaseRepository;
    private final PurchaseItemRepository  purchaseItemRepository;
    private final ProductStockRepository  productStockRepository;
    private final UserRepository          userRepository;
    private final TicketMapper            ticketMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // CLIENTE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TicketDetailResponse createTicket(Long userId, CreateTicketRequest request) {
        User user = findUser(userId);

        Purchase purchase = purchaseRepository.findById(request.purchaseId())
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));

        if (!purchase.getUser().getId().equals(userId)) {
            throw new SecurityException("No tienes permiso para abrir un ticket sobre esta compra");
        }

        PurchaseItem item = purchaseItemRepository.findById(request.purchaseItemId())
                .orElseThrow(() -> new IllegalArgumentException("Ítem de compra no encontrado"));

        if (!item.getPurchase().getId().equals(purchase.getId())) {
            throw new IllegalArgumentException("El ítem no pertenece a la compra indicada");
        }

        if (item.getStatus() != PurchaseItemStatus.DELIVERED) {
            throw new IllegalStateException("Solo se puede abrir un ticket sobre un ítem entregado");
        }

        boolean hasOpenTicket = ticketRepository.existsByPurchaseItemIdAndStatusIn(
                item.getId(),
                List.of(TicketStatus.OPEN, TicketStatus.VALIDATING)
        );
        if (hasOpenTicket) {
            throw new IllegalStateException("Ya existe un ticket abierto para este ítem");
        }

        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .purchase(purchase)
                .purchaseItem(item)
                .type(request.type())
                .reason(request.reason())
                .build();

        ticket.addMessage(buildMessage(user, MessageSenderRole.CLIENT, request.reason()));
        ticketRepository.save(ticket);

        log.info("Ticket #{} creado por usuario {} sobre ítem {}", ticket.getId(), userId, item.getId());
        return ticketMapper.toDetailResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketSummaryResponse> getMyTickets(Long userId, Pageable pageable) {
        return ticketRepository.findByUserId(userId, pageable)
                .map(ticketMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketDetail(Long userId, Long ticketId, boolean isAdmin) {
        SupportTicket ticket = findTicketWithDetails(ticketId);
        if (!isAdmin && !ticket.getUser().getId().equals(userId)) {
            throw new SecurityException("No tienes permiso para ver este ticket");
        }
        return ticketMapper.toDetailResponse(ticket);
    }

    @Override
    @Transactional
    public TicketDetailResponse sendMessage(Long senderId, Long ticketId, SendMessageRequest request, boolean isAdmin) {
        SupportTicket ticket = findTicketWithDetails(ticketId);

        if (!isAdmin && !ticket.getUser().getId().equals(senderId)) {
            throw new SecurityException("No tienes permiso para responder este ticket");
        }

        if (isClosed(ticket)) {
            throw new IllegalStateException("No se puede enviar mensajes en un ticket cerrado");
        }

        User sender = findUser(senderId);
        ticket.addMessage(buildMessage(
                sender,
                isAdmin ? MessageSenderRole.ADMIN : MessageSenderRole.CLIENT,
                request.message()
        ));
        ticketRepository.save(ticket);
        return ticketMapper.toDetailResponse(ticket);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<TicketSummaryResponse> getAllTickets(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            TicketStatus ticketStatus = TicketStatus.valueOf(status.toUpperCase());
            return ticketRepository.findByStatus(ticketStatus, pageable)
                    .map(ticketMapper::toSummaryResponse);
        }
        return ticketRepository.findAll(pageable).map(ticketMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public TicketDetailResponse validateTicket(Long adminId, Long ticketId, ValidateTicketRequest request) {
        SupportTicket ticket = findTicketWithDetails(ticketId);
        assertStatus(ticket, TicketStatus.OPEN, "solo se puede validar un ticket OPEN");

        ticket.markAsValidating();
        if (request.adminNotes() != null) ticket.setAdminNotes(request.adminNotes());

        ticket.addMessage(buildAdminMessage(adminId,
                "Tu solicitud está siendo revisada por nuestro equipo de soporte."));

        ticketRepository.save(ticket);
        log.info("Ticket #{} marcado como VALIDATING por admin {}", ticketId, adminId);
        return ticketMapper.toDetailResponse(ticket);
    }

    @Override
    @Transactional
    public TicketDetailResponse resolveWithReplacement(Long adminId, Long ticketId, ResolveWithReplacementRequest request) {
        SupportTicket ticket = findTicketWithDetails(ticketId);
        assertStatus(ticket, TicketStatus.VALIDATING, "solo se puede resolver un ticket VALIDATING");

        ProductStock newStock = productStockRepository.findById(request.newProductStockId())
                .orElseThrow(() -> new IllegalArgumentException("Stock no encontrado"));

        if (newStock.getStatus() != StockStatus.AVAILABLE) {
            throw new IllegalStateException("El código seleccionado no está disponible");
        }

        newStock.setStatus(StockStatus.SOLD);
        productStockRepository.save(newStock);

        PurchaseItem item = ticket.getPurchaseItem();
        item.assignProductStock(newStock);
        purchaseItemRepository.save(item);

        if (request.adminNotes() != null) ticket.setAdminNotes(request.adminNotes());
        ticket.resolveWithReplacement(newStock);

        ticket.addMessage(buildAdminMessage(adminId,
                "Tu solicitud fue aprobada. Se ha asignado un nuevo código: " + newStock.getCode()));

        ticketRepository.save(ticket);
        log.info("Ticket #{} resuelto con reemplazo. Nuevo stock: {}", ticketId, newStock.getId());
        return ticketMapper.toDetailResponse(ticket);
    }

    @Override
    @Transactional
    public TicketDetailResponse resolveWithRefund(Long adminId, Long ticketId, ResolveWithRefundRequest request) {
        SupportTicket ticket = findTicketWithDetails(ticketId);
        assertStatus(ticket, TicketStatus.VALIDATING, "solo se puede resolver un ticket VALIDATING");

        User user = ticket.getUser();
        user.setBalance(user.getBalance().add(ticket.getPurchaseItem().getPriceAtPurchase()));
        userRepository.save(user);

        if (request.adminNotes() != null) ticket.setAdminNotes(request.adminNotes());
        ticket.resolveWithRefund();

        ticket.addMessage(buildAdminMessage(adminId,
                "Tu reembolso de $" + ticket.getPurchaseItem().getPriceAtPurchase() +
                        " ha sido procesado y acreditado a tu saldo en Mercadox."));

        ticketRepository.save(ticket);
        log.info("Ticket #{} resuelto con reembolso a usuario {}", ticketId, user.getId());
        return ticketMapper.toDetailResponse(ticket);
    }

    @Override
    @Transactional
    public TicketDetailResponse rejectTicket(Long adminId, Long ticketId, RejectTicketRequest request) {
        SupportTicket ticket = findTicketWithDetails(ticketId);
        assertStatus(ticket, TicketStatus.VALIDATING, "solo se puede rechazar un ticket VALIDATING");

        if (request.adminNotes() != null) ticket.setAdminNotes(request.adminNotes());
        ticket.reject(request.justification());

        ticket.addMessage(buildAdminMessage(adminId,
                "Tu solicitud fue rechazada. Motivo: " + request.justification()));

        ticketRepository.save(ticket);
        log.info("Ticket #{} rechazado por admin {}", ticketId, adminId);
        return ticketMapper.toDetailResponse(ticket);
    }

    @Override
    @Transactional
    public TicketDetailResponse closeAsInvalid(Long adminId, Long ticketId) {
        SupportTicket ticket = findTicketWithDetails(ticketId);

        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.VALIDATING) {
            throw new IllegalStateException("El ticket no puede cerrarse como inválido en su estado actual");
        }

        ticket.closeAsInvalid();
        ticket.addMessage(buildAdminMessage(adminId,
                "Revisamos tu solicitud y determinamos que no corresponde a una incidencia válida. El ticket ha sido cerrado."));

        ticketRepository.save(ticket);
        log.info("Ticket #{} cerrado como INVALID por admin {}", ticketId, adminId);
        return ticketMapper.toDetailResponse(ticket);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private SupportTicket findTicketWithDetails(Long ticketId) {
        return ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado: " + ticketId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
    }

    private void assertStatus(SupportTicket ticket, TicketStatus expected, String msg) {
        if (ticket.getStatus() != expected) {
            throw new IllegalStateException(
                    "Acción inválida: " + msg + " (estado actual: " + ticket.getStatus() + ")"
            );
        }
    }

    private boolean isClosed(SupportTicket ticket) {
        return ticket.getStatus() == TicketStatus.RESOLVED
                || ticket.getStatus() == TicketStatus.REJECTED
                || ticket.getStatus() == TicketStatus.CLOSED_INVALID;
    }

    private TicketMessage buildMessage(User sender, MessageSenderRole role, String text) {
        return TicketMessage.builder()
                .sender(sender)
                .senderRole(role)
                .message(text)
                .build();
    }

    private TicketMessage buildAdminMessage(Long adminId, String text) {
        return buildMessage(findUser(adminId), MessageSenderRole.ADMIN, text);
    }
}
