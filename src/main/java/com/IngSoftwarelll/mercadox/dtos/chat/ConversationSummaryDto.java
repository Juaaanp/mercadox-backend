package com.IngSoftwarelll.mercadox.dtos.chat;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data @Builder
    public class ConversationSummaryDto {
        private Long              id;
        private Long              userId;
        private String            userEmail;
        private String            status;
        private int               unreadByAdmin;
        private int               unreadByUser;
        private LocalDateTime     updatedAt;
        private LocalDateTime     createdAt;
        private ChatMessageDto    lastMessage;
    }