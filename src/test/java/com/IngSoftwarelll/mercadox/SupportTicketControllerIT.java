package com.IngSoftwarelll.mercadox;

import com.IngSoftwarelll.mercadox.dtos.ticket.request.*;
import com.IngSoftwarelll.mercadox.models.*;
import com.IngSoftwarelll.mercadox.models.enums.*;
import com.IngSoftwarelll.mercadox.repositories.*;
import com.IngSoftwarelll.mercadox.security.CustomUserDetails;
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
import org.springframework.context.annotation.Import;
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
@Transactional
@Import(TestConfig.class)
@DisplayName("SupportTicketController — Pruebas de integración")
class SupportTicketControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtTokenService jwtTokenService;

    @Autowired UserRepository userRepository;
    @Autowired PurchaseRepository purchaseRepository;
    @Autowired PurchaseItemRepository purchaseItemRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ProductCategoryRepository productCategoryRepository;
    @Autowired SupportTicketRepository ticketRepository;
    @Autowired ProductStockRepository productStockRepository;
    private String consumerToken;
    private String adminToken;

    private Long consumerTicketId;
    private Long purchaseId;
    private Long purchaseItemId;
    // Item exclusivo para IT-01 (crear ticket nuevo sin colisión)
    private Long freshPurchaseItemId;
    private Long productStockId;

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

        // ── Authorities ───────────────────────────────────────────────────────
        List<SimpleGrantedAuthority> consumerAuthorities =
                List.of(new SimpleGrantedAuthority("CONSUMER"));
        List<SimpleGrantedAuthority> adminAuthorities =
                List.of(new SimpleGrantedAuthority("ADMIN"));

        // ── CustomUserDetails ─────────────────────────────────────────────────
        CustomUserDetails consumerDetails = new CustomUserDetails(consumer, consumerAuthorities);
        CustomUserDetails adminDetails = new CustomUserDetails(admin, adminAuthorities);

        // ── JWT Tokens ────────────────────────────────────────────────────────
        UsernamePasswordAuthenticationToken consumerAuth =
                new UsernamePasswordAuthenticationToken(consumerDetails, null, consumerAuthorities);
        consumerToken = jwtTokenService.generateAccessToken(consumerAuth);

        UsernamePasswordAuthenticationToken adminAuth =
                new UsernamePasswordAuthenticationToken(adminDetails, null, adminAuthorities);
        adminToken = jwtTokenService.generateAccessToken(adminAuth);

        // ── ProductCategory ───────────────────────────────────────────────────
        ProductCategory category = new ProductCategory();
        category.setName("Categoría de prueba");
        category = productCategoryRepository.save(category);

        // ── Product ───────────────────────────────────────────────────────────
        Product product = new Product();
        product.setAdmin(admin);
        product.setName("Producto de prueba");
        product.setImageUrl("https://img.example.com/test.jpg");
        product.setPrice(new BigDecimal("50000"));
        product.setProductCategory(category);
        product.setDescription("Descripción de prueba");
        product = productRepository.save(product);


        ProductStock stock = ProductStock.builder()
                .product(product)
                .code("STOCK-TEST-001")
                .status(StockStatus.AVAILABLE)
                .build();
        stock = productStockRepository.save(stock);
        productStockId = stock.getId();

        // ── Compra ────────────────────────────────────────────────────────────
        Purchase purchase = Purchase.builder()
                .referenceId("REF-IT-001")
                .user(consumer)
                .total(new BigDecimal("100000"))
                .status(PurchaseStatus.COMPLETED)
                .contactEmail(consumer.getEmail())
                .build();
        purchase = purchaseRepository.save(purchase);
        purchaseId = purchase.getId();

        // ── PurchaseItem usado por el ticket sembrado ──────────────────────────
        PurchaseItem item = PurchaseItem.builder()
                .purchase(purchase)
                .product(product)
                .priceAtPurchase(new BigDecimal("50000"))
                .quantity(1)
                .subtotal(new BigDecimal("50000"))
                .deliveredCode("AAAA-BBBB-CCCC")
                .status(PurchaseItemStatus.DELIVERED)
                .build();
        item = purchaseItemRepository.save(item);
        purchaseItemId = item.getId();

        // ── PurchaseItem fresco (sin ticket) para IT-01 ───────────────────────
        PurchaseItem freshItem = PurchaseItem.builder()
                .purchase(purchase)
                .product(product)
                .priceAtPurchase(new BigDecimal("50000"))
                .quantity(1)
                .subtotal(new BigDecimal("50000"))
                .deliveredCode("DDDD-EEEE-FFFF")
                .status(PurchaseItemStatus.DELIVERED)
                .build();
        freshItem = purchaseItemRepository.save(freshItem);
        freshPurchaseItemId = freshItem.getId();

        // ── Ticket sembrado en estado VALIDATING (requerido por resolve/reject) ─
        SupportTicket ticket = SupportTicket.builder()
                .user(consumer)
                .purchase(purchase)
                .purchaseItem(item)
                .type(TicketType.REPLACEMENT)
                .status(TicketStatus.VALIDATING)   // ← cambiado de OPEN a VALIDATING
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
            // Usa el item fresco que no tiene ticket asociado
            CreateTicketRequest req = new CreateTicketRequest(
                    purchaseId,
                    freshPurchaseItemId,   // ← item sin ticket previo
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
        @DisplayName("IT-02 | Sin token JWT → 401 Unauthorized — CP-09")
            // NOTA: la API responde 401 (no autenticado) cuando falta el token,
            // no 403 (autenticado pero sin permiso). Se ajusta la expectativa.
        void createTicket_withoutToken_returns401() throws Exception {
            CreateTicketRequest req = new CreateTicketRequest(
                    purchaseId, purchaseItemId, TicketType.REPLACEMENT, "Razón"
            );

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());   // 401
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
        @DisplayName("IT-05 | Sin token → 401 Unauthorized")
        void getMyTickets_withoutToken_returns401() throws Exception {
            mockMvc.perform(get("/tickets/my"))
                    .andExpect(status().isUnauthorized());   // 401
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
            User otherConsumer = new User();
            otherConsumer.setEmail("other@test.com");
            otherConsumer.setPassword("$2a$10$hashedpassword");
            otherConsumer.setPhoneNumber("3003333333");
            otherConsumer.setRole(UserRole.CONSUMER);
            otherConsumer.setBalance(BigDecimal.ZERO);
            otherConsumer = userRepository.save(otherConsumer);

            List<SimpleGrantedAuthority> otherAuthorities =
                    List.of(new SimpleGrantedAuthority("CONSUMER"));

            // ← CustomUserDetails correcto, igual que en setUp()
            CustomUserDetails otherDetails = new CustomUserDetails(otherConsumer, otherAuthorities);

            UsernamePasswordAuthenticationToken otherAuth =
                    new UsernamePasswordAuthenticationToken(
                            otherDetails, null, otherAuthorities
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
            // REQUISITO: GlobalExceptionHandler debe mapear IllegalArgumentException → 404.
            // Si aún responde 500, corrige el handler así:
            //   @ExceptionHandler(IllegalArgumentException.class)
            //   @ResponseStatus(HttpStatus.NOT_FOUND)
            //   public ErrorResponse handleNotFound(IllegalArgumentException ex) { ... }
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
        @DisplayName("IT-14 | ADMIN filtra tickets por status=VALIDATING — solo retorna VALIDATING")
        void getAllTickets_filterByStatus() throws Exception {
            // El ticket sembrado está en VALIDATING, filtramos por ese estado
            mockMvc.perform(get("/tickets/admin?status=VALIDATING")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].status",
                            org.hamcrest.Matchers.everyItem(
                                    org.hamcrest.Matchers.is("VALIDATING"))));
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
                    productStockId,        // ← ID de ProductStock AVAILABLE
                    "Reemplazo aprobado"
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
                    null
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