package dev.sample.quiz.archive.infrastructure;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import dev.sample.quiz.archive.application.RoomEvents;
import dev.sample.quiz.archive.domain.RoomEvent;
/** Redis Streams adapter. Key names follow the Redis room contract (docs/contracts/redis-room.md); gameplay builds the same names from the same page, nothing is imported. */
@Component
public class RedisRoomEvents implements RoomEvents {
    static final String GROUP = "postgres", CONSUMER = "single-instance", ACTIVE = "quiz:active-rooms";
    private static final int BATCH = 100;
    private final StringRedisTemplate redis; private final ObjectMapper json;
    public RedisRoomEvents(StringRedisTemplate redis, ObjectMapper json) { this.redis = redis; this.json = json; }
    static String stream(String room) { return "game:{" + room + "}:events"; }
    static List<String> roomKeys(String room) {
        String prefix = "game:{" + room + "}:";
        return List.of(prefix + "meta", prefix + "members", prefix + "names", prefix + "answers", prefix + "scores", prefix + "visible", prefix + "commands", prefix + "events");
    }
    public Set<String> activeRooms() { var rooms = redis.opsForSet().members(ACTIVE); return rooms == null ? Set.of() : rooms; }
    @SuppressWarnings("unchecked")
    public List<Pending> read(String room) {
        String stream = stream(room);
        if (!Boolean.TRUE.equals(redis.hasKey(stream))) return List.of();
        try { redis.opsForStream().createGroup(stream, ReadOffset.from("0-0"), GROUP); } catch (Exception e) {
            if (!String.valueOf(e.getMessage()).contains("BUSYGROUP") && !String.valueOf(e.getCause()).contains("BUSYGROUP")) throw e;
        }
        var consumer = Consumer.from(GROUP, CONSUMER); var options = StreamReadOptions.empty().count(BATCH);
        var entries = redis.opsForStream().read(consumer, options, StreamOffset.create(stream, ReadOffset.from("0-0")));
        if (entries == null || entries.isEmpty()) entries = redis.opsForStream().read(consumer, options, StreamOffset.create(stream, ReadOffset.lastConsumed()));
        if (entries == null) return List.of();
        List<Pending> pending = new ArrayList<>();
        for (var entry : entries) pending.add(new Pending(entry.getId().getValue(), RoomEvent.of(json.readValue(entry.getValue().get("event").toString(), Map.class))));
        return pending;
    }
    public void ack(String room, String entryId) { redis.opsForStream().acknowledge(stream(room), GROUP, entryId); }
    public void finish(String room, String entryId) {
        // One operation: a process crash cannot leave the ACK done but the room still active or its keys without a TTL.
        var keys = new ArrayList<String>(); keys.add(stream(room)); keys.add(ACTIVE); keys.addAll(roomKeys(room));
        redis.execute(new DefaultRedisScript<Long>(
            "for i=3,#KEYS do redis.call('EXPIRE',KEYS[i],86400) end; " +
            "redis.call('XACK',KEYS[1],ARGV[1],ARGV[2]); return redis.call('SREM',KEYS[2],ARGV[3])", Long.class), keys, GROUP, entryId, room);
    }
}
