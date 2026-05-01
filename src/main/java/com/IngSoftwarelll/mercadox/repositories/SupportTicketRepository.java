package com.IngSoftwarelll.mercadox.repositories;


import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.SupportTicket;
import com.IngSoftwarelll.mercadox.models.enums.TicketStatus;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    // Cliente: ver sus propios tickets paginados
    Page<SupportTicket> findByUserId(Long userId, Pageable pageable);

    // Cliente: ver sus propios tickets por status
    java.util.List<SupportTicket> findByUserIdAndStatusIn(Long userId, java.util.List<TicketStatus> statuses);

    // Admin: filtrar por status
    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);

    // Admin: todos los tickets paginados
    Page<SupportTicket> findAll(Pageable pageable);

    // Evitar tickets duplicados sobre el mismo purchaseItem abierto
    boolean existsByPurchaseItemIdAndStatusIn(Long purchaseItemId, java.util.List<TicketStatus> statuses);

    // Fetch completo para evitar N+1 al cargar un ticket con sus relaciones
    @Query("""
        SELECT t FROM SupportTicket t
        LEFT JOIN FETCH t.user
        LEFT JOIN FETCH t.purchase
        LEFT JOIN FETCH t.purchaseItem pi
        LEFT JOIN FETCH pi.product
        LEFT JOIN FETCH t.newAssignedStock
        WHERE t.id = :id
    """)
    Optional<SupportTicket> findByIdWithDetails(@Param("id") Long id);
}