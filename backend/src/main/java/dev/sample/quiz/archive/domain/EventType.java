package dev.sample.quiz.archive.domain;
import java.util.Optional;
/** Event types of the Redis room contract, with the archived phase each one moves the room to (docs/domain.md state transitions). */
public enum EventType {
    CREATED("LOBBY"), JOINED(null), OPENED("QUESTION"), ANSWERED(null), REVEALED("REVEAL"), KICKED(null), FINISHED("FINISHED");
    private final String phase;
    EventType(String phase) { this.phase = phase; }
    /** Empty when the event leaves the phase unchanged. */
    public Optional<String> phase() { return Optional.ofNullable(phase); }
    public boolean terminal() { return this == FINISHED; }
}
