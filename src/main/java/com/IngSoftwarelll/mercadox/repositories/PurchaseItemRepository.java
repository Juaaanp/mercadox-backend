package com.IngSoftwarelll.mercadox.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.PurchaseItem;


@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    @Query("""
                SELECT pi FROM PurchaseItem pi
                LEFT JOIN FETCH pi.product
                LEFT JOIN FETCH pi.purchase p
                WHERE pi.id = :purchaseItemId
                AND p.user.id = :userId
            """)
    Optional<PurchaseItem> findByIdAndUserId(
            @Param("purchaseItemId") Long purchaseItemId,
            @Param("userId") Long userId);


}
