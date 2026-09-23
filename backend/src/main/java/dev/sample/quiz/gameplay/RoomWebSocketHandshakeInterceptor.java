package dev.sample.quiz.gameplay;

import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import dev.sample.quiz.identity.Identity;
import dev.sample.quiz.shared.ApiException;

public class RoomWebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private final RedisRooms rooms;

    public RoomWebSocketHandshakeInterceptor(RedisRooms rooms) {
        this.rooms = rooms;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servlet)) return false;
        HttpServletRequest http = servlet.getServletRequest();
        String[] parts = http.getRequestURI().split("/");
        if (parts.length == 0) return false;
        UUID room;
        try {
            room = UUID.fromString(parts[parts.length - 1]);
        } catch (IllegalArgumentException ex) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        try {
            Identity identity = Identity.current(http);
            var session = http.getSession(false);
            if (session == null) { response.setStatusCode(HttpStatus.UNAUTHORIZED); return false; }
            rooms.command(room.toString(), "snapshot", identity.id(), Map.of());
            attributes.put("roomId", room.toString());
            attributes.put("principalId", identity.id());
            attributes.put("httpSessionId", session.getId());
            return true;
        } catch (ApiException ex) {
            response.setStatusCode(HttpStatus.valueOf(ex.status));
            return false;
        } catch (RuntimeException ex) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
    }
}
