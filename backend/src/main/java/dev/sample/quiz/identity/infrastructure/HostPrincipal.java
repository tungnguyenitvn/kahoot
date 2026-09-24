package dev.sample.quiz.identity.infrastructure;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import dev.sample.quiz.identity.domain.Account;
/** Spring Security's view of a host account; stored in the HTTP session by the security context. */
public record HostPrincipal(Account account) implements UserDetails {
    public String getUsername() { return account.username(); }
    public String getPassword() { return account.passwordHash(); }
    public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_HOST")); }
}
