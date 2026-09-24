package dev.sample.quiz.archive;
import java.time.OffsetDateTime;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import dev.sample.quiz.identity.application.Identities;
import dev.sample.quiz.shared.ApiException;
@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final JdbcTemplate db;private final Identities identities;
    public HistoryController(JdbcTemplate db,Identities identities){this.db=db;this.identities=identities;}
    // Explicit camelCase projections: raw SQL column names must not leak into JSON (conventions).
    public record HistoryRoom(UUID id,String title,String phase,long archivedVersion,OffsetDateTime createdAt,OffsetDateTime finishedAt){}
    public record HistoryResult(UUID id,String name,long score){}
    @GetMapping List<HistoryRoom> list(HttpServletRequest r){
        return db.query("select id,title,phase,archived_version,created_at,finished_at from game_room where owner_id=? order by created_at desc limit 50",
            (rs,n)->new HistoryRoom(rs.getObject("id",UUID.class),rs.getString("title"),rs.getString("phase"),rs.getLong("archived_version"),rs.getObject("created_at",OffsetDateTime.class),rs.getObject("finished_at",OffsetDateTime.class)),
            identities.host(r).userId());
    }
    @GetMapping("/{id}") List<HistoryResult> result(HttpServletRequest r,@PathVariable UUID id){
        var rows=db.queryForList("select phase from game_room where id=? and owner_id=?",id,identities.host(r).userId());
        if(rows.isEmpty())throw new ApiException(404,"ROOM_NOT_FOUND");
        if(!rows.get(0).get("phase").equals("FINISHED"))throw new ApiException(409,"ARCHIVE_NOT_READY");
        return db.query("select p.id,p.name,coalesce(sum(a.points),0) as score from participant p left join answer a on a.room_id=p.room_id and a.participant_id=p.id where p.room_id=? group by p.id,p.name order by score desc,p.name",
            (rs,n)->new HistoryResult(rs.getObject("id",UUID.class),rs.getString("name"),rs.getLong("score")),id);
    }
}
