package com.IngSoftwarelll.mercadox.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.IngSoftwarelll.mercadox.models.enums.PurchaseStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "purchases", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_purchase_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "items"})
@EqualsAndHashCode(exclude = {"user", "items"})
public class Purchase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", nullable = false, updatable = false)
    private String referenceId;
    
    // ===== RELACIONES =====
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseItem> items = new ArrayList<>();
    
    // ===== INFORMACIÓN FINANCIERA =====
    
    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;
    
    // ===== ESTADO DE LA COMPRA =====
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PurchaseStatus status = PurchaseStatus.PENDING;
    
    // ===== INFORMACIÓN DE ENTREGA DIGITAL =====
    
    @Column(name = "contact_email", length = 255)
    private String contactEmail; // Email donde se envían las credenciales
    
    // ===== AUDITORÍA =====
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt; // Cuándo se completó la compra
    
    // ===== MÉTODOS DE CICLO DE VIDA =====
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // ===== MÉTODOS DE NEGOCIO =====
    
    public void addItem(PurchaseItem item) {
        items.add(item);
        item.setPurchase(this);
    }
    
    public void removeItem(PurchaseItem item) {
        items.remove(item);
        item.setPurchase(null);
    }
    
    public void calculateTotals() {
        this.total = items.stream()
            .map(PurchaseItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public void markAsCompleted() {
        this.status = PurchaseStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
