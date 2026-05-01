package com.IngSoftwarelll.mercadox.dtos.agent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AgentContextDto {
 
    private UserInfo        user;
    private List<Purchase>  recentPurchases;
    private List<Ticket>    activeTickets;
    private List<Product>   availableProducts; // top 20 featured
 
    @Data @Builder
    public static class UserInfo {
        private Long       id;
        private String     name;
        private String     email;
        private BigDecimal balance;
    }
 
    @Data @Builder
    public static class Purchase {
        private String        referenceId;
        private String        productName;
        private String        deliveredCode;
        private BigDecimal    price;
        private LocalDateTime purchasedAt;
        private String        status;
    }
 
    @Data @Builder
    public static class Ticket {
        private Long          id;
        private String        productName;
        private String        status;
        private String        type;
        private String        reason;
        private LocalDateTime createdAt;
    }
 
    @Data @Builder
    public static class Product {
        private Long       id;
        private String     name;
        private String     category;
        private BigDecimal price;
        private boolean    inStock;
        private String     description;
    }
}