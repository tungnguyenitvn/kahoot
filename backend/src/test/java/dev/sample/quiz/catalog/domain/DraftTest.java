package dev.sample.quiz.catalog.domain;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DraftTest {
    static Question question() { return new Question("text", List.of("a", "b", "c", "d"), 0, 15); }

    @Test
    @DisplayName("STUDIO-02 a draft without a title, without questions or with a malformed question cannot be built")
    void rejectsMissingTitleQuestionsOrOptions() {
        assertThrows(IllegalArgumentException.class, () -> new Draft(" ", List.of(question())));
        assertThrows(IllegalArgumentException.class, () -> new Draft("Quiz", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Question("text", List.of("a", "b", "c"), 0, 15));
        assertThrows(IllegalArgumentException.class, () -> new Question("text", List.of("a", "", "c", "d"), 0, 15));
        assertThrows(IllegalArgumentException.class, () -> new Question("text", List.of("a", "b", "c", "d"), Question.OPTIONS, 15));
        assertThrows(IllegalArgumentException.class, () -> new Question("text", List.of("a", "b", "c", "d"), 0, Question.MIN_SECONDS - 1));
        assertThrows(IllegalArgumentException.class, () -> new Question("", List.of("a", "b", "c", "d"), 0, 15));
    }

    @Test
    @DisplayName("STUDIO-02 a valid draft keeps its questions and trims the title")
    void keepsValidContent() {
        var draft = new Draft("  Quiz  ", List.of(question()));
        assertEquals("Quiz", draft.title());
        assertEquals(1, draft.questions().size());
        assertEquals(List.of("a", "b", "c", "d"), draft.questions().get(0).options());
    }
}
