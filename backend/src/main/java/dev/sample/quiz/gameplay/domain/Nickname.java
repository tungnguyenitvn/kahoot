package dev.sample.quiz.gameplay.domain;
import java.util.Locale;
import java.util.Optional;
/** A participant's display name: trimmed, non-empty, bounded; unique per room case-insensitively (docs/domain.md). Not a credential. */
public record Nickname(String value) {
    public static final int MAX_LENGTH = 24;
    public Nickname {
        if (value == null) throw new IllegalArgumentException("nickname missing");
        value = value.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) throw new IllegalArgumentException("invalid nickname");
    }
    public static Optional<Nickname> parse(String raw) {
        try { return Optional.of(new Nickname(raw)); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
    /** The key the room uses for uniqueness. */
    public String normalized() { return value.toLowerCase(Locale.ROOT); }
}
