package com.IngSoftwarelll.mercadox.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.IngSoftwarelll.mercadox.dtos.chat.ChatMessageDto;
import com.IngSoftwarelll.mercadox.dtos.chat.ConversationDetailDto;
import com.IngSoftwarelll.mercadox.dtos.chat.ConversationSummaryDto;
import com.IngSoftwarelll.mercadox.models.ChatConversation;
import com.IngSoftwarelll.mercadox.models.ChatMessage;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "senderRole", expression = "java(msg.getSenderRole().name())")
    ChatMessageDto toMessageDto(ChatMessage msg);

    @Mapping(target = "id", source = "conv.id")
    @Mapping(target = "createdAt", source = "conv.createdAt")
    @Mapping(target = "userId", source = "conv.user.id")
    @Mapping(target = "userEmail", source = "conv.user.email")
    @Mapping(target = "status", expression = "java(conv.getStatus().name())")
    @Mapping(target = "lastMessage", source = "lastMsg")
    ConversationSummaryDto toSummaryDto(ChatConversation conv, ChatMessage lastMsg);

    @Mapping(target = "userId", source = "conv.user.id")
    @Mapping(target = "userEmail", source = "conv.user.email")
    @Mapping(target = "status", expression = "java(conv.getStatus().name())")
    @Mapping(target = "messages", source = "messages")
    ConversationDetailDto toDetailDto(ChatConversation conv, List<ChatMessage> messages);

    List<ChatMessageDto> toMessageDtoList(List<ChatMessage> messages);
}