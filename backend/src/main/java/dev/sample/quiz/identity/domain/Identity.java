package dev.sample.quiz.identity.domain;
import java.util.UUID;
/** Who is calling: a host account (U:<account id>) or a browser guest (G:<guest id>). A guest id is a credential and never appears in public room state. */
public record Identity(String id, String name, boolean host) {
    public static Identity ofHost(Account account) { return new Identity("U:" + account.id(), account.displayName(), true); }
    public static Identity ofGuest(String guestId) { return new Identity("G:" + guestId, "Guest", false); }
    /** The account behind a host identity. Callers go through Identities.host first; a guest here is a programming error, not a request error. */
    public UUID userId() {
        if (!host) throw new IllegalStateException("guest identity has no account");
        return UUID.fromString(id.substring(2));
    }
}
