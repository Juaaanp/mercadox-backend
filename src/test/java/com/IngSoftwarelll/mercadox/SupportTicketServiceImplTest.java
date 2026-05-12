package com.IngSoftwarelll.mercadox;

import com.IngSoftwarelll.mercadox.dtos.ticket.request.*;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketDetailResponse;
import com.IngSoftwarelll.mercadox.dtos.ticket.responses.TicketSummaryResponse;
import com.IngSoftwarelll.mercadox.mappers.TicketMapper;
import com.IngSoftwarelll.mercadox.models.*;
import com.IngSoftwarelll.mercadox.models.enums.*;
import com.IngSoftwarelll.mercadox.repositories.*;
import com.IngSoftwarelll.mercadox.services.SupportTicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("SupportTicketServiceImpl — Pruebas unitarias")
class SupportTicketServiceImplTest {

    @Mock private SupportTicketRepository  ticketRepository;
    @Mock private PurchaseRepository       purchaseRepository;
    @Mock private PurchaseItemRepository   purchaseItemRepository;
    @Mock private ProductStockRepository   productStockRepository;
    @Mock private UserRepository           userRepository;
    @Mock private TicketMapper             ticketMapper;

    @InjectMocks
    private SupportTicketServiceImpl ticketService;

    // ─────────────────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────────────────

    private User consumer;
    private User admin;
    private Purchase purchase;
    private PurchaseItem purchaseItem;
    private SupportTicket openTicket;
    private SupportTicket validatingTicket;

    @BeforeEach
    void setUp() {
        consumer = new User();
        consumer.setId(1L);
        consumer.setEmail("consumer@test.com");
        consumer.setBalance(new BigDecimal("100000"));
        consumer.setRole(UserRole.CONSUMER);

        admin = new User();
        admin.setId(5L);
        admin.setEmail("admin@test.com");
        admin.setBalance(BigDecimal.ZERO);
        admin.setRole(UserRole.ADMIN);

        purchase = Purchase.builder()
                .id(10L)
                .referenceId("REF-001")
                .user(consumer)
                .total(new BigDecimal("50000"))
                .status(PurchaseStatus.COMPLETED)
                .build();

        purchaseItem = PurchaseItem.builder()
                .id(20L)
                .purchase(purchase)
                .priceAtPurchase(new BigDecimal("50000"))
                .quantity(1)
                .subtotal(new BigDecimal("50000"))
                .deliveredCode("XXXX-YYYY-ZZZZ")
                .status(PurchaseItemStatus.DELIVERED)
                .build();

        openTicket = SupportTicket.builder()
                .id(100L)
                .user(consumer)
                .purchase(purchase)
                .purchaseItem(purchaseItem)
                .type(TicketType.REPLACEMENT)
                .status(TicketStatus.OPEN)
                .reason("El código no funciona")
                .build();

        // Ticket en estado VALIDATING — requerido por resolve/reject
        validatingTicket = SupportTicket.builder()
                .id(100L)
                .user(consumer)
                .purchase(purchase)
                .purchaseItem(purchaseItem)
                .type(TicketType.REPLACEMENT)
                .status(TicketStatus.VALIDATING)
                .reason("El código no funciona")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AP-28 | createTicket — CP-21
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createTicket")
    class CreateTicketTests {

        @Test
        @DisplayName("AP-28 | CONSUMER crea ticket exitosamente — estado inicial OPEN — CP-21")
        void createTicket_success() {
            CreateTicketRequest request = new CreateTicketRequest(
                    10L, 20L, TicketType.REPLACEMENT, "El código no funciona"
            );
            TicketDetailResponse expected = TicketDetailResponse.builder()
                    .id(100L)
                    .status(TicketStatus.OPEN)
                    .type(TicketType.REPLACEMENT)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(consumer));
            when(purchaseRepository.findById(10L)).thenReturn(Optional.of(purchase));
            when(purchaseItemRepository.findById(20L)).thenReturn(Optional.of(purchaseItem));
            when(ticketRepository.existsByPurchaseItemIdAndStatusIn(eq(20L), anyList()))
                    .thenReturn(false);
            when(ticketRepository.save(any(SupportTicket.class))).thenReturn(openTicket);
            when(ticketMapper.toDetailResponse(any())).thenReturn(expected);

            TicketDetailResponse result = ticketService.createTicket(1L, request);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo(TicketStatus.OPEN);
            verify(ticketRepository).save(any(SupportTicket.class));
        }

        @Test
        @DisplayName("AP-29 | createTicket con usuario inexistente — IllegalArgumentException — 0 tickets creados")
        void createTicket_userNotFound_throwsException() {
            CreateTicketRequest request = new CreateTicketRequest(
                    10L, 20L, TicketType.REPLACEMENT, "El código no funciona"
            );

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    ticketService.createTicket(99L, request)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario no encontrado");

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("AP-30 | createTicket con compra de otro usuario — SecurityException")
        void createTicket_purchaseNotOwned_throwsSecurityException() {
            CreateTicketRequest request = new CreateTicketRequest(
                    10L, 20L, TicketType.REPLACEMENT, "El código no funciona"
            );
            // La compra pertenece a consumer (id=1), pero quien crea es userId=2
            when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
            when(purchaseRepository.findById(10L)).thenReturn(Optional.of(purchase));

            assertThatThrownBy(() ->
                    ticketService.createTicket(2L, request)
            ).isInstanceOf(SecurityException.class);

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("AP-31 | createTicket con ítem ya con ticket abierto — IllegalStateException")
        void createTicket_existingOpenTicket_throwsException() {
            CreateTicketRequest request = new CreateTicketRequest(
                    10L, 20L, TicketType.REPLACEMENT, "El código no funciona"
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(consumer));
            when(purchaseRepository.findById(10L)).thenReturn(Optional.of(purchase));
            when(purchaseItemRepository.findById(20L)).thenReturn(Optional.of(purchaseItem));
            when(ticketRepository.existsByPurchaseItemIdAndStatusIn(eq(20L), anyList()))
                    .thenReturn(true); // Ya existe ticket abierto

            assertThatThrownBy(() ->
                    ticketService.createTicket(1L, request)
            ).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Ya existe un ticket abierto");

            verify(ticketRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AP-32 | getMyTickets — CP-25
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getMyTickets")
    class GetMyTicketsTests {

        @Test
        @DisplayName("AP-32 | CONSUMER recibe solo sus tickets — aislamiento garantizado — CP-25")
        void getMyTickets_returnsOnlyOwnTickets() {
            Pageable pageable = PageRequest.of(0, 10);
            TicketSummaryResponse summary = new TicketSummaryResponse(
                    100L, "REF-001", "Steam Key",
                    TicketType.REPLACEMENT, TicketStatus.OPEN,
                    null, LocalDateTime.now(), null
            );
            Page<SupportTicket> ticketPage = new PageImpl<>(List.of(openTicket));

            when(ticketRepository.findByUserId(1L, pageable)).thenReturn(ticketPage);
            when(ticketMapper.toSummaryResponse(openTicket)).thenReturn(summary);

            Page<TicketSummaryResponse> result = ticketService.getMyTickets(1L, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(100L);
            verify(ticketRepository, never()).findAll(any(Pageable.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AP-33 a AP-36 | getTicketDetail — CP-25, CP-26
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getTicketDetail")
    class GetTicketDetailTests {

        @Test
        @DisplayName("AP-33 | Dueño del ticket puede ver su detalle — CP-25")
        void getTicketDetail_ownerCanView() {
            TicketDetailResponse expected = TicketDetailResponse.builder()
                    .id(100L)
                    .status(TicketStatus.OPEN)
                    .deliveredCode("XXXX-YYYY-ZZZZ")
                    .build();

            // ← findByIdWithDetails, no findById
            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(openTicket));
            when(ticketMapper.toDetailResponse(openTicket)).thenReturn(expected);

            TicketDetailResponse result = ticketService.getTicketDetail(1L, 100L, false);

            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("AP-34 | CONSUMER intenta ver ticket ajeno — SecurityException — CP-26")
        void getTicketDetail_nonOwnerConsumer_throwsSecurityException() {
            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(openTicket));

            // userId=2 no es dueño (dueño es userId=1) y no es admin
            assertThatThrownBy(() ->
                    ticketService.getTicketDetail(2L, 100L, false)
            ).isInstanceOf(SecurityException.class);

            verify(ticketMapper, never()).toDetailResponse(any());
        }

        @Test
        @DisplayName("AP-35 | ADMIN puede ver cualquier ticket sin importar el dueño")
        void getTicketDetail_adminCanViewAnyTicket() {
            TicketDetailResponse expected = TicketDetailResponse.builder()
                    .id(100L).build();

            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(openTicket));
            when(ticketMapper.toDetailResponse(openTicket)).thenReturn(expected);

            // isAdmin=true — no importa que adminId=5 no sea el dueño
            assertThatCode(() ->
                    ticketService.getTicketDetail(5L, 100L, true)
            ).doesNotThrowAnyException();

            verify(ticketMapper).toDetailResponse(openTicket);
        }

        @Test
        @DisplayName("AP-36 | Ticket inexistente — IllegalArgumentException")
        void getTicketDetail_notFound_throwsException() {
            when(ticketRepository.findByIdWithDetails(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    ticketService.getTicketDetail(1L, 999L, false)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ticket no encontrado");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AP-37 a AP-39 | sendMessage
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sendMessage")
    class SendMessageTests {

        @Test
        @DisplayName("AP-37 | CONSUMER envía mensaje — senderRole=CLIENT persistido")
        void sendMessage_consumer_senderRoleIsClient() {
            SendMessageRequest request = new SendMessageRequest("Adjunto evidencia");
            TicketDetailResponse expected = TicketDetailResponse.builder().id(100L).build();

            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(openTicket));
            when(userRepository.findById(1L)).thenReturn(Optional.of(consumer));
            when(ticketRepository.save(any())).thenReturn(openTicket);
            when(ticketMapper.toDetailResponse(openTicket)).thenReturn(expected);

            ticketService.sendMessage(1L, 100L, request, false);

            verify(ticketRepository).save(argThat(t ->
                    t.getMessages().stream().anyMatch(m ->
                            m.getSenderRole() == MessageSenderRole.CLIENT
                                    && m.getMessage().equals("Adjunto evidencia")
                    )
            ));
        }

        @Test
        @DisplayName("AP-38 | ADMIN envía mensaje — senderRole=ADMIN persistido")
        void sendMessage_admin_senderRoleIsAdmin() {
            SendMessageRequest request = new SendMessageRequest("Estamos revisando su caso");
            TicketDetailResponse expected = TicketDetailResponse.builder().id(100L).build();

            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(openTicket));
            // isAdmin=true → buildAdminMessage → findUser(adminId)
            when(userRepository.findById(5L)).thenReturn(Optional.of(admin));
            when(ticketRepository.save(any())).thenReturn(openTicket);
            when(ticketMapper.toDetailResponse(openTicket)).thenReturn(expected);

            ticketService.sendMessage(5L, 100L, request, true);

            verify(ticketRepository).save(argThat(t ->
                    t.getMessages().stream().anyMatch(m ->
                            m.getSenderRole() == MessageSenderRole.ADMIN
                    )
            ));
        }

        @Test
        @DisplayName("AP-39 | CONSUMER envía mensaje a ticket ajeno — SecurityException")
        void sendMessage_nonOwnerConsumer_throwsSecurityException() {
            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(openTicket));

            assertThatThrownBy(() ->
                    ticketService.sendMessage(2L, 100L, new SendMessageRequest("hack"), false)
            ).isInstanceOf(SecurityException.class);

            verify(ticketRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AP-40 a AP-41 | resolveWithReplacement — CP-23
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("resolveWithReplacement")
    class ResolveWithReplacementTests {

        @Test
        @DisplayName("AP-40 | Admin resuelve con reemplazo — estado RESOLVED, REPLACEMENT_SENT — CP-23")
        void resolveWithReplacement_success() {
            ProductStock newStock = ProductStock.builder()
                    .id(30L)
                    .code("NUEVO-CODE-1234")
                    .status(StockStatus.AVAILABLE)
                    .build();

            ResolveWithReplacementRequest request =
                    new ResolveWithReplacementRequest(30L, "Reemplazo aprobado");

            TicketDetailResponse expected = TicketDetailResponse.builder()
                    .id(100L)
                    .status(TicketStatus.RESOLVED)
                    .resolution(TicketResolution.REPLACEMENT_SENT)
                    .build();

            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(validatingTicket)); // ← VALIDATING
            when(productStockRepository.findById(30L))
                    .thenReturn(Optional.of(newStock));
            when(productStockRepository.save(any())).thenReturn(newStock);
            when(purchaseItemRepository.save(any())).thenReturn(purchaseItem);
            // buildAdminMessage → findUser(adminId)
            when(userRepository.findById(5L)).thenReturn(Optional.of(admin));
            when(ticketRepository.save(any())).thenReturn(validatingTicket);
            when(ticketMapper.toDetailResponse(any())).thenReturn(expected);

            TicketDetailResponse result =
                    ticketService.resolveWithReplacement(5L, 100L, request);

            assertThat(result.getStatus()).isEqualTo(TicketStatus.RESOLVED);
            assertThat(result.getResolution()).isEqualTo(TicketResolution.REPLACEMENT_SENT);
            verify(productStockRepository).save(argThat(s ->
                    s.getStatus() == StockStatus.SOLD
            ));
        }

        @Test
        @DisplayName("AP-41 | resolveWithReplacement sobre ticket OPEN (no VALIDATING) — IllegalStateException")
        void resolveWithReplacement_wrongStatus_throwsException() {
            ResolveWithReplacementRequest request =
                    new ResolveWithReplacementRequest(30L, null);

            // openTicket está en OPEN, assertStatus espera VALIDATING
            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(openTicket));

            assertThatThrownBy(() ->
                    ticketService.resolveWithReplacement(5L, 100L, request)
            ).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("solo se puede resolver un ticket VALIDATING");

            verify(ticketRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AP-42 | resolveWithRefund
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("resolveWithRefund")
    class ResolveWithRefundTests {

        @Test
        @DisplayName("AP-42 | Admin resuelve con reembolso — balance restaurado, REFUND_PROCESSED")
        void resolveWithRefund_balanceRestored() {
            consumer.setBalance(new BigDecimal("100000"));

            SupportTicket validating = SupportTicket.builder()
                    .id(100L)
                    .user(consumer)
                    .purchase(purchase)
                    .purchaseItem(purchaseItem) // priceAtPurchase = 50000
                    .type(TicketType.REFUND)
                    .status(TicketStatus.VALIDATING)
                    .reason("Código nunca funcionó")
                    .build();

            TicketDetailResponse expected = TicketDetailResponse.builder()
                    .id(100L)
                    .status(TicketStatus.RESOLVED)
                    .resolution(TicketResolution.REFUND_PROCESSED)
                    .build();

            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(validating));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // buildAdminMessage → findUser(adminId)
            when(userRepository.findById(5L)).thenReturn(Optional.of(admin));
            when(ticketRepository.save(any())).thenReturn(validating);
            when(ticketMapper.toDetailResponse(any())).thenReturn(expected);

            ticketService.resolveWithRefund(5L, 100L,
                    new ResolveWithRefundRequest("Reembolso aprobado"));

            // Balance: 100000 + 50000 = 150000
            verify(userRepository).save(argThat(u ->
                    u.getBalance().compareTo(new BigDecimal("150000")) == 0
            ));
            verify(ticketRepository).save(argThat(t ->
                    t.getStatus() == TicketStatus.RESOLVED
                            && t.getResolution() == TicketResolution.REFUND_PROCESSED
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AP-43 a AP-44 | rejectTicket
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("rejectTicket")
    class RejectTicketTests {

        @Test
        @DisplayName("AP-43 | Admin rechaza ticket — estado REJECTED, justificación persistida")
        void rejectTicket_success() {
            RejectTicketRequest request = new RejectTicketRequest(
                    "El código fue usado correctamente", null
            );
            TicketDetailResponse expected = TicketDetailResponse.builder()
                    .id(100L)
                    .status(TicketStatus.REJECTED)
                    .resolution(TicketResolution.REJECTED)
                    .rejectionJustification("El código fue usado correctamente")
                    .build();

            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(validatingTicket)); // ← VALIDATING
            when(userRepository.findById(5L)).thenReturn(Optional.of(admin));
            when(ticketRepository.save(any())).thenReturn(validatingTicket);
            when(ticketMapper.toDetailResponse(any())).thenReturn(expected);

            TicketDetailResponse result = ticketService.rejectTicket(5L, 100L, request);

            assertThat(result.getStatus()).isEqualTo(TicketStatus.REJECTED);
            verify(ticketRepository).save(argThat(t ->
                    t.getStatus() == TicketStatus.REJECTED
                            && t.getRejectionJustification()
                            .equals("El código fue usado correctamente")
                            && t.getResolvedAt() != null
            ));
        }

        @Test
        @DisplayName("AP-44 | rejectTicket sobre ticket OPEN (no VALIDATING) — IllegalStateException")
        void rejectTicket_wrongStatus_throwsException() {
            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(openTicket)); // OPEN, no VALIDATING

            assertThatThrownBy(() ->
                    ticketService.rejectTicket(5L, 100L,
                            new RejectTicketRequest("Justificación", null))
            ).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("solo se puede rechazar un ticket VALIDATING");

            verify(ticketRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AP-45 a AP-46 | closeAsInvalid
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("closeAsInvalid")
    class CloseAsInvalidTests {

        @Test
        @DisplayName("AP-45 | Admin cierra ticket OPEN como inválido — estado CLOSED_INVALID")
        void closeAsInvalid_fromOpen_success() {
            TicketDetailResponse expected = TicketDetailResponse.builder()
                    .id(100L)
                    .status(TicketStatus.CLOSED_INVALID)
                    .resolution(TicketResolution.CLOSED_INVALID)
                    .build();

            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(openTicket));
            when(userRepository.findById(5L)).thenReturn(Optional.of(admin));
            when(ticketRepository.save(any())).thenReturn(openTicket);
            when(ticketMapper.toDetailResponse(any())).thenReturn(expected);

            TicketDetailResponse result = ticketService.closeAsInvalid(5L, 100L);

            assertThat(result.getStatus()).isEqualTo(TicketStatus.CLOSED_INVALID);
            verify(ticketRepository).save(argThat(t ->
                    t.getStatus() == TicketStatus.CLOSED_INVALID
                            && t.getResolvedAt() != null
            ));
        }

        @Test
        @DisplayName("AP-46 | Admin cierra ticket VALIDATING como inválido — estado CLOSED_INVALID")
        void closeAsInvalid_fromValidating_success() {
            TicketDetailResponse expected = TicketDetailResponse.builder()
                    .id(100L)
                    .status(TicketStatus.CLOSED_INVALID)
                    .build();

            when(ticketRepository.findByIdWithDetails(100L))
                    .thenReturn(Optional.of(validatingTicket));
            when(userRepository.findById(5L)).thenReturn(Optional.of(admin));
            when(ticketRepository.save(any())).thenReturn(validatingTicket);
            when(ticketMapper.toDetailResponse(any())).thenReturn(expected);

            assertThatCode(() ->
                    ticketService.closeAsInvalid(5L, 100L)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("AP-47 | closeAsInvalid sobre ticket inexistente — IllegalArgumentException")
        void closeAsInvalid_notFound_throwsException() {
            when(ticketRepository.findByIdWithDetails(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    ticketService.closeAsInvalid(5L, 999L)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ticket no encontrado");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AP-48 a AP-49 | getAllTickets
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getAllTickets")
    class GetAllTicketsTests {

        @Test
        @DisplayName("AP-48 | Admin lista todos los tickets sin filtro — paginación correcta")
        void getAllTickets_noFilter_returnsAll() {
            Pageable pageable = PageRequest.of(0, 20);
            TicketSummaryResponse summary = new TicketSummaryResponse(
                    100L, "REF-001", "Steam Key",
                    TicketType.REPLACEMENT, TicketStatus.OPEN,
                    null, LocalDateTime.now(), null
            );
            Page<SupportTicket> page = new PageImpl<>(List.of(openTicket));

            when(ticketRepository.findAll(pageable)).thenReturn(page);
            when(ticketMapper.toSummaryResponse(openTicket)).thenReturn(summary);

            Page<TicketSummaryResponse> result = ticketService.getAllTickets(null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(ticketRepository, never()).findByStatus(any(), any());
        }

        @Test
        @DisplayName("AP-49 | Admin filtra tickets por status=OPEN — solo retorna OPEN")
        void getAllTickets_filterByStatus_returnsOnlyOpen() {
            Pageable pageable = PageRequest.of(0, 20);
            TicketSummaryResponse summary = new TicketSummaryResponse(
                    100L, "REF-001", "Steam Key",
                    TicketType.REPLACEMENT, TicketStatus.OPEN,
                    null, LocalDateTime.now(), null
            );
            Page<SupportTicket> page = new PageImpl<>(List.of(openTicket));

            when(ticketRepository.findByStatus(TicketStatus.OPEN, pageable))
                    .thenReturn(page);
            when(ticketMapper.toSummaryResponse(openTicket)).thenReturn(summary);

            Page<TicketSummaryResponse> result =
                    ticketService.getAllTickets("OPEN", pageable);

            assertThat(result.getContent()).allMatch(t ->
                    t.status() == TicketStatus.OPEN
            );
            verify(ticketRepository, never()).findAll(any(Pageable.class));
        }
    }
}