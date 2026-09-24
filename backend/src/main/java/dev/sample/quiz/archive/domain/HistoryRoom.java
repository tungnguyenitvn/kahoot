package dev.sample.quiz.archive.domain;
import java.time.OffsetDateTime;
import java.util.UUID;
/** A host's room as the history lists it. Explicit camelCase fields: raw SQL column names never leak into JSON (conventions). */
public record HistoryRoom(UUID id, String title, String phase, long archivedVersion, OffsetDateTime createdAt, OffsetDateTime finishedAt) {}
