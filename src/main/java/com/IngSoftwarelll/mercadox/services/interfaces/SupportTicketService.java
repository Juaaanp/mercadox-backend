package com.IngSoftwarelll.mercadox.services.interfaces;



import com.IngSoftwarelll.mercadox.dtos.ticket.request.*;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketDetailResponse;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;



public interface SupportTicketService {

    // ── Cliente ──────────────────────────────────────────────────────────────

    /** Abre un nuevo ticket de postventa */
    TicketDetailResponse createTicket(Long userId, CreateTicketRequest request);

    /** Lista los tickets del cliente autenticado */
    Page<TicketSummaryResponse> getMyTickets(Long userId, Pageable pageable);

    /** Ver detalle de un ticket (cliente solo puede ver los suyos) */
    TicketDetailResponse getTicketDetail(Long userId, Long ticketId, boolean isAdmin);

    /** Cliente o admin envía un mensaje al hilo del ticket */
    TicketDetailResponse sendMessage(Long senderId, Long ticketId, SendMessageRequest request, boolean isAdmin);

    // ── Admin ────────────────────────────────────────────────────────────────

    /** Lista todos los tickets (admin), con filtro opcional por status */
    Page<TicketSummaryResponse> getAllTickets(String status, Pageable pageable);

    /** Admin marca el ticket como en validación */
    TicketDetailResponse validateTicket(Long adminId, Long ticketId, ValidateTicketRequest request);

    /** Admin resuelve con reemplazo de código */
    TicketDetailResponse resolveWithReplacement(Long adminId, Long ticketId, ResolveWithReplacementRequest request);

    /** Admin resuelve con reembolso al balance del usuario */
    TicketDetailResponse resolveWithRefund(Long adminId, Long ticketId, ResolveWithRefundRequest request);

    /** Admin rechaza el ticket con justificación */
    TicketDetailResponse rejectTicket(Long adminId, Long ticketId, RejectTicketRequest request);

    /** Admin cierra el ticket como inválido */
    TicketDetailResponse closeAsInvalid(Long adminId, Long ticketId);
}
