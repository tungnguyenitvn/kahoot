package dev.sample.quiz.gameplay.infrastructure;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import dev.sample.quiz.gameplay.application.RoomProvisioning;
import dev.sample.quiz.gameplay.domain.ProvisionedRoom;
import dev.sample.quiz.gameplay.domain.RoomContent;
/** PostgreSQL adapter for the provisioning columns of game_room. */
@Repository
public class JdbcRoomProvisioning implements RoomProvisioning {
    private final JdbcTemplate db; private final ObjectMapper json;
    public JdbcRoomProvisioning(JdbcTemplate db, ObjectMapper json) { this.db = db; this.json = json; }
    @SuppressWarnings("unchecked")
    public Optional<ProvisionedRoom> find(UUID room) {
        return db.query("select id,quiz_id,phase,content from game_room where id=?",
            (rs, n) -> new ProvisionedRoom(rs.getObject("id", UUID.class), rs.getObject("quiz_id", UUID.class), rs.getString("phase"), json.readValue(rs.getString("content"), Map.class)), room).stream().findFirst();
    }
    public boolean insertIfAbsent(UUID room, UUID quizId, UUID owner, String pin, String title, RoomContent content) {
        return db.update("insert into game_room(id,quiz_id,owner_id,pin,title,content) values(?,?,?,?,?,?::jsonb) on conflict(id) do nothing",
            room, quizId, owner, pin, title, json.writeValueAsString(content)) == 1;
    }
    public void markLobby(UUID room) { db.update("update game_room set phase='LOBBY' where id=? and phase='PROVISIONING'", room); }
}
