package dev.sample.quiz.archive.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.sample.quiz.archive.domain.EventType;
import dev.sample.quiz.archive.domain.RoomEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectionTest {
    /** In-memory projection: records what the real adapter would write. */
    static final class MemoryArchive implements ArchiveRepository {
        final Map<UUID, Long> version = new HashMap<>(); final Map<UUID, String> phase = new HashMap<>();
        final List<String> writes = new ArrayList<>();
        public Optional<Long> lockArchivedVersion(UUID room) { return Optional.ofNullable(version.get(room)); }
        public void recordEvent(UUID room, long v, String streamId, EventType type, Map<String, Object> envelope) { writes.add("event " + v + " " + type + " " + streamId); }
        public void addParticipant(UUID room, UUID participant, String name, String principal) { writes.add("participant " + name); }
        public void deactivateParticipant(UUID room, UUID participant) { writes.add("kicked " + participant); }
        public void recordAnswer(UUID room, UUID round, UUID participant, int option, int correctOrder, int points, long acceptedAt) { writes.add("answer " + points); }
        public void advance(UUID room, long v, String p) { version.put(room, v); if (p != null) phase.put(room, p); }
    }
    static RoomEvent event(long version, String type, Map<String, Object> data) {
        return RoomEvent.of(Map.of("roomId", "r", "version", version, "type", type, "at", 1L, "data", data));
    }

    @Test
    @DisplayName("LIVE-06 events apply in order once: a replay is a no-op, a gap is rejected, the next version projects")
    void replayIsANoOpAndGapsAreRejected() {
        var archive = new MemoryArchive(); var projection = new Projection(archive);
        UUID room = UUID.randomUUID(); archive.version.put(room, 0L);
        UUID participant = UUID.randomUUID();

        assertTrue(projection.apply(room.toString(), "1-0", event(1, "CREATED", Map.of("quizId", "q", "owner", "U:x"))));
        assertEquals("LOBBY", archive.phase.get(room));
        assertFalse(projection.apply(room.toString(), "1-0", event(1, "CREATED", Map.of())), "replay of an archived version");
        assertEquals(List.of("event 1 CREATED 1-0"), archive.writes);

        assertThrows(IllegalStateException.class, () -> projection.apply(room.toString(), "3-0", event(3, "OPENED", Map.of())), "sequence gap");
        assertEquals(1L, archive.version.get(room));

        assertTrue(projection.apply(room.toString(), "2-0", event(2, "JOINED", Map.of("id", participant.toString(), "name", "ann", "principal", "G:1", "active", true))));
        assertTrue(projection.apply(room.toString(), "3-0", event(3, "ANSWERED", Map.of("roundId", UUID.randomUUID().toString(), "participantId", participant.toString(), "option", 1, "correctOrder", 1, "points", 1000, "acceptedAt", 5L))));
        assertTrue(projection.apply(room.toString(), "4-0", event(4, "FINISHED", Map.of("reason", "COMPLETED"))));
        assertEquals(List.of("event 1 CREATED 1-0", "event 2 JOINED 2-0", "participant ann", "event 3 ANSWERED 3-0", "answer 1000", "event 4 FINISHED 4-0"), archive.writes);
        assertEquals("FINISHED", archive.phase.get(room)); assertEquals(4L, archive.version.get(room));
        assertThrows(IllegalStateException.class, () -> projection.apply(UUID.randomUUID().toString(), "1-0", event(1, "CREATED", Map.of())), "unknown room");
    }
}
