package dev.sample.quiz.identity.api;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import dev.sample.quiz.identity.application.Identities;
import dev.sample.quiz.identity.domain.Identity;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final Identities identities;
    public AuthController(Identities identities) { this.identities = identities; }
    @GetMapping("csrf") Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken()); }
    @GetMapping("me") Identity me(HttpServletRequest request) { return identities.ensure(request); }
}
