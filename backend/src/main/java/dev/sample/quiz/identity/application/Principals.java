package dev.sample.quiz.identity.application;
import java.util.Optional;
import dev.sample.quiz.identity.domain.Account;
/** Port to the authentication mechanism: the account behind the current request, if a host is signed in. */
public interface Principals {
    Optional<Account> authenticated();
}
