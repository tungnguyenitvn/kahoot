package dev.sample.quiz.catalog.application;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.sample.quiz.catalog.domain.Draft;
import dev.sample.quiz.catalog.domain.Quiz;
import dev.sample.quiz.catalog.domain.QuizStatus;
import dev.sample.quiz.shared.ApiException;
/** Facade of the catalog module: the only entry point other modules use. */
@Service
public class QuizCatalog {
    private final QuizRepository quizzes;
    public QuizCatalog(QuizRepository quizzes) { this.quizzes = quizzes; }
    public List<Quiz> list(UUID owner) { return quizzes.findByOwner(owner); }
    public Quiz get(UUID owner, UUID id) { return quizzes.find(owner, id).orElseThrow(() -> new ApiException(404, "QUIZ_NOT_FOUND")); }
    @Transactional public Quiz create(UUID owner, Draft draft) {
        var quiz = new Quiz(UUID.randomUUID().toString(), draft.title(), QuizStatus.DRAFT, draft.questions());
        quizzes.insert(owner, quiz);
        return quiz;
    }
    @Transactional public Quiz publish(UUID owner, UUID id) {
        var quiz = get(owner, id);
        quizzes.publish(owner, id);
        return new Quiz(quiz.id(), quiz.title(), QuizStatus.PUBLISHED, quiz.questions());
    }
}
