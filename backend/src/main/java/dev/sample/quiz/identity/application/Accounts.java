package dev.sample.quiz.identity.application;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import dev.sample.quiz.identity.domain.Account;
/** Facade for account provisioning; the only way another module creates an account. */
@Service
public class Accounts {
    private final AccountRepository accounts; private final PasswordEncoder encoder;
    public Accounts(AccountRepository accounts, PasswordEncoder encoder) { this.accounts = accounts; this.encoder = encoder; }
    /** Idempotent: an existing username is returned as stored and its password is left alone. The id derives from the username so a re-seed finds the same account. */
    public Account register(String username, String displayName, String rawPassword) {
        UUID id = UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8));
        accounts.insertIfAbsent(new Account(id, username, displayName, encoder.encode(rawPassword)));
        return accounts.findByUsername(username).orElseThrow(() -> new IllegalStateException("account not stored: " + username));
    }
    public Optional<Account> find(String username) { return accounts.findByUsername(username); }
}
