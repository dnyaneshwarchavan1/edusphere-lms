package com.edusphere.lms.controller;

import com.edusphere.lms.dto.ChatDtos.ChatBootstrapResponse;
import com.edusphere.lms.dto.ChatDtos.ChatMessageResponse;
import com.edusphere.lms.dto.ChatDtos.ChatSendRequest;
import com.edusphere.lms.entity.User;
import com.edusphere.lms.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/bootstrap")
    public ChatBootstrapResponse bootstrap(@AuthenticationPrincipal User currentUser) {
        return chatService.bootstrap(currentUser);
    }

    @GetMapping("/messages/{otherUserId}")
    public List<ChatMessageResponse> conversation(@PathVariable Long otherUserId, @AuthenticationPrincipal User currentUser) {
        return chatService.conversation(otherUserId, currentUser);
    }

    @PostMapping("/messages")
    public ChatMessageResponse send(@Valid @RequestBody ChatSendRequest request, @AuthenticationPrincipal User currentUser) {
        return chatService.send(request, currentUser);
    }
}
