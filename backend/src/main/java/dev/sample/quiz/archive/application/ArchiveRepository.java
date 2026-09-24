package dev.sample.quiz.archive.application;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.sample.quiz.archive.domain.EventType;
/** Port to the projection tables; archive is their only writer. Every call runs inside the transaction Projection.apply opens. */
public interface ArchiveRepository {
    /** Locks the room row for the transaction; empty when the room is unknown. */
    Optional<Long> lockArchivedVersion(UUID room);
    void recordEvent(UUID room, long version, String streamId, EventType type, Map<String, Object> envelope);
    void addParticipant(UUID room, UUID participant, String name, String principal);
    void deactivateParticipant(UUID room, UUID participant);
    void recordAnswer(UUID room, UUID round, UUID participant, int option, int correctOrder, int points, long acceptedAt);
    /** Advances archived_version; a non-null phase also moves the room there and stamps finished_at on FINISHED. */
    void advance(UUID room, long version, String phase);
}
