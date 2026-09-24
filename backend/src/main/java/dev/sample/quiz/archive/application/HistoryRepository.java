package dev.sample.quiz.archive.application;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.sample.quiz.archive.domain.HistoryResult;
import dev.sample.quiz.archive.domain.HistoryRoom;
/** Read port over the projection; ownership is applied here, never by the caller. */
public interface HistoryRepository {
    List<HistoryRoom> rooms(UUID owner);
    Optional<String> phase(UUID owner, UUID room);
    List<HistoryResult> results(UUID room);
}
