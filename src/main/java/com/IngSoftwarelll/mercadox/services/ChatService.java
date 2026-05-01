package com.IngSoftwarelll.mercadox.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.IngSoftwarelll.mercadox.dtos.chat.ChatMessageDto;
import com.IngSoftwarelll.mercadox.dtos.chat.ConversationDetailDto;
import com.IngSoftwarelll.mercadox.dtos.chat.ConversationSummaryDto;
import com.IngSoftwarelll.mercadox.exceptions.ResourceNotFoundException;
import com.IngSoftwarelll.mercadox.mappers.ChatMapper;
import com.IngSoftwarelll.mercadox.models.ChatConversation;
import com.IngSoftwarelll.mercadox.models.ChatMessage;
import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.repositories.ChatConversationRepository;
import com.IngSoftwarelll.mercadox.repositories.ChatMessageRepository;
import com.IngSoftwarelll.mercadox.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {
 
    private final ChatConversationRepository conversationRepo;
    private final ChatMessageRepository      messageRepo;
    private final UserRepository             userRepo;
    private final ChatMapper                 mapper;
    private final SimpMessagingTemplate      messagingTemplate;
 
    // ── Get or create conversation for a user ─────────────────────────────
 
    @Transactional
    public ConversationDetailDto getOrCreateConversation(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
 
        ChatConversation conv = conversationRepo.findByUserId(userId)
            .orElseGet(() -> {
                ChatConversation c = ChatConversation.builder()
                    .user(user)
                    .status(ChatConversation.ConversationStatus.OPEN)
                    .unreadByAdmin(0)
                    .unreadByUser(0)
                    .build();
                return conversationRepo.save(c);
            });
 
        List<ChatMessage> messages = messageRepo
            .findByConversationIdOrderByCreatedAtAsc(conv.getId());
 
        return mapper.toDetailDto(conv, messages);
    }
 
    // ── User sends a message ───────────────────────────────────────────────
 
    @Transactional
    public ChatMessageDto userSendMessage(Long userId, String content) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
 
        ChatConversation conv = conversationRepo.findByUserId(userId)
            .orElseGet(() -> {
                ChatConversation c = ChatConversation.builder()
                    .user(user)
                    .status(ChatConversation.ConversationStatus.OPEN)
                    .unreadByAdmin(0)
                    .unreadByUser(0)
                    .build();
                return conversationRepo.save(c);
            });
 
        ChatMessage msg = ChatMessage.builder()
            .conversation(conv)
            .content(content)
            .senderRole(ChatMessage.SenderRole.USER)
            .senderName(user.getEmail())
            .readByOther(false)
            .build();
 
        msg = messageRepo.save(msg);
 
        conv.setUnreadByAdmin(conv.getUnreadByAdmin() + 1);
        conv.setStatus(ChatConversation.ConversationStatus.OPEN);
        conversationRepo.save(conv);
 
        ChatMessageDto dto = mapper.toMessageDto(msg);
 
        // Push to admin WebSocket topic
        messagingTemplate.convertAndSend(
            "/topic/admin/chat/" + conv.getId(), dto);
 
        // Push unread count update to admin
        messagingTemplate.convertAndSend(
            "/topic/admin/chat-updates", buildSummary(conv, msg));
 
        return dto;
    }
 
    // ── Admin sends a message ──────────────────────────────────────────────
 
    @Transactional
    public ChatMessageDto adminSendMessage(Long conversationId, String adminName, String content) {
        ChatConversation conv = conversationRepo.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
 
        ChatMessage msg = ChatMessage.builder()
            .conversation(conv)
            .content(content)
            .senderRole(ChatMessage.SenderRole.ADMIN)
            .senderName(adminName)
            .readByOther(false)
            .build();
 
        msg = messageRepo.save(msg);
 
        conv.setUnreadByUser(conv.getUnreadByUser() + 1);
        conversationRepo.save(conv);
 
        ChatMessageDto dto = mapper.toMessageDto(msg);
 
        // Push to user WebSocket topic
        messagingTemplate.convertAndSend(
            "/topic/user/chat/" + conv.getUser().getId(), dto);
 
        return dto;
    }
 
    // ── Admin marks conversation as read ──────────────────────────────────
 
    @Transactional
    public void adminMarkAsRead(Long conversationId) {
        ChatConversation conv = conversationRepo.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
 
        messageRepo.markAllAsRead(conversationId, ChatMessage.SenderRole.USER);
        conv.setUnreadByAdmin(0);
        conversationRepo.save(conv);
    }
 
    // ── User marks conversation as read ───────────────────────────────────
 
    @Transactional
    public void userMarkAsRead(Long userId) {
        conversationRepo.findByUserId(userId).ifPresent(conv -> {
            messageRepo.markAllAsRead(conv.getId(), ChatMessage.SenderRole.ADMIN);
            conv.setUnreadByUser(0);
            conversationRepo.save(conv);
        });
    }
 
    // ── Admin: list all conversations ─────────────────────────────────────
 
    @Transactional(readOnly = true)
    public Page<ConversationSummaryDto> getAllConversations(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatConversation> convPage = conversationRepo.findAllOrderByUpdatedAtDesc(pageable);
 
        return convPage.map(conv -> {
            List<ChatMessage> msgs = messageRepo.findByConversationIdOrderByCreatedAtAsc(conv.getId());
            ChatMessage last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            return mapper.toSummaryDto(conv, last);
        });
    }
 
    // ── Admin: get conversation detail ────────────────────────────────────
 
    @Transactional(readOnly = true)
    public ConversationDetailDto getConversationDetail(Long conversationId) {
        ChatConversation conv = conversationRepo.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
 
        List<ChatMessage> messages = messageRepo
            .findByConversationIdOrderByCreatedAtAsc(conversationId);
 
        return mapper.toDetailDto(conv, messages);
    }
 
    // ── User: get unread count ────────────────────────────────────────────
 
    @Transactional(readOnly = true)
    public int getUserUnreadCount(Long userId) {
        return conversationRepo.findByUserId(userId)
            .map(ChatConversation::getUnreadByUser)
            .orElse(0);
    }
 
    // ── Helper ────────────────────────────────────────────────────────────
 
    private ConversationSummaryDto buildSummary(ChatConversation conv, ChatMessage lastMsg) {
        return mapper.toSummaryDto(conv, lastMsg);
    }
}