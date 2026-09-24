package dev.sample.quiz.identity.application;

import java.util.Optional;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import dev.sample.quiz.identity.domain.Account;
import dev.sample.quiz.shared.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentitiesTest {
    static Identities identities(Account signedIn) { return new Identities(() -> Optional.ofNullable(signedIn)); }
    static HttpServletRequest request(HttpSession session) {
        var request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        return request;
    }

    @Test
    @DisplayName("no host and no guest in the session is SESSION_REQUIRED; ensure creates the guest once and keeps it")
    void guestIsBootstrappedOnce() {
        var session = mock(HttpSession.class); var identities = identities(null);
        var denied = assertThrows(ApiException.class, () -> identities.current(request(null)));
        assertEquals(401, denied.status); assertEquals("SESSION_REQUIRED", denied.code);

        when(session.getAttribute(Identities.GUEST)).thenReturn(null, "guest-1", "guest-1");
        var guest = identities.ensure(request(session));
        verify(session).setAttribute(eq(Identities.GUEST), anyString());
        assertEquals("G:guest-1", guest.id()); assertFalse(guest.host());
        assertEquals("G:guest-1", identities.ensure(request(session)).id());
        verify(session, times(1)).setAttribute(eq(Identities.GUEST), anyString());
    }

    @Test
    @DisplayName("JOIN-04 a guest identity never passes as a host: host() is LOGIN_REQUIRED and the signed-in account wins over the session guest")
    void hostRequiresAnAccount() {
        var session = mock(HttpSession.class); when(session.getAttribute(Identities.GUEST)).thenReturn("guest-1");
        var denied = assertThrows(ApiException.class, () -> identities(null).host(request(session)));
        assertEquals(403, denied.status); assertEquals("LOGIN_REQUIRED", denied.code);

        var account = new Account(UUID.randomUUID(), "host@example.test", "Demo Host", "hash");
        var host = identities(account).host(request(session));
        assertTrue(host.host()); assertEquals("U:" + account.id(), host.id()); assertEquals(account.id(), host.userId());
        assertEquals("Demo Host", host.name());
    }
}
