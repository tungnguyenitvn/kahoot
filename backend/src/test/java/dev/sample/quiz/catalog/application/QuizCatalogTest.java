package dev.sample.quiz.catalog.application;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.sample.quiz.catalog.domain.Draft;
import dev.sample.quiz.catalog.domain.Question;
import dev.sample.quiz.catalog.domain.Quiz;
import dev.sample.quiz.catalog.domain.QuizStatus;
import dev.sample.quiz.shared.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuizCatalogTest {
    /** In-memory port: the facade is tested without Spring or a database. */
    static final class MemoryQuizzes implements QuizRepository {
        final Map<UUID, Map<UUID, Quiz>> byOwner = new HashMap<>();
        public List<Quiz> findByOwner(UUID owner) { return List.copyOf(byOwner.getOrDefault(owner, Map.of()).values()); }
        public Optional<Quiz> find(UUID owner, UUID id) { return Optional.ofNullable(byOwner.getOrDefault(owner, Map.of()).get(id)); }
        public void insert(UUID owner, Quiz quiz) { byOwner.computeIfAbsent(owner, o -> new LinkedHashMap<>()).put(UUID.fromString(quiz.id()), quiz); }
        public boolean publish(UUID owner, UUID id) {
            var quiz = find(owner, id).orElse(null); if (quiz == null) return false;
            byOwner.get(owner).put(id, new Quiz(quiz.id(), quiz.title(), QuizStatus.PUBLISHED, quiz.questions())); return true;
        }
    }
    static Draft draft() { return new Draft("Quiz", List.of(new Question("text", List.of("a", "b", "c", "d"), 0, 15))); }

    @Test
    @DisplayName("STUDIO-03 publish touches only the current host's quiz: another host gets QUIZ_NOT_FOUND and the draft stays a draft")
    void publishIsScopedToTheOwner() {
        var quizzes = new MemoryQuizzes(); var catalog = new QuizCatalog(quizzes);
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        UUID id = UUID.fromString(catalog.create(alice, draft()).id());

        var denied = assertThrows(ApiException.class, () -> catalog.publish(bob, id));
        assertEquals(404, denied.status); assertEquals("QUIZ_NOT_FOUND", denied.code);
        assertEquals(QuizStatus.DRAFT, catalog.get(alice, id).status());
        assertTrue(catalog.list(bob).isEmpty());

        assertEquals(QuizStatus.PUBLISHED, catalog.publish(alice, id).status());
        assertEquals(QuizStatus.PUBLISHED, catalog.get(alice, id).status());
    }
}
