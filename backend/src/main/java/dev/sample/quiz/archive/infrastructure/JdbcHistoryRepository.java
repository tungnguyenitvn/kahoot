package dev.sample.quiz.archive.infrastructure;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import dev.sample.quiz.archive.application.HistoryRepository;
import dev.sample.quiz.archive.domain.HistoryResult;
import dev.sample.quiz.archive.domain.HistoryRoom;
/** PostgreSQL read adapter over the projection; the owner clause is part of every room query. */
@Repository
public class JdbcHistoryRepository implements HistoryRepository {
    private final JdbcTemplate db;
    public JdbcHistoryRepository(JdbcTemplate db) { this.db = db; }
    public List<HistoryRoom> rooms(UUID owner) {
        return db.query("select id,title,phase,archived_version,created_at,finished_at from game_room where owner_id=? order by created_at desc limit 50",
            (rs, n) -> new HistoryRoom(rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("phase"), rs.getLong("archived_version"),
                rs.getObject("created_at", OffsetDateTime.class), rs.getObject("finished_at", OffsetDateTime.class)), owner);
    }
    public Optional<String> phase(UUID owner, UUID room) {
        return db.query("select phase from game_room where id=? and owner_id=?", (rs, n) -> rs.getString(1), room, owner).stream().findFirst();
    }
    public List<HistoryResult> results(UUID room) {
        return db.query("select p.id,p.name,coalesce(sum(a.points),0) as score from participant p left join answer a on a.room_id=p.room_id and a.participant_id=p.id where p.room_id=? group by p.id,p.name order by score desc,p.name",
            (rs, n) -> new HistoryResult(rs.getObject("id", UUID.class), rs.getString("name"), rs.getLong("score")), room);
    }
}
