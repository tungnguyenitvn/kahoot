package dev.sample.quiz.gameplay;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RoomWebSocketHubTest {
    private WebSocketSession socket(String id) {
        var socket=mock(WebSocketSession.class);when(socket.getId()).thenReturn(id);when(socket.isOpen()).thenReturn(true);
        when(socket.getAttributes()).thenReturn(Map.of("roomId","room","principalId","G:"+id,"httpSessionId","session"));
        return socket;
    }
    @Test @SuppressWarnings("unchecked")
    @DisplayName("LIVE-05 burst and SYNC do not spawn another snapshot while one is running; the newest state still ships")
    void burstAndSyncDoNotSpawnAnotherSnapshotWhileOneIsRunning() throws Exception {
        var rooms=mock(RedisRooms.class);SessionRepository<MapSession> sessions=mock(SessionRepository.class);
        when(sessions.findById("session")).thenReturn(new MapSession());
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var now=new AtomicLong(1000);
        when(rooms.command(anyString(),eq("snapshot"),anyString(),any())).thenAnswer(call->{
            entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return new ObjectMapper().readTree("{}");});
        var hub=new RoomWebSocketHub(rooms,new ObjectMapper(),sessions,now::get);var socket=socket("a");
        try {
            hub.register(socket);hub.flush();assertTrue(entered.await(5,TimeUnit.SECONDS));
            for(int i=0;i<1000;i++){hub.broadcast("room");hub.receive(socket,"{\"type\":\"SYNC\"}");hub.flush();}
            verify(rooms,times(1)).command(anyString(),eq("snapshot"),anyString(),any());
        } finally {release.countDown();hub.close();}
    }
    @Test @SuppressWarnings("unchecked")
    void globalPermitBoundPreventsUnboundedTaskSubmission() throws Exception {
        var rooms=mock(RedisRooms.class);SessionRepository<MapSession> sessions=mock(SessionRepository.class);
        when(sessions.findById("session")).thenReturn(new MapSession());
        var entered=new CountDownLatch(16);var release=new CountDownLatch(1);
        when(rooms.command(anyString(),eq("snapshot"),anyString(),any())).thenAnswer(call->{
            entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return new ObjectMapper().readTree("{}");});
        var hub=new RoomWebSocketHub(rooms,new ObjectMapper(),sessions,()->1000L);
        try {
            for(int i=0;i<20;i++)hub.register(socket("s"+i));hub.flush();
            assertTrue(entered.await(5,TimeUnit.SECONDS));hub.flush();
            verify(rooms,times(16)).command(anyString(),eq("snapshot"),anyString(),any());
        } finally {release.countDown();hub.close();}
    }
    @Test @SuppressWarnings("unchecked")
    void expiredSessionIsRevokedBeforeReadingRoom() throws Exception {
        var rooms=mock(RedisRooms.class);SessionRepository<MapSession> sessions=mock(SessionRepository.class);
        var hub=new RoomWebSocketHub(rooms,new ObjectMapper(),sessions,()->1000L);var socket=socket("expired");
        try {hub.register(socket);hub.flush();verify(socket,timeout(2000)).close(CloseStatus.POLICY_VIOLATION);verifyNoInteractions(rooms);}
        finally {hub.close();}
    }
    @Test @SuppressWarnings("unchecked")
    @DisplayName("ROOM-07 registration refuses room capacity before scheduling work and closes with 1012")
    void registrationRefusesRoomCapacityBeforeSchedulingWork() throws Exception {
        var rooms=mock(RedisRooms.class);SessionRepository<MapSession> sessions=mock(SessionRepository.class);
        var hub=new RoomWebSocketHub(rooms,new ObjectMapper(),sessions,()->1000L);
        try {
            for(int i=0;i<150;i++)hub.register(socket("s"+i));var extra=socket("extra");hub.register(extra);
            verify(extra).close(CloseStatus.SERVICE_RESTARTED);verifyNoInteractions(rooms);
        } finally {hub.close();}
    }
}
