package dev.sample.quiz.gameplay.api;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import dev.sample.quiz.gameplay.application.Rooms;
import dev.sample.quiz.gameplay.domain.Nickname;
import dev.sample.quiz.identity.application.Identities;
@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final Rooms rooms; private final Identities identities;
    public RoomController(Rooms rooms, Identities identities) { this.rooms = rooms; this.identities = identities; }
    /** Wire shapes of docs/contracts/rest-api.md, Room commands; the name bound is the domain constant. */
    public record Create(@NotNull UUID quizId, @NotNull UUID commandId) {}
    public record Join(@Pattern(regexp = "[0-9]{6}") @NotNull String pin, @NotBlank @Size(max = Nickname.MAX_LENGTH) String name) {}
    public record Answer(@NotNull UUID roundId, @NotNull @Min(0) @Max(3) Integer option, @NotNull UUID commandId) {}
    public record Control(UUID roundId, @NotNull UUID commandId, UUID participantId) {}
    @PostMapping JsonNode create(HttpServletRequest r, @Valid @RequestBody Create body) { return rooms.create(identities.host(r), body.quizId(), body.commandId()); }
    @PostMapping("/join") JsonNode join(HttpServletRequest r, @Valid @RequestBody Join body) { return rooms.join(body.pin(), body.name(), identities.ensure(r)); }
    @GetMapping("/{id}") JsonNode snapshot(HttpServletRequest r, @PathVariable UUID id) { return rooms.snapshot(id, identities.current(r)); }
    @PostMapping("/{id}/answers") JsonNode answer(HttpServletRequest r, @PathVariable UUID id, @Valid @RequestBody Answer body) { return rooms.answer(id, identities.current(r), body.roundId(), body.option(), body.commandId()); }
    @PostMapping("/{id}/{action:start|reveal|next|kick}") JsonNode control(HttpServletRequest r, @PathVariable UUID id, @PathVariable String action, @Valid @RequestBody Control body) { return rooms.control(id, identities.current(r), action, body.roundId(), body.commandId(), body.participantId()); }
}
