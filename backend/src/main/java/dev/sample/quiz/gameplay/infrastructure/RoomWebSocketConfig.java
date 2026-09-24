package dev.sample.quiz.gameplay.infrastructure;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class RoomWebSocketConfig implements WebSocketConfigurer {
    private final RoomWebSocketHandler handler;
    private final RoomWebSocketHandshakeInterceptor interceptor;
    private final List<String> origins;

    public RoomWebSocketConfig(RoomWebSocketHandler handler, RoomWebSocketHandshakeInterceptor interceptor,
            @Value("${app.websocket.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}") String allowedOrigins) {
        this.handler = handler;
        this.interceptor = interceptor;
        this.origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/rooms/*")
                .addInterceptors(interceptor)
                .setAllowedOrigins(origins.toArray(String[]::new));
    }
}
