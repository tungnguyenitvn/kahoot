package dev.sample.quiz.gameplay.application;
import java.util.Optional;
import java.util.Set;
/** Port to the room-level Redis state that lives outside the Lua script: the active set and the PIN reservations. */
public interface RoomRegistry {
    long activeRooms();
    Set<String> activeRoomIds();
    /** Reserves a fresh PIN for the room; empty when the adapter ran out of attempts. */
    Optional<String> reservePin(String room);
    void releasePin(String pin);
    Optional<String> roomByPin(String pin);
}
