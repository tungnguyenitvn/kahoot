package dev.sample.quiz.gameplay;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import dev.sample.quiz.identity.Identity;
import dev.sample.quiz.catalog.QuizCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RoomServiceTest {
    @Test @SuppressWarnings("unchecked")
    @DisplayName("STUDIO-05 SQL failure after init must not remove the live registration; retry repairs it")
    void sqlFailureAfterInitMustNotRemoveLiveRegistrationAndRetryRepairsIt() {
        var json=new ObjectMapper();var rooms=mock(RedisRooms.class);var redis=mock(StringRedisTemplate.class);
        SetOperations<String,String> active=mock(SetOperations.class);when(redis.opsForSet()).thenReturn(active);
        var db=mock(JdbcTemplate.class);UUID quiz=UUID.randomUUID();
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of(
            "quiz_id",quiz,"phase","PROVISIONING","content","{}")));
        when(rooms.meta(anyString())).thenReturn(null,json.readTree("{\"phase\":\"LOBBY\"}"));
        when(db.update(eq("update game_room set phase='LOBBY' where id=? and phase='PROVISIONING'"),any(Object[].class)))
            .thenThrow(new DataAccessResourceFailureException("fault after init")).thenReturn(1);
        var service=new RoomService(rooms,redis,db,mock(QuizCatalog.class),json,mock(RoomWebSocketHub.class));
        var who=new Identity("U:"+UUID.randomUUID(),"Host",true);UUID command=UUID.randomUUID();
        assertThrows(DataAccessResourceFailureException.class,()->service.create(who,quiz,command));
        verify(active,never()).remove(anyString(),any(Object[].class));
        service.create(who,quiz,command);
        verify(rooms,times(2)).registerActive(anyString());
        verify(rooms,times(1)).command(anyString(),eq("init"),eq(who.id()),any());
    }
}
