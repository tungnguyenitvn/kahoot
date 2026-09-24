package dev.sample.quiz.archive.application;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import dev.sample.quiz.archive.domain.HistoryResult;
import dev.sample.quiz.archive.domain.HistoryRoom;
import dev.sample.quiz.shared.ApiException;
/** Read facade of the archive: only host-owned rooms, only completed results. */
@Service
public class History {
    private final HistoryRepository history;
    public History(HistoryRepository history) { this.history = history; }
    public List<HistoryRoom> rooms(UUID owner) { return history.rooms(owner); }
    public List<HistoryResult> result(UUID owner, UUID room) {
        String phase = history.phase(owner, room).orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND"));
        if (!phase.equals("FINISHED")) throw new ApiException(409, "ARCHIVE_NOT_READY");
        return history.results(room);
    }
}
