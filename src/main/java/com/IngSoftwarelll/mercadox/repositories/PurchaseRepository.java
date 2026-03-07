package com.IngSoftwarelll.mercadox.repositories;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.Purchase;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Page<Purchase> findByUserId(Long userId, Pageable pageable);

}
