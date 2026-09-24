package dev.sample.quiz.identity.domain;
import java.io.Serializable;
import java.util.UUID;
/** A persisted host account. Serializable because the security principal that wraps it lives in the HTTP session. */
public record Account(UUID id, String username, String displayName, String passwordHash) implements Serializable {
    /** The hash never reaches a log line. */
    @Override public String toString() { return "Account[" + id + ", " + username + "]"; }
}
