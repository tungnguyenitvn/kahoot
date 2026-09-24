package dev.sample.quiz.gameplay.domain;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
/** A room id is deterministic for host identity + create commandId, so a retried create finds its room instead of making a second one. */
public final class RoomId {
    private RoomId() { }
    public static UUID of(String hostIdentity, UUID commandId) {
        return UUID.nameUUIDFromBytes((hostIdentity + ":" + commandId).getBytes(StandardCharsets.UTF_8));
    }
}
