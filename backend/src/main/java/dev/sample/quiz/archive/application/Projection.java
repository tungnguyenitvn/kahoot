package dev.sample.quiz.archive.application;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.sample.quiz.archive.domain.RoomEvent;
/** Applies one event to the PostgreSQL projection: steps 2 to 4 of the processing order, one transaction per event. */
@Service
public class Projection {
    private final ArchiveRepository archive;
    public Projection(ArchiveRepository archive) { this.archive = archive; }
    /** Returns false for a replay (version already archived); throws on a sequence gap so the room is retried later. */
    @Transactional
    public boolean apply(String room, String streamId, RoomEvent event) {
        UUID id = UUID.fromString(room); long version = event.version();
        Optional<Long> current = archive.lockArchivedVersion(id);
        if (current.isPresent() && version <= current.get()) return false;
        if (current.isEmpty() || version != current.get() + 1) throw new IllegalStateException("Archive sequence gap for " + room);
        archive.recordEvent(id, version, streamId, event.type(), event.envelope());
        switch (event.type()) {
            case JOINED -> archive.addParticipant(id, UUID.fromString(event.text("id")), event.text("name"), event.text("principal"));
            case KICKED -> archive.deactivateParticipant(id, UUID.fromString(event.text("id")));
            case ANSWERED -> archive.recordAnswer(id, UUID.fromString(event.text("roundId")), UUID.fromString(event.text("participantId")),
                (int) event.number("option"), (int) event.number("correctOrder"), (int) event.number("points"), event.number("acceptedAt"));
            default -> { }
        }
        archive.advance(id, version, event.type().phase().orElse(null));
        return true;
    }
}
