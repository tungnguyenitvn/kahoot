package dev.sample.quiz.catalog.domain;
import java.util.List;
/** Host-owned quiz as other modules and the wire see it; ownership is resolved by the repository, never carried here. */
public record Quiz(String id, String title, QuizStatus status, List<Question> questions) {}
