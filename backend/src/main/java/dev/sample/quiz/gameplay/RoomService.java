package dev.sample.quiz.gameplay;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import dev.sample.quiz.identity.domain.Identity;
import dev.sample.quiz.catalog.application.QuizCatalog;
import dev.sample.quiz.catalog.domain.QuizStatus;
import dev.sample.quiz.shared.ApiException;
@Service
public class RoomService {
    private final RedisRooms rooms;private final StringRedisTemplate redis;private final JdbcTemplate db;
    private final QuizCatalog catalog;private final ObjectMapper json;private final RoomWebSocketHub sockets;private final SecureRandom random=new SecureRandom();
    public RoomService(RedisRooms rooms,StringRedisTemplate redis,JdbcTemplate db,QuizCatalog catalog,ObjectMapper json,RoomWebSocketHub sockets){this.rooms=rooms;this.redis=redis;this.db=db;this.catalog=catalog;this.json=json;this.sockets=sockets;}
    public JsonNode create(Identity who,UUID quizId,UUID commandId){
        UUID owner=who.userId();
        UUID id=UUID.nameUUIDFromBytes((who.id()+":"+commandId).getBytes(StandardCharsets.UTF_8));
        var existing=db.queryForList("select * from game_room where id=?",id);
        if(existing.isEmpty()){
            if(Optional.ofNullable(redis.opsForSet().size(RedisRooms.ACTIVE)).orElse(0L)>=100)throw new ApiException(503,"ROOM_LIMIT");
            var quiz=catalog.get(owner,quizId);
            if(quiz.status()!=QuizStatus.PUBLISHED)throw new ApiException(409,"QUIZ_NOT_PUBLISHED");
            String pin=reservePin(id.toString());
            List<Map<String,Object>> questions=quiz.questions().stream().map(q->{
                Map<String,Object> m=new LinkedHashMap<>();m.put("roundId",UUID.randomUUID().toString());m.put("text",q.text());m.put("options",q.options());m.put("correctOption",q.correctOption());m.put("seconds",q.seconds());return m;
            }).toList();
            var meta=Map.of("id",id.toString(),"pin",pin,"quizId",quizId.toString(),"owner",who.id(),"title",quiz.title(),"questions",questions);
            try {
                int inserted=db.update("insert into game_room(id,quiz_id,owner_id,pin,title,content) values(?,?,?,?,?,?::jsonb) on conflict(id) do nothing",id,quizId,owner,pin,quiz.title(),json.writeValueAsString(meta));
                if(inserted==0)redis.delete("quiz:pin:"+pin);
            } catch(RuntimeException ex){redis.delete("quiz:pin:"+pin);throw ex;}
            existing=db.queryForList("select * from game_room where id=?",id);
        }
        var row=existing.get(0);
        if(!row.get("quiz_id").toString().equals(quizId.toString()))throw new ApiException(409,"COMMAND_CONFLICT");
        JsonNode live=rooms.meta(id.toString());
        if(live==null){
            if(!row.get("phase").equals("PROVISIONING"))throw new ApiException(503,"ROOM_STATE_LOST");
            rooms.command(id.toString(),"init",who.id(),json.readTree(row.get("content").toString()));
        }
        // Register after init. A failed SQL update must not orphan a usable Redis room.
        // Replay repairs registration even when meta already exists; never revive a finished room.
        rooms.registerActive(id.toString());
        if(row.get("phase").equals("PROVISIONING"))
            db.update("update game_room set phase='LOBBY' where id=? and phase='PROVISIONING'",id);
        return snapshot(id,who);
    }
    private String reservePin(String id){
        for(int i=0;i<100;i++){String pin=String.format("%06d",random.nextInt(1000000));
            if(Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("quiz:pin:"+pin,id,Duration.ofHours(2))))return pin;}
        throw new ApiException(503,"PIN_UNAVAILABLE");
    }
    public JsonNode join(String pin,String name,Identity who){
        String id=redis.opsForValue().get("quiz:pin:"+pin);
        if(id==null)throw new ApiException(404,"ROOM_NOT_FOUND");
        String clean=name.trim();if(clean.isEmpty()||clean.length()>24)throw new ApiException(400,"INVALID_NAME");
        rooms.command(id,"join",who.id(),Map.of("name",clean,"normalizedName",clean.toLowerCase(Locale.ROOT),"participantId",UUID.randomUUID().toString()));
        UUID roomId=UUID.fromString(id);JsonNode result=snapshot(roomId,who);sockets.broadcast(id);return result;
    }
    public JsonNode snapshot(UUID id,Identity who){return rooms.command(id.toString(),"snapshot",who.id(),Map.of());}
    public JsonNode answer(UUID id,Identity who,UUID roundId,int option,UUID commandId){
        JsonNode result=rooms.command(id.toString(),"answer",who.id(),Map.of("roundId",roundId.toString(),"option",option,"commandId",commandId.toString()));
        sockets.broadcast(id.toString());return result;
    }
    public JsonNode control(UUID id,Identity who,String action,UUID roundId,UUID commandId,UUID participantId){
        rooms.command(id.toString(),action,who.id(),Map.of("roundId",roundId==null?"":roundId.toString(),"commandId",commandId.toString(),"participantId",participantId==null?"":participantId.toString()));
        JsonNode result=snapshot(id,who);sockets.broadcast(id.toString());return result;
    }
}
