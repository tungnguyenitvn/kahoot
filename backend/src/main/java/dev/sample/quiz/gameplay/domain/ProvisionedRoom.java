package dev.sample.quiz.gameplay.domain;
import java.util.Map;
import java.util.UUID;
/** The SQL side of a room during provisioning: the row gameplay owns until the live room is registered. */
public record ProvisionedRoom(UUID id, UUID quizId, String phase, Map<String, Object> content) {
    public boolean provisioning() { return "PROVISIONING".equals(phase); }
}
