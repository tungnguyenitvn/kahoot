package dev.sample.quiz.identity.application;
import java.util.Optional;
import dev.sample.quiz.identity.domain.Account;
/** Port to the account store; identity is the only writer of app_user. */
public interface AccountRepository {
    Optional<Account> findByUsername(String username);
    void insertIfAbsent(Account account);
}
