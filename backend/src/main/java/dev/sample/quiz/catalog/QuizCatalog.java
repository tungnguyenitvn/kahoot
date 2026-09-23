package dev.sample.quiz.catalog;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import dev.sample.quiz.shared.ApiException;
@Service
public class QuizCatalog {
    private final JdbcTemplate jdbc; private final ObjectMapper json;
    public QuizCatalog(JdbcTemplate jdbc,ObjectMapper json) { this.jdbc=jdbc;this.json=json; }
    public record Question(@NotBlank @Size(max=300) String text,@NotNull @Size(min=4,max=4) List<@NotBlank @Size(max=120) String> options,@NotNull @Min(0) @Max(3) Integer correctOption,@NotNull @Min(5) @Max(120) Integer seconds) {}
    public record Draft(@NotBlank @Size(max=120) String title,@Valid @NotEmpty @Size(max=20) List<@NotNull Question> questions) {}
    public record Quiz(String id,String title,String status,List<@NotNull Question> questions) {}
    public List<Quiz> list(UUID owner) {
        return jdbc.query("select * from quiz where owner_id=? order by created_at desc",(rs,n)-> {
            Draft d=json.readValue(rs.getString("content"),Draft.class);
            return new Quiz(rs.getString("id"),d.title(),rs.getString("status"),d.questions());
        },owner);
    }
    public Quiz get(UUID owner,UUID id) { return list(owner).stream().filter(q->q.id().equals(id.toString())).findFirst().orElseThrow(()->new ApiException(404,"QUIZ_NOT_FOUND")); }
    @Transactional public Quiz create(UUID owner,Draft draft) {
        String id=UUID.randomUUID().toString();
        jdbc.update("insert into quiz(id,owner_id,title,status,content) values(?,?,?,'DRAFT',?::jsonb)",UUID.fromString(id),owner,draft.title().trim(),json.writeValueAsString(draft));
        return new Quiz(id,draft.title(),"DRAFT",draft.questions());
    }
    @Transactional public Quiz publish(UUID owner,UUID id) {
        Quiz q=get(owner,id);
        jdbc.update("update quiz set status='PUBLISHED' where id=? and owner_id=?",id,owner);
        return new Quiz(q.id(),q.title(),"PUBLISHED",q.questions());
    }
}
