package dev.sample.quiz.archive.api;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import dev.sample.quiz.archive.application.History;
import dev.sample.quiz.archive.domain.HistoryResult;
import dev.sample.quiz.archive.domain.HistoryRoom;
import dev.sample.quiz.identity.application.Identities;
@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final History history; private final Identities identities;
    public HistoryController(History history, Identities identities) { this.history = history; this.identities = identities; }
    @GetMapping List<HistoryRoom> list(HttpServletRequest r) { return history.rooms(identities.host(r).userId()); }
    @GetMapping("/{id}") List<HistoryResult> result(HttpServletRequest r, @PathVariable UUID id) { return history.result(identities.host(r).userId(), id); }
}
