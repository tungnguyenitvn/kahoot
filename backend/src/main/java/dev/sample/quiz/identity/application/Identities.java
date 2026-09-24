package dev.sample.quiz.identity.application;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import dev.sample.quiz.identity.domain.Identity;
import dev.sample.quiz.shared.ApiException;
/** Facade that answers "who is calling" for every module; it owns the 401 and 403 decisions. */
@Service
public class Identities {
    static final String GUEST = "guestId";
    private final Principals principals;
    public Identities(Principals principals) { this.principals = principals; }
    /** The signed-in host, else the guest already in the HTTP session; SESSION_REQUIRED when there is neither. */
    public Identity current(HttpServletRequest request) {
        var host = principals.authenticated();
        if (host.isPresent()) return Identity.ofHost(host.get());
        var session = request.getSession(false);
        Object guest = session == null ? null : session.getAttribute(GUEST);
        if (guest == null) throw new ApiException(401, "SESSION_REQUIRED");
        return Identity.ofGuest(guest.toString());
    }
    /** Bootstraps a guest identity in the session when none exists; PIN and display name are not credentials. */
    public Identity ensure(HttpServletRequest request) {
        var session = request.getSession();
        synchronized (session) { if (session.getAttribute(GUEST) == null) session.setAttribute(GUEST, UUID.randomUUID().toString()); }
        return current(request);
    }
    /** The calling host; a guest gets LOGIN_REQUIRED. */
    public Identity host(HttpServletRequest request) {
        var who = current(request);
        if (!who.host()) throw new ApiException(403, "LOGIN_REQUIRED");
        return who;
    }
}
