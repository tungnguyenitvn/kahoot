package dev.sample.quiz.catalog.infrastructure;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import dev.sample.quiz.catalog.application.QuizRepository;
import dev.sample.quiz.catalog.domain.Draft;
import dev.sample.quiz.catalog.domain.Quiz;
import dev.sample.quiz.catalog.domain.QuizStatus;
/** PostgreSQL adapter: the quiz table holds the draft as JSONB content next to its status. */
@Repository
public class JdbcQuizRepository implements QuizRepository {
    private final JdbcTemplate jdbc; private final ObjectMapper json;
    public JdbcQuizRepository(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }
    public List<Quiz> findByOwner(UUID owner) {
        return jdbc.query("select id,status,content from quiz where owner_id=? order by created_at desc", this::row, owner);
    }
    public Optional<Quiz> find(UUID owner, UUID id) {
        return jdbc.query("select id,status,content from quiz where id=? and owner_id=?", this::row, id, owner).stream().findFirst();
    }
    public void insert(UUID owner, Quiz quiz) {
        jdbc.update("insert into quiz(id,owner_id,title,status,content) values(?,?,?,?,?::jsonb)",
            UUID.fromString(quiz.id()), owner, quiz.title(), quiz.status().name(), json.writeValueAsString(new Draft(quiz.title(), quiz.questions())));
    }
    public boolean publish(UUID owner, UUID id) {
        return jdbc.update("update quiz set status=? where id=? and owner_id=?", QuizStatus.PUBLISHED.name(), id, owner) == 1;
    }
    private Quiz row(ResultSet rs, int n) throws SQLException {
        Draft content = json.readValue(rs.getString("content"), Draft.class);
        return new Quiz(rs.getString("id"), content.title(), QuizStatus.valueOf(rs.getString("status")), content.questions());
    }
}
