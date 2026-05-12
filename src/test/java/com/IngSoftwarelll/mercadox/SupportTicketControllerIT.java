package com.IngSoftwarelll.mercadox;


import com.IngSoftwarelll.mercadox.dtos.ticket.request.*;
import com.IngSoftwarelll.mercadox.models.*;
import com.IngSoftwarelll.mercadox.models.enums.*;
import com.IngSoftwarelll.mercadox.repositories.*;
import com.IngSoftwarelll.mercadox.security.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@TestConfiguration
class TestConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // rollback automático tras cada test — BD limpia entre tests
@DisplayName("SupportTicketController — Pruebas de integración")
class SupportTicketControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtTokenService jwtTokenService;

    // Repositorios para sembrar datos
    @Autowired UserRepository userRepository;
    @Autowired PurchaseRepository purchaseRepository;
    @Autowired PurchaseItemRepository purchaseItemRepository;
    @Autowired ProductRepository productRepository;
    @Autowired SupportTicketRepository ticketRepository;

    private String consumerToken;
    private String adminToken;

    // IDs sembrados — se usan en los tests
    private Long consumerTicketId;
    private Long purchaseId;
    private Long purchaseItemId;

    @BeforeEach
    void setUp() {
        // ── Usuarios ──────────────────────────────────────────────────────────
        User consumer = new User();
        consumer.setEmail("consumer@test.com");
        consumer.setPassword("$2a$10$hashedpassword");
        consumer.setPhoneNumber("3001111111");
        consumer.setRole(UserRole.CONSUMER);
        consumer.setBalance(new BigDecimal("200000"));
        consumer = userRepository.save(consumer);

        User admin = new User();
        admin.setEmail("admin@test.com");
        admin.setPassword("$2a$10$hashedpassword");
        admin.setPhoneNumber("3002222222");
        admin.setRole(UserRole.ADMIN);
        admin.setBalance(BigDecimal.ZERO);
        admin = userRepository.save(admin);

        // ── Tokens JWT ────────────────────────────────────────────────────────
        UsernamePasswordAuthenticationToken consumerAuth =
                new UsernamePasswordAuthenticationToken(
                        consumer.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("CONSUMER"))
                );
        consumerToken = jwtTokenService.generateAccessToken(consumerAuth);

        UsernamePasswordAuthenticationToken adminAuth =
                new UsernamePasswordAuthenticationToken(
                        admin.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ADMIN"))
                );
        adminToken = jwtTokenService.generateAccessToken(adminAuth);

        // ── Compra y PurchaseItem ─────────────────────────────────────────────
        Purchase purchase = Purchase.builder()
                .referenceId("REF-IT-001")
                .user(consumer)
                .total(new BigDecimal("50000"))
                .status(PurchaseStatus.COMPLETED)
                .contactEmail(consumer.getEmail())
                .build();
        purchase = purchaseRepository.save(purchase);
        purchaseId = purchase.getId();

        PurchaseItem item = PurchaseItem.builder()
                .purchase(purchase)
                .priceAtPurchase(new BigDecimal("50000"))
                .quantity(1)
                .subtotal(new BigDecimal("50000"))
                .deliveredCode("AAAA-BBBB-CCCC")
                .status(PurchaseItemStatus.DELIVERED)
                .build();
        item = purchaseItemRepository.save(item);
        purchaseItemId = item.getId();

        // ── Ticket del consumer (dueño = consumer) ────────────────────────────
        SupportTicket ticket = SupportTicket.builder()
                .user(consumer)
                .purchase(purchase)
                .purchaseItem(item)
                .type(TicketType.REPLACEMENT)
                .status(TicketStatus.OPEN)
                .reason("El código no funciona")
                .build();
        ticket = ticketRepository.save(ticket);
        consumerTicketId = ticket.getId();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /tickets — CP-21
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /tickets")
    class CreateTicketIT {

        @Test
        @DisplayName("IT-01 | CONSUMER crea ticket — 201 Created — CP-21")
        void createTicket_returns201() throws Exception {
            CreateTicketRequest req = new CreateTicketRequest(
                    purchaseId,
                    purchaseItemId,
                    TicketType.REPLACEMENT,
                    "El código que recibí da error"
            );

            mockMvc.perform(post("/tickets")
                            .header("Authorization", "Bearer " + consumerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.type").value("REPLACEMENT"));
        }

        @Test
        @DisplayName("IT-02 | Sin token JWT → 403 Forbidden — CP-09")
        void createTicket_withoutToken_returns403() throws Exception {
            CreateTicketRequest req = new CreateTicketRequest(
                    purchaseId, purchaseItemId, TicketType.REPLACEMENT, "Razón"
            );

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("IT-03 | ADMIN intenta crear ticket → 403 Forbidden (@PreAuthorize CONSUMER)")
        void createTicket_withAdminToken_returns403() throws Exception {
            CreateTicketRequest req = new CreateTicketRequest(
                    purchaseId, purchaseItemId, TicketType.REPLACEMENT, "Razón"
            );

            mockMvc.perform(post("/tickets")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /tickets/my — CP-25
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /tickets/my")
    class GetMyTicketsIT {

        @Test
        @DisplayName("IT-04 | CONSUMER lista sus tickets — 200 OK con estructura paginada — CP-25")
        void getMyTickets_returns200() throws Exception {
            mockMvc.perform(get("/tickets/my")
                            .header("Authorization", "Bearer " + consumerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").exists())
                    .andExpect(jsonPath("$.totalPages").exists());
        }

        @Test
        @DisplayName("IT-05 | Sin token → 403 Forbidden")
        void getMyTickets_withoutToken_returns403() throws Exception {
            mockMvc.perform(get("/tickets/my"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /tickets/{id} — CP-25, CP-26
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /tickets/{id}")
    class GetTicketDetailIT {

        @Test
        @DisplayName("IT-06 | CONSUMER ve su propio ticket — 200 OK — CP-25")
        void getTicketDetail_ownerReturns200() throws Exception {
            mockMvc.perform(get("/tickets/" + consumerTicketId)
                            .header("Authorization", "Bearer " + consumerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(consumerTicketId));
        }

        @Test
        @DisplayName("IT-07 | CONSUMER intenta ver ticket ajeno — 403 — CP-26")
        void getTicketDetail_nonOwnerReturns403() throws Exception {
            // adminToken pertenece a un usuario distinto al dueño del ticket
            // pero admin=true en el controlador — usamos un segundo consumer
            User otherConsumer = new User();
            otherConsumer.setEmail("other@test.com");
            otherConsumer.setPassword("$2a$10$hashedpassword");
            otherConsumer.setPhoneNumber("3003333333");
            otherConsumer.setRole(UserRole.CONSUMER);
            otherConsumer.setBalance(BigDecimal.ZERO);
            userRepository.save(otherConsumer);

            UsernamePasswordAuthenticationToken otherAuth =
                    new UsernamePasswordAuthenticationToken(
                            otherConsumer.getEmail(), null,
                            List.of(new SimpleGrantedAuthority("CONSUMER"))
                    );
            String otherToken = jwtTokenService.generateAccessToken(otherAuth);

            mockMvc.perform(get("/tickets/" + consumerTicketId)
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("IT-08 | ADMIN ve cualquier ticket — 200 OK")
        void getTicketDetail_adminReturns200() throws Exception {
            mockMvc.perform(get("/tickets/" + consumerTicketId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(consumerTicketId));
        }

        @Test
        @DisplayName("IT-09 | Ticket inexistente — 404 con ErrorResponse sin stack trace")
        void getTicketDetail_notFoundReturns404() throws Exception {
            mockMvc.perform(get("/tickets/99999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.stackTrace").doesNotExist());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /tickets/{id}/messages
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /tickets/{id}/messages")
    class SendMessageIT {

        @Test
        @DisplayName("IT-10 | CONSUMER envía mensaje a su ticket — 200 OK")
        void sendMessage_consumerReturns200() throws Exception {
            SendMessageRequest req = new SendMessageRequest("Adjunto captura de pantalla");

            mockMvc.perform(post("/tickets/" + consumerTicketId + "/messages")
                            .header("Authorization", "Bearer " + consumerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages").isArray());
        }

        @Test
        @DisplayName("IT-11 | ADMIN responde al ticket — 200 OK")
        void sendMessage_adminReturns200() throws Exception {
            SendMessageRequest req = new SendMessageRequest("Estamos revisando su caso");

            mockMvc.perform(post("/tickets/" + consumerTicketId + "/messages")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /tickets/admin
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /tickets/admin")
    class GetAllTicketsIT {

        @Test
        @DisplayName("IT-12 | ADMIN lista todos los tickets — 200 OK con paginación")
        void getAllTickets_adminReturns200() throws Exception {
            mockMvc.perform(get("/tickets/admin")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").exists());
        }

        @Test
        @DisplayName("IT-13 | CONSUMER intenta acceder a /tickets/admin — 403 Forbidden")
        void getAllTickets_consumerReturns403() throws Exception {
            mockMvc.perform(get("/tickets/admin")
                            .header("Authorization", "Bearer " + consumerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("IT-14 | ADMIN filtra tickets por status=OPEN — solo retorna OPEN")
        void getAllTickets_filterByStatus() throws Exception {
            mockMvc.perform(get("/tickets/admin?status=OPEN")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].status",
                            org.hamcrest.Matchers.everyItem(
                                    org.hamcrest.Matchers.is("OPEN"))));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /tickets/{id}/resolve/replacement — CP-23
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /tickets/{id}/resolve/replacement")
    class ResolveWithReplacementIT {

        @Test
        @DisplayName("IT-15 | Admin resuelve con reemplazo — 200 OK, estado RESOLVED — CP-23")
        void resolveWithReplacement_returns200() throws Exception {
            ResolveWithReplacementRequest req = new ResolveWithReplacementRequest(
                    purchaseItemId,     // newProductStockId — ID de stock existente
                    "Reemplazo aprobado" // adminNotes
            );

            mockMvc.perform(put("/tickets/" + consumerTicketId + "/resolve/replacement")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"));
        }

        @Test
        @DisplayName("IT-16 | CONSUMER intenta resolver ticket — 403 Forbidden")
        void resolveWithReplacement_consumerReturns403() throws Exception {
            ResolveWithReplacementRequest req = new ResolveWithReplacementRequest(
                    purchaseItemId, null
            );

            mockMvc.perform(put("/tickets/" + consumerTicketId + "/resolve/replacement")
                            .header("Authorization", "Bearer " + consumerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /tickets/{id}/reject
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /tickets/{id}/reject")
    class RejectTicketIT {

        @Test
        @DisplayName("IT-17 | Admin rechaza ticket — 200 OK, estado REJECTED")
        void rejectTicket_returns200() throws Exception {
            RejectTicketRequest req = new RejectTicketRequest(
                    "El código fue usado correctamente",
                    null // adminNotes
            );

            mockMvc.perform(put("/tickets/" + consumerTicketId + "/reject")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /tickets/{id}/close-invalid
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /tickets/{id}/close-invalid")
    class CloseAsInvalidIT {

        @Test
        @DisplayName("IT-18 | Admin cierra ticket como inválido — 200 OK, estado CLOSED_INVALID")
        void closeAsInvalid_returns200() throws Exception {
            mockMvc.perform(put("/tickets/" + consumerTicketId + "/close-invalid")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLOSED_INVALID"));
        }
    }
}