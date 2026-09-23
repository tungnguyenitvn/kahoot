package dev.sample.quiz.identity;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
public record HostPrincipal(String id,String username,String displayName,String password) implements UserDetails {
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_HOST")); }
}
