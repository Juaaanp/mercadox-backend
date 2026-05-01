package com.IngSoftwarelll.mercadox.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.ChatConversation;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
 
    Optional<ChatConversation> findByUserId(Long userId);
 
    @Query("""
        SELECT c FROM ChatConversation c
        LEFT JOIN FETCH c.user u
        ORDER BY c.updatedAt DESC
    """)
    Page<ChatConversation> findAllOrderByUpdatedAtDesc(Pageable pageable);
 
    @Query("""
        SELECT c FROM ChatConversation c
        WHERE c.unreadByAdmin > 0
        ORDER BY c.updatedAt DESC
    """)
    Page<ChatConversation> findWithUnreadMessages(Pageable pageable);
}
 