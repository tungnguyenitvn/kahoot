package dev.sample.quiz.gameplay.application;
import tools.jackson.databind.JsonNode;
/** Port to the room aggregate, which executes in Lua: one invocation is one atomic room decision (docs/contracts/redis-room.md). */
public interface RoomCommands {
    /** Runs one command; a domain refusal surfaces as ApiException with the code the script returned. */
    JsonNode command(String room, String op, String principal, Object input);
    /** The live meta of a room, or null when Redis holds none. */
    JsonNode meta(String room);
    /** Adds the room to the active set unless it is already FINISHED; ROOM_STATE_LOST when no live meta exists. */
    void registerActive(String room);
}
