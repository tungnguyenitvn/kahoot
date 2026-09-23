package dev.sample.quiz.archive;
import java.util.*;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import dev.sample.quiz.gameplay.RedisRooms;
@Component
public class ArchiveWorker {
    private static final String GROUP="postgres",CONSUMER="single-instance";
    private static final Logger log=LoggerFactory.getLogger(ArchiveWorker.class);
    private final StringRedisTemplate redis;private final ArchiveTransactions transactions;private final ObjectMapper json;
    public ArchiveWorker(StringRedisTemplate redis,ArchiveTransactions transactions,ObjectMapper json){this.redis=redis;this.transactions=transactions;this.json=json;}
    @Scheduled(fixedDelay=1000) public void drain(){
        try{var ids=redis.opsForSet().members(RedisRooms.ACTIVE);if(ids==null)return;
            for(String id:ids)try{drainRoom(id);}catch(Exception e){log.warn("Archive pending for room {}: {}",id,e.getClass().getSimpleName());}
        }catch(Exception e){log.warn("Archive Redis unavailable");}
    }
    private void drainRoom(String room){
        String stream=RedisRooms.keys(room).get(7);
        if(!Boolean.TRUE.equals(redis.hasKey(stream)))return;
        try{redis.opsForStream().createGroup(stream,ReadOffset.from("0-0"),GROUP);}catch(Exception e){
            if(!String.valueOf(e.getMessage()).contains("BUSYGROUP") && !String.valueOf(e.getCause()).contains("BUSYGROUP"))throw e;
        }
        List<MapRecord<String,Object,Object>> entries=redis.opsForStream().read(Consumer.from(GROUP,CONSUMER),StreamReadOptions.empty().count(100),StreamOffset.create(stream,ReadOffset.from("0-0")));
        if(entries==null||entries.isEmpty())entries=redis.opsForStream().read(Consumer.from(GROUP,CONSUMER),StreamReadOptions.empty().count(100),StreamOffset.create(stream,ReadOffset.lastConsumed()));
        if(entries==null)return;
        for(var entry:entries){
            var event=json.readTree(entry.getValue().get("event").toString());
            transactions.apply(room,entry.getId().getValue(),event);
            if(event.path("type").asText().equals("FINISHED")){
                // One cleanup operation: a process crash cannot leave ACK done but ACTIVE uncleared.
                var keys=new ArrayList<String>();keys.add(stream);keys.add(RedisRooms.ACTIVE);keys.addAll(RedisRooms.keys(room));
                redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
                    "for i=3,#KEYS do redis.call('EXPIRE',KEYS[i],86400) end; " +
                    "redis.call('XACK',KEYS[1],ARGV[1],ARGV[2]); return redis.call('SREM',KEYS[2],ARGV[3])",Long.class),keys,GROUP,entry.getId().getValue(),room);
            }else redis.opsForStream().acknowledge(stream,GROUP,entry.getId());
        }
    }
}
