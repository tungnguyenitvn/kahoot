package dev.sample.quiz.identity.infrastructure;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import dev.sample.quiz.identity.application.Principals;
import dev.sample.quiz.identity.domain.Account;
/** Reads the account of the signed-in host from the Spring Security context of the current request. */
@Component
public class SecurityPrincipals implements Principals {
    public Optional<Account> authenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof HostPrincipal host ? Optional.of(host.account()) : Optional.empty();
    }
}
