package com.edusphere.lms.service;

import com.edusphere.lms.dto.ChatDtos.ChatBootstrapResponse;
import com.edusphere.lms.dto.ChatDtos.ChatContactResponse;
import com.edusphere.lms.dto.ChatDtos.ChatMessageResponse;
import com.edusphere.lms.dto.ChatDtos.ChatSendRequest;
import com.edusphere.lms.dto.ChatDtos.ChatSocketEnvelope;
import com.edusphere.lms.entity.ChatMessage;
import com.edusphere.lms.entity.NotificationType;
import com.edusphere.lms.entity.Role;
import com.edusphere.lms.entity.User;
import com.edusphere.lms.exception.ApiException;
import com.edusphere.lms.realtime.ChatWebSocketHandler;
import com.edusphere.lms.repository.ChatMessageRepository;
import com.edusphere.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public ChatBootstrapResponse bootstrap(User currentUser) {
        List<ChatContactResponse> contacts = allowedContacts(currentUser).stream()
                .map(contact -> new ChatContactResponse(
                        contact.getId(),
                        contact.getName(),
                        contact.getEmail(),
                        contact.getRole(),
                        chatWebSocketHandler.onlineUserIds().contains(contact.getId())
                ))
                .sorted(Comparator.comparing(ChatContactResponse::online).reversed()
                        .thenComparing(ChatContactResponse::role)
                        .thenComparing(ChatContactResponse::name))
                .toList();

        return new ChatBootstrapResponse(contacts, List.copyOf(chatWebSocketHandler.onlineUserIds()));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> conversation(Long otherUserId, User currentUser) {
        User otherUser = requireAllowedContact(currentUser, otherUserId);
        return chatMessageRepository.findConversation(currentUser.getId(), otherUser.getId()).stream()
                .map(message -> toResponse(message, currentUser))
                .toList();
    }

    @Transactional
    public ChatMessageResponse send(ChatSendRequest request, User currentUser) {
        User recipient = requireAllowedContact(currentUser, request.recipientId());
        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .sender(currentUser)
                .recipient(recipient)
                .content(request.content().trim())
                .build());

        ChatMessageResponse senderView = toResponse(saved, currentUser);
        ChatMessageResponse recipientView = toResponse(saved, recipient);

        chatWebSocketHandler.sendToUser(currentUser.getId(), new ChatSocketEnvelope("MESSAGE", senderView, null));
        chatWebSocketHandler.sendToUser(recipient.getId(), new ChatSocketEnvelope("MESSAGE", recipientView, null));

        notificationService.notifyUser(
                recipient,
                "New chat message",
                currentUser.getName() + " sent you a message.",
                NotificationType.QUIZ,
                currentUser.getRole() == Role.INSTRUCTOR ? "/instructor" : currentUser.getRole() == Role.ADMIN ? "/admin" : "/student"
        );
        return senderView;
    }

    private User requireAllowedContact(User currentUser, Long otherUserId) {
        return allowedContacts(currentUser).stream()
                .filter(user -> user.getId().equals(otherUserId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You cannot chat with this user."));
    }

    private List<User> allowedContacts(User currentUser) {
        List<User> allUsers = userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .toList();

        return switch (currentUser.getRole()) {
            case ADMIN -> allUsers;
            case INSTRUCTOR -> allUsers.stream()
                    .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.STUDENT)
                    .toList();
            case STUDENT -> allUsers.stream()
                    .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.INSTRUCTOR)
                    .toList();
        };
    }

    private ChatMessageResponse toResponse(ChatMessage message, User currentUser) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getRecipient().getId(),
                message.getRecipient().getName(),
                message.getContent(),
                message.getCreatedAt(),
                message.getSender().getId().equals(currentUser.getId())
        );
    }
}
