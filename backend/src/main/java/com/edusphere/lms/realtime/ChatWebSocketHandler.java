package com.edusphere.lms.realtime;

import com.edusphere.lms.dto.ChatDtos.ChatPresenceEvent;
import com.edusphere.lms.dto.ChatDtos.ChatSocketEnvelope;
import com.edusphere.lms.entity.User;
import com.edusphere.lms.repository.UserRepository;
import com.edusphere.lms.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final Map<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, Long> userBySessionId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = tokenFromUri(session.getUri());
        if (token == null || token.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing token"));
            return;
        }

        String username;
        try {
            username = jwtService.extractUsername(token);
        } catch (Exception exception) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
            return;
        }

        User user = userRepository.findByEmail(username).orElse(null);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unknown user"));
            return;
        }

        sessionsByUser.computeIfAbsent(user.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(session);
        userBySessionId.put(session.getId(), user.getId());
        broadcastPresence(user.getId(), true);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userBySessionId.remove(session.getId());
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
            broadcastPresence(userId, false);
        }
    }

    public void sendToUser(Long userId, Object payload) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(payload);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(body));
                }
            }
        } catch (IOException ignored) {
        }
    }

    public Set<Long> onlineUserIds() {
        return Set.copyOf(sessionsByUser.keySet());
    }

    private void broadcastPresence(Long userId, boolean online) {
        ChatSocketEnvelope envelope = new ChatSocketEnvelope(
                "PRESENCE",
                null,
                new ChatPresenceEvent("PRESENCE", userId, online)
        );
        for (Long connectedUserId : sessionsByUser.keySet()) {
            sendToUser(connectedUserId, envelope);
        }
    }

    private String tokenFromUri(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String part : uri.getQuery().split("&")) {
            String[] pieces = part.split("=", 2);
            if (pieces.length == 2 && "token".equals(pieces[0])) {
                return URLDecoder.decode(pieces[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
