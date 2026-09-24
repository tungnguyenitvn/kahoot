package dev.sample.quiz.catalog.domain;
import java.util.List;
/** What a host submits before a quiz exists; also the JSON stored as quiz content. */
public record Draft(String title, List<Question> questions) {
    public static final int MAX_TITLE = 120, MAX_QUESTIONS = 20;
    public Draft {
        Question.require(title != null && !title.isBlank() && title.length() <= MAX_TITLE, "title");
        Question.require(questions != null && !questions.isEmpty() && questions.size() <= MAX_QUESTIONS, "questions");
        title = title.trim();
        questions = List.copyOf(questions);
    }
}
