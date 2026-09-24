package dev.sample.quiz.gameplay.application;
import java.util.Optional;
import java.util.UUID;
import dev.sample.quiz.gameplay.domain.ProvisionedRoom;
import dev.sample.quiz.gameplay.domain.RoomContent;
/** Port to the provisioning columns of game_room, the SQL side gameplay owns; archive owns the projection columns. */
public interface RoomProvisioning {
    Optional<ProvisionedRoom> find(UUID room);
    /** Inserts the row; false when it already existed. */
    boolean insertIfAbsent(UUID room, UUID quizId, UUID owner, String pin, String title, RoomContent content);
    void markLobby(UUID room);
}
