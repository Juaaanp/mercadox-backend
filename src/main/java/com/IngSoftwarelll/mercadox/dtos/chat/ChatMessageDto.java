package com.IngSoftwarelll.mercadox.dtos.chat;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data @Builder
    public class ChatMessageDto {
        private Long           id;
        private String         content;
        private String         senderRole;
        private String         senderName;
        private boolean        readByOther;
        private LocalDateTime  createdAt;
    }