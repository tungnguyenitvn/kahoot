package dev.sample.quiz.gameplay;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.sample.quiz.gameplay.application.RoomCommands;
import dev.sample.quiz.identity.application.Identities;

@Configuration
public class RoomWebSocketHandshakeConfiguration {
    @Bean
    RoomWebSocketHandshakeInterceptor roomWebSocketHandshakeInterceptor(RoomCommands rooms, Identities identities) {
        return new RoomWebSocketHandshakeInterceptor(rooms, identities);
    }
}
