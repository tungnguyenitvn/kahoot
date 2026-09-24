package dev.sample.quiz.archive.infrastructure;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import dev.sample.quiz.archive.application.ArchiveRepository;
import dev.sample.quiz.archive.domain.EventType;
/** PostgreSQL adapter for the projection tables game_event, participant, answer and the projection columns of game_room. */
@Repository
public class JdbcArchiveRepository implements ArchiveRepository {
    private final JdbcTemplate db; private final ObjectMapper json;
    public JdbcArchiveRepository(JdbcTemplate db, ObjectMapper json) { this.db = db; this.json = json; }
    public Optional<Long> lockArchivedVersion(UUID room) {
        return db.query("select archived_version from game_room where id=? for update", (rs, n) -> rs.getLong(1), room).stream().findFirst();
    }
    public void recordEvent(UUID room, long version, String streamId, EventType type, Map<String, Object> envelope) {
        db.update("insert into game_event(room_id,version,stream_id,kind,payload) values(?,?,?,?,?::jsonb)", room, version, streamId, type.name(), json.writeValueAsString(envelope));
    }
    public void addParticipant(UUID room, UUID participant, String name, String principal) {
        db.update("insert into participant(room_id,id,name,principal_id,active) values(?,?,?,?,true)", room, participant, name, principal);
    }
    public void deactivateParticipant(UUID room, UUID participant) {
        db.update("update participant set active=false where room_id=? and id=?", room, participant);
    }
    public void recordAnswer(UUID room, UUID round, UUID participant, int option, int correctOrder, int points, long acceptedAt) {
        db.update("insert into answer(room_id,round_id,participant_id,selected_option,correct_order,points,accepted_at) values(?,?,?,?,?,?,?)", room, round, participant, option, correctOrder, points, acceptedAt);
    }
    public void advance(UUID room, long version, String phase) {
        if (phase != null) db.update("update game_room set phase=?,archived_version=?,finished_at=case when ?='FINISHED' then now() else finished_at end where id=?", phase, version, phase, room);
        else db.update("update game_room set archived_version=? where id=?", version, room);
    }
}
