package dev.sample.quiz.gameplay.domain;
import java.util.List;
import java.util.UUID;
import dev.sample.quiz.catalog.domain.Quiz;
import dev.sample.quiz.catalog.domain.QuizStatus;
/** The frozen quiz a room plays: copied at creation, round ids generated then, never changed afterwards (docs/domain.md). Serialized as the room's SQL content and the Lua init input. */
public record RoomContent(String id, String pin, String quizId, String owner, String title, List<Round> questions) {
    public record Round(String roundId, String text, List<String> options, int correctOption, int seconds) {}
    public static RoomContent freeze(UUID id, String pin, UUID quizId, String owner, Quiz quiz) {
        if (quiz.status() != QuizStatus.PUBLISHED) throw new IllegalArgumentException("only a published quiz can be frozen into a room");
        var rounds = quiz.questions().stream().map(q -> new Round(UUID.randomUUID().toString(), q.text(), q.options(), q.correctOption(), q.seconds())).toList();
        return new RoomContent(id.toString(), pin, quizId.toString(), owner, quiz.title(), rounds);
    }
}
