package dev.sample.quiz.gameplay.domain;

import java.util.List;
import java.util.UUID;
import dev.sample.quiz.catalog.domain.Question;
import dev.sample.quiz.catalog.domain.Quiz;
import dev.sample.quiz.catalog.domain.QuizStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoomDomainTest {
    @Test
    @DisplayName("a nickname is trimmed, bounded and compared case-insensitively; a blank one does not exist")
    void nicknameInvariants() {
        var ann = new Nickname("  Ann  ");
        assertEquals("Ann", ann.value()); assertEquals("ann", ann.normalized());
        assertTrue(Nickname.parse("   ").isEmpty());
        assertTrue(Nickname.parse(null).isEmpty());
        assertTrue(Nickname.parse("x".repeat(Nickname.MAX_LENGTH + 1)).isEmpty());
        assertTrue(Nickname.parse("x".repeat(Nickname.MAX_LENGTH)).isPresent());
    }

    @Test
    @DisplayName("a room id is deterministic for host identity + commandId, so a retried create finds its room")
    void roomIdIsDeterministic() {
        UUID command = UUID.randomUUID();
        assertEquals(RoomId.of("U:host", command), RoomId.of("U:host", command));
        assertNotEquals(RoomId.of("U:host", command), RoomId.of("U:other", command));
        assertNotEquals(RoomId.of("U:host", command), RoomId.of("U:host", UUID.randomUUID()));
    }

    @Test
    @DisplayName("freezing a published quiz copies every question and gives each round its own id; a draft cannot be frozen")
    void freezeCopiesQuestionsWithRoundIds() {
        var questions = List.of(new Question("one", List.of("a", "b", "c", "d"), 0, 15), new Question("two", List.of("a", "b", "c", "d"), 3, 30));
        var published = new Quiz("q", "Quiz", QuizStatus.PUBLISHED, questions);
        UUID room = UUID.randomUUID(), quizId = UUID.randomUUID();
        var content = RoomContent.freeze(room, "123456", quizId, "U:host", published);

        assertEquals(room.toString(), content.id()); assertEquals("123456", content.pin()); assertEquals("Quiz", content.title());
        assertEquals(2, content.questions().size());
        assertEquals("two", content.questions().get(1).text()); assertEquals(3, content.questions().get(1).correctOption()); assertEquals(30, content.questions().get(1).seconds());
        assertNotEquals(content.questions().get(0).roundId(), content.questions().get(1).roundId());
        assertThrows(IllegalArgumentException.class, () -> RoomContent.freeze(room, "123456", quizId, "U:host", new Quiz("q", "Quiz", QuizStatus.DRAFT, questions)));
    }
}
