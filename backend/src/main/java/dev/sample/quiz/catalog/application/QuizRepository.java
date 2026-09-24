package dev.sample.quiz.catalog.application;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.sample.quiz.catalog.domain.Quiz;
/** Port to the quiz store; every method is scoped to the owning host. */
public interface QuizRepository {
    List<Quiz> findByOwner(UUID owner);
    Optional<Quiz> find(UUID owner, UUID id);
    void insert(UUID owner, Quiz quiz);
    boolean publish(UUID owner, UUID id);
}
