package dev.sample.quiz.gameplay.infrastructure;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import dev.sample.quiz.gameplay.application.Notifier;
import dev.sample.quiz.gameplay.application.RoomCommands;
import dev.sample.quiz.gameplay.application.RoomRegistry;
@Component
public class RoomTimers {
    private final RoomCommands rooms;private final RoomRegistry registry;private final Notifier notifier;
    private static final Logger log=LoggerFactory.getLogger(RoomTimers.class);
    public RoomTimers(RoomCommands rooms,RoomRegistry registry,Notifier notifier){this.rooms=rooms;this.registry=registry;this.notifier=notifier;}
    @Scheduled(fixedDelay=250) public void tick(){
        Set<String> ids;
        try{ids=registry.activeRoomIds();}catch(Exception e){log.warn("Redis timer unavailable");return;}
        for(String id:ids)try{long before=Optional.ofNullable(rooms.meta(id)).map(m->m.path("version").asLong()).orElse(-1L);rooms.command(id,"tick","",Map.of());long after=Optional.ofNullable(rooms.meta(id)).map(m->m.path("version").asLong()).orElse(before);if(after!=before)notifier.broadcast(id);}catch(Exception e){log.warn("Room timer unavailable: {}",id);}
    }
}
