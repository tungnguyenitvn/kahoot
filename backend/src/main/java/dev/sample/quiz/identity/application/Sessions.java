package dev.sample.quiz.identity.application;
/** Port other modules use to ask whether an HTTP session still exists, without touching the session store. */
public interface Sessions {
    boolean live(String sessionId);
}
