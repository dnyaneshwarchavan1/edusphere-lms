package com.edusphere.lms.dto;

import com.edusphere.lms.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class ChatDtos {
    public record ChatContactResponse(
            Long userId,
            String name,
            String email,
            Role role,
            boolean online
    ) {}

    public record ChatMessageResponse(
            Long id,
            Long senderId,
            String senderName,
            Long recipientId,
            String recipientName,
            String content,
            Instant createdAt,
            boolean ownMessage
    ) {}

    public record ChatSendRequest(
            @NotNull Long recipientId,
            @NotBlank String content
    ) {}

    public record ChatPresenceEvent(
            String type,
            Long userId,
            boolean online
    ) {}

    public record ChatSocketEnvelope(
            String type,
            ChatMessageResponse message,
            ChatPresenceEvent presence
    ) {}

    public record ChatBootstrapResponse(
            List<ChatContactResponse> contacts,
            List<Long> onlineUserIds
    ) {}
}
