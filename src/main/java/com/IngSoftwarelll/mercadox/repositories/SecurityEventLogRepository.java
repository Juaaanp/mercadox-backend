package com.IngSoftwarelll.mercadox.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.SecurityEventLog;

@Repository
public interface SecurityEventLogRepository extends JpaRepository<SecurityEventLog, Long> {
}