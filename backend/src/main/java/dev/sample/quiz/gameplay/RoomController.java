package dev.sample.quiz.gameplay;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import dev.sample.quiz.identity.Identity;
@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService service;
    public RoomController(RoomService service){this.service=service;}
    public record Create(@NotNull UUID quizId,@NotNull UUID commandId){}
    public record Join(@Pattern(regexp="[0-9]{6}") @NotNull String pin,@NotBlank @Size(max=24) String name){}
    public record Answer(@NotNull UUID roundId,@NotNull @Min(0) @Max(3) Integer option,@NotNull UUID commandId){}
    public record Control(UUID roundId,@NotNull UUID commandId,UUID participantId){}
    @PostMapping JsonNode create(HttpServletRequest r,@Valid @RequestBody Create body){return service.create(Identity.current(r),body.quizId(),body.commandId());}
    @PostMapping("/join") JsonNode join(HttpServletRequest r,@Valid @RequestBody Join body){return service.join(body.pin(),body.name(),Identity.ensure(r));}
    @GetMapping("/{id}") JsonNode snapshot(HttpServletRequest r,@PathVariable UUID id){return service.snapshot(id,Identity.current(r));}
    @PostMapping("/{id}/answers") JsonNode answer(HttpServletRequest r,@PathVariable UUID id,@Valid @RequestBody Answer body){return service.answer(id,Identity.current(r),body.roundId(),body.option(),body.commandId());}
    @PostMapping("/{id}/{action:start|reveal|next|kick}") JsonNode control(HttpServletRequest r,@PathVariable UUID id,@PathVariable String action,@Valid @RequestBody Control body){return service.control(id,Identity.current(r),action,body.roundId(),body.commandId(),body.participantId());}
}
