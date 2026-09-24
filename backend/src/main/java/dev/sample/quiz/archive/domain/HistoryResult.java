package dev.sample.quiz.archive.domain;
import java.util.UUID;
/** One participant's archived total for a finished room. */
public record HistoryResult(UUID id, String name, long score) {}
