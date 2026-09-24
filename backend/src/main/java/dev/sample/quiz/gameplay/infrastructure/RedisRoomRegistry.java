package dev.sample.quiz.gameplay.infrastructure;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import dev.sample.quiz.gameplay.application.RoomRegistry;
/** Active set and PIN reservations (quiz:pin:<pin> -> room); attempts and TTL are in the limits table. */
@Component
public class RedisRoomRegistry implements RoomRegistry {
    private final StringRedisTemplate redis; private final SecureRandom random = new SecureRandom();
    public RedisRoomRegistry(StringRedisTemplate redis) { this.redis = redis; }
    public long activeRooms() { return Optional.ofNullable(redis.opsForSet().size(LuaRooms.ACTIVE)).orElse(0L); }
    public Set<String> activeRoomIds() { var ids = redis.opsForSet().members(LuaRooms.ACTIVE); return ids == null ? Set.of() : ids; }
    public Optional<String> reservePin(String room) {
        for (int attempt = 0; attempt < 100; attempt++) {
            String pin = String.format("%06d", random.nextInt(1_000_000));
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("quiz:pin:" + pin, room, Duration.ofHours(2)))) return Optional.of(pin);
        }
        return Optional.empty();
    }
    public void releasePin(String pin) { redis.delete("quiz:pin:" + pin); }
    public Optional<String> roomByPin(String pin) { return Optional.ofNullable(redis.opsForValue().get("quiz:pin:" + pin)); }
}
