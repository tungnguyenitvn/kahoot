package dev.sample.quiz.gameplay.application;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import dev.sample.quiz.catalog.application.QuizCatalog;
import dev.sample.quiz.gameplay.domain.ProvisionedRoom;
import dev.sample.quiz.identity.domain.Identity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RoomsTest {
    @Test
    @DisplayName("STUDIO-05 SQL failure after init must not remove the live registration; retry repairs it")
    void sqlFailureAfterInitMustNotRemoveLiveRegistrationAndRetryRepairsIt() {
        var json = new ObjectMapper();
        var commands = mock(RoomCommands.class); var registry = mock(RoomRegistry.class); var provisioning = mock(RoomProvisioning.class);
        UUID quiz = UUID.randomUUID();
        when(provisioning.find(any())).thenAnswer(call -> Optional.of(new ProvisionedRoom(call.getArgument(0), quiz, "PROVISIONING", Map.of())));
        when(commands.meta(anyString())).thenReturn(null, json.readTree("{\"phase\":\"LOBBY\"}"));
        doThrow(new DataAccessResourceFailureException("fault after init")).doNothing().when(provisioning).markLobby(any());
        var rooms = new Rooms(commands, registry, provisioning, mock(QuizCatalog.class), mock(Notifier.class));
        var who = new Identity("U:" + UUID.randomUUID(), "Host", true); UUID command = UUID.randomUUID();

        assertThrows(DataAccessResourceFailureException.class, () -> rooms.create(who, quiz, command));
        rooms.create(who, quiz, command);

        verify(commands, times(2)).registerActive(anyString());
        verify(commands, times(1)).command(anyString(), eq("init"), eq(who.id()), any());
        verify(registry, never()).releasePin(anyString());
        verifyNoMoreInteractions(registry);
    }
}
