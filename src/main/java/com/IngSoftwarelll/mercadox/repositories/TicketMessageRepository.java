package com.IngSoftwarelll.mercadox.repositories;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.TicketMessage;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
