package dev.sample.quiz.gameplay;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import dev.sample.quiz.shared.ApiException;
@Component
public class RedisRooms {
    public static final String ACTIVE="quiz:active-rooms";
    private final StringRedisTemplate redis;private final ObjectMapper json;
    private final DefaultRedisScript<String> script;
    public RedisRooms(StringRedisTemplate redis,ObjectMapper json){
        this.redis=redis;this.json=json;script=new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/room.lua"));script.setResultType(String.class);
    }
    public static List<String> keys(String id){
        String p="game:{"+id+"}:";
        return List.of(p+"meta",p+"members",p+"names",p+"answers",p+"scores",p+"visible",p+"commands",p+"events");
    }
    public JsonNode command(String id,String op,String principal,Object input){
        String result=redis.execute(script,keys(id),op,principal,json.writeValueAsString(input));
        if(result==null)throw new ApiException(503,"REDIS_NO_RESULT");
        JsonNode body=json.readTree(result);
        if(!body.path("ok").asBoolean())throw new ApiException(body.path("status").asInt(503),body.path("code").asText("REDIS_ERROR"));
        return body.get("result");
    }
    public JsonNode meta(String id){String raw=redis.opsForValue().get(keys(id).get(0));return raw==null?null:json.readTree(raw);}
    public void registerActive(String id) {
        // Standalone-only cross-key operation; serialize against FINISHED + archive cleanup.
        var registration=new DefaultRedisScript<Long>(
            "local raw=redis.call('GET',KEYS[1]); if not raw then return -1 end; " +
            "if cjson.decode(raw).phase=='FINISHED' then return 0 end; " +
            "return redis.call('SADD',KEYS[2],ARGV[1])",Long.class);
        Long result=redis.execute(registration,List.of(keys(id).get(0),ACTIVE),id);
        if(result==null || result<0)throw new ApiException(503,"ROOM_STATE_LOST");
    }
}
