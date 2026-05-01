package com.IngSoftwarelll.mercadox.dtos.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
    public class SendMessageRequest {
        private String content;
    }