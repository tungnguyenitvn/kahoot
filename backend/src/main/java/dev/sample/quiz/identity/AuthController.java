package dev.sample.quiz.identity;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("csrf") Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken()); }
    @GetMapping("me") Identity me(HttpServletRequest request) { return Identity.ensure(request); }
}
