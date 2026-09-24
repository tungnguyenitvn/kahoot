package dev.sample.quiz.identity.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import dev.sample.quiz.identity.domain.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

class AccountsTest {
    /** In-memory port and a transparent encoder: the facade is tested without Spring or a database. */
    static final class MemoryAccounts implements AccountRepository {
        final Map<String, Account> byUsername = new HashMap<>();
        public Optional<Account> findByUsername(String username) { return Optional.ofNullable(byUsername.get(username)); }
        public void insertIfAbsent(Account account) { byUsername.putIfAbsent(account.username(), account); }
    }
    static final PasswordEncoder ENCODER = new PasswordEncoder() {
        public String encode(CharSequence raw) { return "hash(" + raw + ")"; }
        public boolean matches(CharSequence raw, String encoded) { return encode(raw).equals(encoded); }
    };

    @Test
    @DisplayName("register is idempotent: a second call keeps the stored account and its password, and the raw password is never stored")
    void registerIsIdempotentAndHashes() {
        var accounts = new Accounts(new MemoryAccounts(), ENCODER);
        var first = accounts.register("host@example.test", "Demo Host", "first-secret");
        var second = accounts.register("host@example.test", "Renamed", "second-secret");

        assertEquals(first.id(), second.id());
        assertEquals("Demo Host", second.displayName());
        assertEquals("hash(first-secret)", second.passwordHash());
        assertFalse(second.toString().contains("secret"));
        assertEquals(Optional.of(first), accounts.find("host@example.test"));
        assertTrue(accounts.find("nobody@example.test").isEmpty());
    }
}
