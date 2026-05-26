package com.edusphere.lms.config;

import com.edusphere.lms.realtime.ChatWebSocketHandler;
import com.edusphere.lms.realtime.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .setAllowedOrigins(frontendUrl, "http://localhost:5173");
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins(frontendUrl, "http://localhost:5173");
    }
}
