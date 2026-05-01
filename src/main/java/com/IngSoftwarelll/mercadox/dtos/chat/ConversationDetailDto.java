package com.IngSoftwarelll.mercadox.dtos.chat;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data @Builder
    public class ConversationDetailDto {
        private Long                   id;
        private Long                   userId;
        private String                 userEmail;
        private String                 status;
        private int                    unreadByAdmin;
        private int                    unreadByUser;
        private LocalDateTime          updatedAt;
        private LocalDateTime          createdAt;
        private List<ChatMessageDto>   messages;
    }