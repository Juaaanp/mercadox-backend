package com.IngSoftwarelll.mercadox.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.IngSoftwarelll.mercadox.models.enums.TicketResolution;
import com.IngSoftwarelll.mercadox.models.enums.TicketStatus;
import com.IngSoftwarelll.mercadox.models.enums.TicketType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "support_tickets", indexes = {
        @Index(name = "idx_ticket_user",   columnList = "user_id"),
        @Index(name = "idx_ticket_status", columnList = "status"),
        @Index(name = "idx_ticket_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "purchase", "purchaseItem", "newAssignedStock", "messages"})
@EqualsAndHashCode(exclude = {"user", "purchase", "purchaseItem", "newAssignedStock", "messages"})
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== RELACIONES =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_item_id", nullable = false)
    private PurchaseItem purchaseItem;

    // Nuevo código asignado en caso de reemplazo
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_product_stock_id")
    private ProductStock newAssignedStock;

    // ===== CLASIFICACIÓN =====

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketType type; // REPLACEMENT o REFUND

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private TicketResolution resolution; // Null hasta que se resuelva

    // ===== CONTENIDO =====

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason; // Descripción del problema por el cliente

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes; // Notas internas del admin

    @Column(name = "rejection_justification", columnDefinition = "TEXT")
    private String rejectionJustification; // Enviada al cliente si se rechaza

    // ===== HISTORIAL DE MENSAJES =====

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TicketMessage> messages = new ArrayList<>();

    // ===== AUDITORÍA =====

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== MÉTODOS DE NEGOCIO =====

    public void markAsValidating() {
        this.status = TicketStatus.VALIDATING;
    }

    public void resolveWithReplacement(ProductStock newStock) {
        this.newAssignedStock = newStock;
        this.status = TicketStatus.RESOLVED;
        this.resolution = TicketResolution.REPLACEMENT_SENT;
        this.resolvedAt = LocalDateTime.now();
    }

    public void resolveWithRefund() {
        this.status = TicketStatus.RESOLVED;
        this.resolution = TicketResolution.REFUND_PROCESSED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void reject(String justification) {
        this.rejectionJustification = justification;
        this.status = TicketStatus.REJECTED;
        this.resolution = TicketResolution.REJECTED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void closeAsInvalid() {
        this.status = TicketStatus.CLOSED_INVALID;
        this.resolution = TicketResolution.CLOSED_INVALID;
        this.resolvedAt = LocalDateTime.now();
    }

    public void addMessage(TicketMessage message) {
        messages.add(message);
        message.setTicket(this);
    }
}