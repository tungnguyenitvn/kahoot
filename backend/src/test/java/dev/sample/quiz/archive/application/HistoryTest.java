package dev.sample.quiz.archive.application;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.sample.quiz.archive.domain.HistoryResult;
import dev.sample.quiz.archive.domain.HistoryRoom;
import dev.sample.quiz.shared.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HistoryTest {
    /** In-memory read port: rooms keyed by owner, results keyed by room. */
    static final class MemoryHistory implements HistoryRepository {
        final Map<UUID, Map<UUID, HistoryRoom>> byOwner = new HashMap<>(); final Map<UUID, List<HistoryResult>> resultsByRoom = new HashMap<>();
        HistoryRoom room(UUID owner, String phase) {
            var room = new HistoryRoom(UUID.randomUUID(), "Quiz", phase, 0, OffsetDateTime.now(), null);
            byOwner.computeIfAbsent(owner, o -> new HashMap<>()).put(room.id(), room); return room;
        }
        public List<HistoryRoom> rooms(UUID owner) { return List.copyOf(byOwner.getOrDefault(owner, Map.of()).values()); }
        public Optional<String> phase(UUID owner, UUID room) { return Optional.ofNullable(byOwner.getOrDefault(owner, Map.of()).get(room)).map(HistoryRoom::phase); }
        public List<HistoryResult> results(UUID room) { return resultsByRoom.getOrDefault(room, List.of()); }
    }

    @Test
    @DisplayName("HIST-01 a host sees only their own rooms; another host's room is ROOM_NOT_FOUND, not a leak")
    void resultsAreScopedToTheOwner() {
        var repo = new MemoryHistory(); var history = new History(repo);
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        var finished = repo.room(alice, "FINISHED"); repo.resultsByRoom.put(finished.id(), List.of(new HistoryResult(UUID.randomUUID(), "ann", 1000)));

        assertEquals(List.of(finished), history.rooms(alice));
        assertTrue(history.rooms(bob).isEmpty());
        var denied = assertThrows(ApiException.class, () -> history.result(bob, finished.id()));
        assertEquals(404, denied.status); assertEquals("ROOM_NOT_FOUND", denied.code);
        assertEquals(1000, history.result(alice, finished.id()).get(0).score());
    }

    @Test
    @DisplayName("HIST-02 a room that is not FINISHED has no archived result yet: ARCHIVE_NOT_READY, never an empty list")
    void unfinishedRoomIsNotReady() {
        var repo = new MemoryHistory(); var history = new History(repo);
        UUID alice = UUID.randomUUID(); var live = repo.room(alice, "QUESTION");
        var notReady = assertThrows(ApiException.class, () -> history.result(alice, live.id()));
        assertEquals(409, notReady.status); assertEquals("ARCHIVE_NOT_READY", notReady.code);
    }
}
