package dev.sample.quiz.gameplay;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoomWebSocketHandshakeConfiguration {
    @Bean
    RoomWebSocketHandshakeInterceptor roomWebSocketHandshakeInterceptor(RedisRooms rooms) {
        return new RoomWebSocketHandshakeInterceptor(rooms);
    }
}
