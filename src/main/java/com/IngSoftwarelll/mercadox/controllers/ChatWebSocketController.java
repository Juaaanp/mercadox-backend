package com.IngSoftwarelll.mercadox.controllers;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.IngSoftwarelll.mercadox.dtos.chat.SendMessageRequest;
import com.IngSoftwarelll.mercadox.services.ChatService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
 
    private final ChatService chatService;
 
    /** User sends via WebSocket */
    @MessageMapping("/chat.user.send")
    public void userSend(
            @Payload SendMessageRequest req,
            @Header("userId") Long userId) {
        chatService.userSendMessage(userId, req.getContent());
    }
 
    /** Admin sends via WebSocket */
    @MessageMapping("/chat.admin.send/{conversationId}")
    public void adminSend(
            @DestinationVariable Long conversationId,
            @Payload SendMessageRequest req,
            @Header("adminName") String adminName) {
        chatService.adminSendMessage(conversationId, adminName, req.getContent());
    }
}