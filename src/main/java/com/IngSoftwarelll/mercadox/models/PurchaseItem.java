package com.IngSoftwarelll.mercadox.models;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.IngSoftwarelll.mercadox.models.enums.PurchaseItemStatus;

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
@Table(name = "purchase_items",
    indexes = {
        @Index(name = "idx_purchase_items_product_deliveredat", columnList = "product_id, delivered_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"purchase", "product", "assignedProductStock"})
@EqualsAndHashCode(exclude = {"purchase", "product", "assignedProductStock"})
public class PurchaseItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_stock_id")
    private ProductStock assignedProductStock;
    
    @Column(nullable = false)
    private Integer quantity;
    
    // Precio al momento de la compra (importante para histórico)
    @Column(name = "price_at_purchase", nullable = false, precision = 19, scale = 2)
    private BigDecimal priceAtPurchase;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;  // quantity * priceAtPurchase
    
    @Column(name = "delivered_code", columnDefinition = "TEXT")
    private String deliveredCode; // El código que se le entregó al cliente
    
    @Column(name = "delivered_at")
    private ZonedDateTime deliveredAt; // Cuándo se entregó

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PurchaseItemStatus status = PurchaseItemStatus.PENDING;
    
    @PrePersist
    @PreUpdate
    protected void calculateSubtotal() {
        this.subtotal = priceAtPurchase.multiply(new BigDecimal(quantity));
    }

    public void assignProductStock(ProductStock stock) {
        this.assignedProductStock = stock;
        this.deliveredCode = stock.getCode();
        this.deliveredAt = ZonedDateTime.now();
        this.status = PurchaseItemStatus.DELIVERED;
    }

    public boolean isDelivered() {
        return status == PurchaseItemStatus.DELIVERED;
    }

    public boolean canBeReviewed() {
        return this.isDelivered();
    }
}
