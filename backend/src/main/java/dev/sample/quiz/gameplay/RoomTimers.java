package dev.sample.quiz.gameplay;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
@Component
public class RoomTimers {
    private final RedisRooms rooms;private final StringRedisTemplate redis;private final RoomWebSocketHub sockets;
    private static final Logger log=LoggerFactory.getLogger(RoomTimers.class);
    public RoomTimers(RedisRooms rooms,StringRedisTemplate redis,RoomWebSocketHub sockets){this.rooms=rooms;this.redis=redis;this.sockets=sockets;}
    @Scheduled(fixedDelay=250) public void tick(){
        try{Set<String> ids=redis.opsForSet().members(RedisRooms.ACTIVE);if(ids==null)return;
            for(String id:ids)try{long before=Optional.ofNullable(rooms.meta(id)).map(m->m.path("version").asLong()).orElse(-1L);rooms.command(id,"tick","",Map.of());long after=Optional.ofNullable(rooms.meta(id)).map(m->m.path("version").asLong()).orElse(before);if(after!=before)sockets.broadcast(id);}catch(Exception e){log.warn("Room timer unavailable: {}",id);}
        }catch(Exception e){log.warn("Redis timer unavailable");}
    }
}
