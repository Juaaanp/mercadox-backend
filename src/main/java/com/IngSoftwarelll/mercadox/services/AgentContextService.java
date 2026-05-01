package com.IngSoftwarelll.mercadox.services;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.IngSoftwarelll.mercadox.dtos.agent.AgentContextDto;
import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.models.enums.TicketStatus;
import com.IngSoftwarelll.mercadox.repositories.ProductRepository;
import com.IngSoftwarelll.mercadox.repositories.PurchaseRepository;
import com.IngSoftwarelll.mercadox.repositories.SupportTicketRepository;
import com.IngSoftwarelll.mercadox.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentContextService {
 
    private final UserRepository        userRepo;
    private final PurchaseRepository    purchaseRepo;
    private final SupportTicketRepository      supportTicketRepo;
    private final ProductRepository     productRepo;
 
    @Transactional(readOnly = true)
    public AgentContextDto buildContext(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
 
        // Recent purchases (last 10)
        List<AgentContextDto.Purchase> purchases = purchaseRepo
            .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10))
            .getContent()
            .stream()
            .flatMap(p -> p.getItems().stream()
                .map(pi -> AgentContextDto.Purchase.builder()
                    .referenceId(p.getReferenceId())
                    .productName(pi.getProduct().getName())
                    .deliveredCode(pi.getDeliveredCode() != null ? pi.getDeliveredCode() : "N/A")
                    .price(pi.getPriceAtPurchase())
                    .purchasedAt(p.getCreatedAt())
                    .status(p.getStatus() != null ? p.getStatus().name() : "COMPLETED")
                    .build()))
            .toList();
 
        // Active tickets (OPEN + VALIDATING)
        List<AgentContextDto.Ticket> tickets = supportTicketRepo
            .findByUserIdAndStatusIn(userId,
                List.of(TicketStatus.OPEN, TicketStatus.VALIDATING))
            .stream()
            .map(t -> AgentContextDto.Ticket.builder()
                .id(t.getId())
                .productName(t.getPurchaseItem().getProduct().getName())
                .status(t.getStatus().name())
                .type(t.getType().name())
                .reason(t.getReason())
                .createdAt(t.getCreatedAt())
                .build())
            .toList();
 
        // Featured products (top 20 with stock)
        List<AgentContextDto.Product> products = productRepo
            .findAllProducts(PageRequest.of(0, 20))
            .getContent()
            .stream()
            .map(p -> AgentContextDto.Product.builder()
                .id(p.getId())
                .name(p.getName())
                .category(p.getProductCategory() != null ? p.getProductCategory().getName() : "General")
                .price(p.getPrice())
                .inStock(p.getAvailableStock() > 0)
                .description(p.getDescription())
                .build())
            .toList();
 
        return AgentContextDto.builder()
            .user(AgentContextDto.UserInfo.builder()
                .id(user.getId())
                .name(user.getEmail())
                .email(user.getEmail())
                .balance(user.getBalance())
                .build())
            .recentPurchases(purchases)
            .activeTickets(tickets)
            .availableProducts(products)
            .build();
    }
}