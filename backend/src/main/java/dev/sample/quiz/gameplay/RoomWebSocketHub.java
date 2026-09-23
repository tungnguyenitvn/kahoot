package dev.sample.quiz.gameplay;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import dev.sample.quiz.shared.ApiException;

@Component
public class RoomWebSocketHub {
    private static final int MAX_CLIENTS = 1000;
    private static final int MAX_CLIENTS_PER_ROOM = 150;
    private final RedisRooms rooms;
    private final ObjectMapper json;
    private final SessionRepository<?> sessions;
    private final Set<Client> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService sender = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore inFlight = new Semaphore(16);
    private final LongSupplier nowMillis;
    private volatile boolean stopped;

    @Autowired
    public RoomWebSocketHub(RedisRooms rooms, ObjectMapper json, SessionRepository<?> sessions) {
        this(rooms,json,sessions,()->System.nanoTime()/1_000_000);
    }

    RoomWebSocketHub(RedisRooms rooms, ObjectMapper json, SessionRepository<?> sessions, LongSupplier nowMillis) {
        this.rooms = rooms;
        this.json = json;
        this.sessions = sessions;
        this.nowMillis = nowMillis;
    }

    public void register(WebSocketSession raw) {
        String room = attribute(raw, "roomId");
        // Admission count + registration must be one operation under parallel upgrades.
        synchronized (clients) {
            long roomCount = clients.stream().filter(c -> c.room.equals(room)).count();
            if (stopped || clients.size() >= MAX_CLIENTS || roomCount >= MAX_CLIENTS_PER_ROOM) {
                close(raw, CloseStatus.SERVICE_RESTARTED);
                return;
            }
            WebSocketSession session = new ConcurrentWebSocketSessionDecorator(raw, 10_000, 256 * 1024);
            clients.add(new Client(room, attribute(raw, "principalId"), attribute(raw, "httpSessionId"), session, nowMillis.getAsLong()));
        }
    }

    public void remove(WebSocketSession session) {
        clients.removeIf(client -> client.session.getId().equals(session.getId()));
    }

    public void receive(WebSocketSession session, String payload) {
        Client client = clients.stream().filter(c -> c.session.getId().equals(session.getId())).findFirst().orElse(null);
        if (client == null) return;
        if (payload.length() > 1024) {
            remove(session); close(session, CloseStatus.TOO_BIG_TO_PROCESS); return;
        }
        try {
            JsonNode message = json.readTree(payload);
            String type = message.path("type").asText("");
            if ("SYNC".equals(type)) {
                client.dirty.set(true);
            } else if (!allowControlReply(client)) {
                return;
            } else if ("PING".equals(type)) {
                send(client, Map.of("type", "PONG"));
            } else {
                send(client, Map.of("type", "ERROR", "code", "READ_ONLY_CHANNEL", "retryable", false));
            }
        } catch (RuntimeException ex) {
            if (allowControlReply(client)) send(client, Map.of("type", "ERROR", "code", "INVALID_MESSAGE", "retryable", false));
        }
    }

    public void broadcast(String room) {
        clients.stream().filter(client -> client.room.equals(room)).forEach(client -> client.dirty.set(true));
    }

    private boolean allowControlReply(Client client) {
        long now=nowMillis.getAsLong(), previous=client.lastControl.get();
        return now-previous>=1000 && client.lastControl.compareAndSet(previous,now);
    }

    @Scheduled(fixedDelay = 50)
    public void flush() {
        if (stopped) return;
        long now=nowMillis.getAsLong();
        // Oldest first: sustained dirty clients must not starve quiet session checks.
        for (Client client : clients.stream().sorted(java.util.Comparator.comparingLong(c->c.lastStarted)).toList()) {
            if (!client.session.isOpen()) { remove(client.session); continue; }
            if (now-client.lastStarted<250 || (!client.dirty.get() && now-client.lastStarted<5000)) continue;
            if (!inFlight.tryAcquire()) break;
            if (!client.sending.compareAndSet(false,true)) { inFlight.release(); continue; }
            client.dirty.set(false);client.lastStarted=now;
            try {
                sender.execute(()->{
                    try { if (!stopped && clients.contains(client)) sendSnapshot(client); }
                    finally { client.sending.set(false);inFlight.release(); }
                });
            } catch (RejectedExecutionException ex) {
                client.dirty.set(true);client.sending.set(false);inFlight.release();
            }
        }
    }

    private void sendSnapshot(Client client) {
        try {
            if (!client.session.isOpen()) {
                remove(client.session);
                return;
            }
            if (sessions.findById(client.httpSessionId) == null) {
                revoke(client, "SESSION_EXPIRED");
                return;
            }
            JsonNode snapshot = rooms.command(client.room, "snapshot", client.principal, Map.of());
            send(client, Map.of("type", "STATE", "room", snapshot));
        } catch (ApiException ex) {
            if (ex.status == 401 || ex.status == 403 || ex.status == 404) revoke(client, ex.code);
            else send(client, Map.of("type", "ERROR", "code", ex.code, "retryable", true));
        } catch (Exception ex) {
            send(client, Map.of("type", "ERROR", "code", "REALTIME_UNAVAILABLE", "retryable", true));
        }
    }

    private void send(Client client, Object body) {
        try {
            if (client.session.isOpen()) client.session.sendMessage(new TextMessage(json.writeValueAsString(body)));
        } catch (Exception ex) {
            remove(client.session);
            close(client.session, CloseStatus.NOT_ACCEPTABLE);
        }
    }

    private void revoke(Client client, String code) {
        send(client, Map.of("type", "REVOKED", "code", code));
        remove(client.session);
        close(client.session, CloseStatus.POLICY_VIOLATION);
    }

    private static String attribute(WebSocketSession session, String name) {
        Object value = session.getAttributes().get(name);
        if (value == null) throw new IllegalStateException("Missing WebSocket handshake attribute: " + name);
        return value.toString();
    }

    private static void close(WebSocketSession session, CloseStatus status) {
        try { if (session.isOpen()) session.close(status); } catch (Exception ignored) { }
    }

    @PreDestroy
    public void close() {
        stopped=true;
        sender.shutdownNow();
        clients.forEach(client -> close(client.session, CloseStatus.GOING_AWAY));
        clients.clear();
    }

    private static final class Client {
        final String room, principal, httpSessionId;
        final WebSocketSession session;
        final AtomicBoolean dirty=new AtomicBoolean(true), sending=new AtomicBoolean();
        final AtomicLong lastControl;
        volatile long lastStarted;
        Client(String room,String principal,String httpSessionId,WebSocketSession session,long now) {
            this.room=room;this.principal=principal;this.httpSessionId=httpSessionId;this.session=session;
            this.lastStarted=now-5000;this.lastControl=new AtomicLong(now-1000);
        }
    }
}
