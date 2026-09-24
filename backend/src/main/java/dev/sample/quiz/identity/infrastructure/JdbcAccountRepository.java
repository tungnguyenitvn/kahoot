package dev.sample.quiz.identity.infrastructure;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import dev.sample.quiz.identity.application.AccountRepository;
import dev.sample.quiz.identity.domain.Account;
/** PostgreSQL adapter for app_user. */
@Repository
public class JdbcAccountRepository implements AccountRepository {
    private final JdbcTemplate jdbc;
    public JdbcAccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Optional<Account> findByUsername(String username) {
        return jdbc.query("select id,username,display_name,password_hash from app_user where username=?",
            (rs, n) -> new Account(UUID.fromString(rs.getString("id")), rs.getString("username"), rs.getString("display_name"), rs.getString("password_hash")), username).stream().findFirst();
    }
    public void insertIfAbsent(Account account) {
        jdbc.update("insert into app_user(id,username,display_name,password_hash) values(?,?,?,?) on conflict(username) do nothing",
            account.id(), account.username(), account.displayName(), account.passwordHash());
    }
}
