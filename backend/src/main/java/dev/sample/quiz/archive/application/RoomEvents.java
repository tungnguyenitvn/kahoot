package dev.sample.quiz.archive.application;
import java.util.List;
import java.util.Set;
import dev.sample.quiz.archive.domain.RoomEvent;
/** Port to the room event streams. The adapter owns the key names and the consumer group; this layer never sees a Redis key. */
public interface RoomEvents {
    record Pending(String entryId, RoomEvent event) {}
    Set<String> activeRooms();
    /** This consumer's unacknowledged entries first, then new ones; empty when the room has no stream. */
    List<Pending> read(String room);
    void ack(String room, String entryId);
    /** Acknowledges the terminal event, starts the retention TTL and leaves the active set in one operation. */
    void finish(String room, String entryId);
}
