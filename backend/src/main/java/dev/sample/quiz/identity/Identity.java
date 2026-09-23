package dev.sample.quiz.identity;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import dev.sample.quiz.shared.ApiException;
public record Identity(String id,String name,boolean host) {
    public static Identity current(HttpServletRequest request) {
        Authentication a=SecurityContextHolder.getContext().getAuthentication();
        if(a!=null && a.getPrincipal() instanceof HostPrincipal h) return new Identity("U:"+h.id(),h.displayName(),true);
        var session=request.getSession(false);
        if(session==null || session.getAttribute("guestId")==null) throw new ApiException(401,"SESSION_REQUIRED");
        return new Identity("G:"+session.getAttribute("guestId"),"Guest",false);
    }
    public static Identity ensure(HttpServletRequest request) {
        var session=request.getSession();
        synchronized(session) { if(session.getAttribute("guestId")==null) session.setAttribute("guestId",UUID.randomUUID().toString()); }
        return current(request);
    }
    public UUID userId() { if(!host) throw new ApiException(403,"LOGIN_REQUIRED"); return UUID.fromString(id.substring(2)); }
}
