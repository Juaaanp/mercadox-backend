package com.IngSoftwarelll.mercadox.controllers;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.IngSoftwarelll.mercadox.dtos.chat.ChatMessageDto;
import com.IngSoftwarelll.mercadox.dtos.chat.ConversationDetailDto;
import com.IngSoftwarelll.mercadox.dtos.chat.ConversationSummaryDto;
import com.IngSoftwarelll.mercadox.dtos.chat.SendMessageRequest;
import com.IngSoftwarelll.mercadox.security.CustomUserDetails;
import com.IngSoftwarelll.mercadox.services.ChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
 
    private final ChatService chatService;
 
    // ────────────────────────────────────────────────────────────────────────
    // USER endpoints
    // ────────────────────────────────────────────────────────────────────────
 
    /** Get (or create) the current user's conversation */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ConversationDetailDto> getMyConversation(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(
            chatService.getOrCreateConversation(principal.getId()));
    }
 
    /** User sends a message */
    @PostMapping("/my/messages")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ChatMessageDto> userSendMessage(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(
            chatService.userSendMessage(principal.getId(), req.getContent()));
    }
 
    /** User marks messages as read */
    @PostMapping("/my/read")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<Void> userMarkAsRead(
            @AuthenticationPrincipal CustomUserDetails principal) {
        chatService.userMarkAsRead(principal.getId());
        return ResponseEntity.ok().build();
    }
 
    /** Get unread count for the user */
    @GetMapping("/my/unread")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<Integer> getUserUnread(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(
            chatService.getUserUnreadCount(principal.getId()));
    }
 
    // ────────────────────────────────────────────────────────────────────────
    // ADMIN endpoints
    // ────────────────────────────────────────────────────────────────────────
 
    /** List all conversations */
    @GetMapping("/admin/conversations")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<ConversationSummaryDto>> getAllConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.getAllConversations(page, size));
    }
 
    /** Get a specific conversation with all messages */
    @GetMapping("/admin/conversations/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ConversationDetailDto> getConversation(
            @PathVariable Long id) {
        return ResponseEntity.ok(chatService.getConversationDetail(id));
    }
 
    /** Admin sends a message */
    @PostMapping("/admin/conversations/{id}/messages")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ChatMessageDto> adminSendMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(
            chatService.adminSendMessage(id, principal.getUsername(), req.getContent()));
    }
 
    /** Admin marks conversation as read */
    @PostMapping("/admin/conversations/{id}/read")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> adminMarkAsRead(@PathVariable Long id) {
        chatService.adminMarkAsRead(id);
        return ResponseEntity.ok().build();
    }
}