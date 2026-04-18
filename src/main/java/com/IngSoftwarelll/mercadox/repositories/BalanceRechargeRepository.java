package com.IngSoftwarelll.mercadox.repositories;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.BalanceRecharge;

@Repository
public interface BalanceRechargeRepository extends JpaRepository<BalanceRecharge, Long> {

    Page<BalanceRecharge> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}