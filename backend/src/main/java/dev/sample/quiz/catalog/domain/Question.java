package dev.sample.quiz.catalog.domain;
import java.util.List;
/** One multiple-choice question. The bounds are game rules (docs/domain.md); the REST contract repeats the wire-visible ones. */
public record Question(String text, List<String> options, int correctOption, int seconds) {
    public static final int MAX_TEXT = 300, OPTIONS = 4, MAX_OPTION = 120, MIN_SECONDS = 5, MAX_SECONDS = 120;
    public Question {
        require(text != null && !text.isBlank() && text.length() <= MAX_TEXT, "question text");
        require(options != null && options.size() == OPTIONS && options.stream().allMatch(o -> o != null && !o.isBlank() && o.length() <= MAX_OPTION), "question options");
        require(correctOption >= 0 && correctOption < OPTIONS, "correct option");
        require(seconds >= MIN_SECONDS && seconds <= MAX_SECONDS, "question seconds");
        options = List.copyOf(options);
    }
    static void require(boolean holds, String what) { if (!holds) throw new IllegalArgumentException("invalid " + what); }
}
