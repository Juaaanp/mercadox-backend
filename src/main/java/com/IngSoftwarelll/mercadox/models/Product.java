package com.IngSoftwarelll.mercadox.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.IngSoftwarelll.mercadox.exceptions.InsufficientStockException;
import com.IngSoftwarelll.mercadox.models.enums.StockStatus;




import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "products")
@Getter
@Setter
@ToString(exclude = {"stockItems", "productCategory" })
@EqualsAndHashCode(exclude = {"stockItems", "productCategory" })
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Transient
    @Column(nullable = false)
    private Integer stock;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductStock> stockItems = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id", nullable = false)
    private ProductCategory productCategory;

    // Campos adicionales recomendados
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public Integer getAvailableStock() {
        return (int) stockItems.stream()
                .filter(stock -> stock.getStatus() == StockStatus.AVAILABLE).count();
    }

    public void updateStockCount() {
        this.stock = getAvailableStock();
    }

    public ProductStock getNextAvailableCode() {
        return stockItems.stream().filter(stock -> stock.getStatus() == StockStatus.AVAILABLE)
                .findFirst().orElseThrow(() -> new InsufficientStockException(this.name));
    }

}
