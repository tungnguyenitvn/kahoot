package dev.sample.quiz.gameplay.application;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import dev.sample.quiz.catalog.application.QuizCatalog;
import dev.sample.quiz.catalog.domain.QuizStatus;
import dev.sample.quiz.gameplay.domain.Nickname;
import dev.sample.quiz.gameplay.domain.RoomContent;
import dev.sample.quiz.gameplay.domain.RoomId;
import dev.sample.quiz.identity.domain.Identity;
import dev.sample.quiz.shared.ApiException;
/** Facade of the gameplay module: provisioning across two stores and every live command. Live decisions stay in Lua behind RoomCommands. */
@Service
public class Rooms {
    private final RoomCommands commands; private final RoomRegistry registry; private final RoomProvisioning provisioning;
    private final QuizCatalog catalog; private final Notifier notifier;
    public Rooms(RoomCommands commands, RoomRegistry registry, RoomProvisioning provisioning, QuizCatalog catalog, Notifier notifier) {
        this.commands = commands; this.registry = registry; this.provisioning = provisioning; this.catalog = catalog; this.notifier = notifier;
    }
    /** Retry repair, not a transaction: every step is idempotent and a replay finishes whatever the previous attempt left (backend.md, Gameplay). */
    public JsonNode create(Identity who, UUID quizId, UUID commandId) {
        UUID owner = who.userId();
        UUID id = RoomId.of(who.id(), commandId);
        var existing = provisioning.find(id);
        if (existing.isEmpty()) {
            if (registry.activeRooms() >= 100) throw new ApiException(503, "ROOM_LIMIT");
            var quiz = catalog.get(owner, quizId);
            if (quiz.status() != QuizStatus.PUBLISHED) throw new ApiException(409, "QUIZ_NOT_PUBLISHED");
            String pin = registry.reservePin(id.toString()).orElseThrow(() -> new ApiException(503, "PIN_UNAVAILABLE"));
            try {
                if (!provisioning.insertIfAbsent(id, quizId, owner, pin, quiz.title(), RoomContent.freeze(id, pin, quizId, who.id(), quiz))) registry.releasePin(pin);
            } catch (RuntimeException ex) { registry.releasePin(pin); throw ex; }
            existing = provisioning.find(id);
        }
        var row = existing.orElseThrow(() -> new ApiException(503, "ROOM_STATE_LOST"));
        if (!row.quizId().equals(quizId)) throw new ApiException(409, "COMMAND_CONFLICT");
        if (commands.meta(id.toString()) == null) {
            if (!row.provisioning()) throw new ApiException(503, "ROOM_STATE_LOST");
            commands.command(id.toString(), "init", who.id(), row.content());
        }
        // Register after init. A failed SQL update must not orphan a usable Redis room.
        // Replay repairs registration even when meta already exists; never revive a finished room.
        commands.registerActive(id.toString());
        if (row.provisioning()) provisioning.markLobby(id);
        return snapshot(id, who);
    }
    public JsonNode join(String pin, String name, Identity who) {
        String id = registry.roomByPin(pin).orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND"));
        Nickname nickname = Nickname.parse(name).orElseThrow(() -> new ApiException(400, "INVALID_NAME"));
        commands.command(id, "join", who.id(), Map.of("name", nickname.value(), "normalizedName", nickname.normalized(), "participantId", UUID.randomUUID().toString()));
        JsonNode result = snapshot(UUID.fromString(id), who);
        notifier.broadcast(id);
        return result;
    }
    public JsonNode snapshot(UUID id, Identity who) { return commands.command(id.toString(), "snapshot", who.id(), Map.of()); }
    public JsonNode answer(UUID id, Identity who, UUID roundId, int option, UUID commandId) {
        JsonNode result = commands.command(id.toString(), "answer", who.id(), Map.of("roundId", roundId.toString(), "option", option, "commandId", commandId.toString()));
        notifier.broadcast(id.toString());
        return result;
    }
    public JsonNode control(UUID id, Identity who, String action, UUID roundId, UUID commandId, UUID participantId) {
        commands.command(id.toString(), action, who.id(), Map.of("roundId", roundId == null ? "" : roundId.toString(), "commandId", commandId.toString(), "participantId", participantId == null ? "" : participantId.toString()));
        JsonNode result = snapshot(id, who);
        notifier.broadcast(id.toString());
        return result;
    }
}
