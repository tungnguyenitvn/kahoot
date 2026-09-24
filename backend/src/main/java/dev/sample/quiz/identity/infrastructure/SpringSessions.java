package dev.sample.quiz.identity.infrastructure;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import dev.sample.quiz.identity.application.Sessions;
/** Session liveness over Spring Session's repository (Redis-backed in this deployment). */
@Component
public class SpringSessions implements Sessions {
    private final SessionRepository<?> sessions;
    public SpringSessions(SessionRepository<?> sessions) { this.sessions = sessions; }
    public boolean live(String sessionId) { return sessions.findById(sessionId) != null; }
}
