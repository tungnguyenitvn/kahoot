package dev.sample.quiz.archive.application;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
/** One pass over the active rooms: read, project, acknowledge. Kept apart from Projection so that apply runs through its transactional proxy. */
@Service
public class Archiver {
    private static final Logger log = LoggerFactory.getLogger(Archiver.class);
    private final RoomEvents events; private final Projection projection;
    public Archiver(RoomEvents events, Projection projection) { this.events = events; this.projection = projection; }
    /** A failing room is logged and retried on the next pass; the other rooms continue. */
    public void drain() {
        Set<String> rooms;
        try { rooms = events.activeRooms(); } catch (RuntimeException e) { log.warn("Archive Redis unavailable"); return; }
        for (String room : rooms) {
            try { drain(room); } catch (RuntimeException e) { log.warn("Archive pending for room {}: {}", room, e.getClass().getSimpleName()); }
        }
    }
    void drain(String room) {
        for (var pending : events.read(room)) {
            projection.apply(room, pending.entryId(), pending.event());
            if (pending.event().type().terminal()) events.finish(room, pending.entryId()); else events.ack(room, pending.entryId());
        }
    }
}
