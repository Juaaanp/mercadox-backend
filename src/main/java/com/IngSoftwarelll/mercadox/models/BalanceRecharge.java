package com.IngSoftwarelll.mercadox.models;



import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.IngSoftwarelll.mercadox.models.enums.PaymentMethod;
import com.IngSoftwarelll.mercadox.models.enums.PurchaseStatus;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "balance_recharges", indexes = {
        @Index(name = "idx_recharge_user",    columnList = "user_id"),
        @Index(name = "idx_recharge_status",  columnList = "status"),
        @Index(name = "idx_recharge_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
@EqualsAndHashCode(exclude = "user")
public class BalanceRecharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private PurchaseStatus status = PurchaseStatus.PENDING;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_holder", length = 100)
    private String cardHolder;

    @Column(name = "expiry_date", length = 5)
    private String expiryDate;

    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
