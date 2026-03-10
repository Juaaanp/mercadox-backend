package com.IngSoftwarelll.mercadox.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
