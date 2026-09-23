package dev.sample.quiz.archive;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
@Service
public class ArchiveTransactions {
    private final JdbcTemplate db;
    public ArchiveTransactions(JdbcTemplate db){this.db=db;}
    @Transactional public void apply(String room,String streamId,JsonNode event){
        UUID id=UUID.fromString(room);long version=event.path("version").asLong();
        Long current=db.queryForObject("select archived_version from game_room where id=? for update",Long.class,id);
        if(current!=null && version<=current)return;
        if(current==null||version!=current+1)throw new IllegalStateException("Archive sequence gap for "+room);
        String type=event.path("type").asText();JsonNode d=event.path("data");
        db.update("insert into game_event(room_id,version,stream_id,kind,payload) values(?,?,?,?,?::jsonb)",id,version,streamId,type,event.toString());
        if(type.equals("JOINED"))db.update("insert into participant(room_id,id,name,principal_id,active) values(?,?,?,?,true)",id,UUID.fromString(d.path("id").asText()),d.path("name").asText(),d.path("principal").asText());
        if(type.equals("KICKED"))db.update("update participant set active=false where room_id=? and id=?",id,UUID.fromString(d.path("id").asText()));
        if(type.equals("ANSWERED"))db.update("insert into answer(room_id,round_id,participant_id,selected_option,correct_order,points,accepted_at) values(?,?,?,?,?,?,?)",id,UUID.fromString(d.path("roundId").asText()),UUID.fromString(d.path("participantId").asText()),d.path("option").asInt(),d.path("correctOrder").asInt(),d.path("points").asInt(),d.path("acceptedAt").asLong());
        String phase=switch(type){case "CREATED"->"LOBBY";case "OPENED"->"QUESTION";case "REVEALED"->"REVEAL";case "FINISHED"->"FINISHED";default->null;};
        if(phase!=null)db.update("update game_room set phase=?,archived_version=?,finished_at=case when ?='FINISHED' then now() else finished_at end where id=?",phase,version,phase,id);
        else db.update("update game_room set archived_version=? where id=?",version,id);
    }
}
