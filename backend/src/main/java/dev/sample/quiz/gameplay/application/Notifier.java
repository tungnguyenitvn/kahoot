package dev.sample.quiz.gameplay.application;
/** Port to realtime delivery: marks a room's connections dirty; nothing is awaited and no receipt depends on it (LIVE-05). */
public interface Notifier {
    void broadcast(String room);
}
