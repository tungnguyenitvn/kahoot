package dev.sample.quiz.archive.domain;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/** One entry of a room's event stream as the Redis room contract defines it: {roomId, version, type, at, data}. Built from the parsed envelope; parsing JSON is the adapter's job. */
public record RoomEvent(Map<String, Object> envelope) {
    public static RoomEvent of(Map<String, Object> envelope) {
        if (envelope == null || !(envelope.get("version") instanceof Number) || !(envelope.get("type") instanceof String type)) throw new IllegalArgumentException("event envelope without version or type");
        EventType.valueOf(type);
        return new RoomEvent(Collections.unmodifiableMap(new LinkedHashMap<>(envelope)));
    }
    public long version() { return ((Number) envelope.get("version")).longValue(); }
    public EventType type() { return EventType.valueOf((String) envelope.get("type")); }
    @SuppressWarnings("unchecked")
    public Map<String, Object> data() { return envelope.get("data") instanceof Map<?, ?> data ? (Map<String, Object>) data : Map.of(); }
    public String text(String field) { Object value = data().get(field); return value == null ? null : value.toString(); }
    public long number(String field) { return ((Number) data().get(field)).longValue(); }
}
