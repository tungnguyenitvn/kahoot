package dev.sample.quiz.gameplay;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.sample.quiz.identity.application.Identities;

@Configuration
public class RoomWebSocketHandshakeConfiguration {
    @Bean
    RoomWebSocketHandshakeInterceptor roomWebSocketHandshakeInterceptor(RedisRooms rooms, Identities identities) {
        return new RoomWebSocketHandshakeInterceptor(rooms, identities);
    }
}
