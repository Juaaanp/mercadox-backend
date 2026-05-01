package com.IngSoftwarelll.mercadox.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.IngSoftwarelll.mercadox.models.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
 
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
 
    Page<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);
 
    @Modifying
    @Query("""
        UPDATE ChatMessage m
        SET m.readByOther = true
        WHERE m.conversation.id = :conversationId
          AND m.senderRole = :role
          AND m.readByOther = false
    """)
    int markAllAsRead(@Param("conversationId") Long conversationId,
                      @Param("role") ChatMessage.SenderRole role);
}
 