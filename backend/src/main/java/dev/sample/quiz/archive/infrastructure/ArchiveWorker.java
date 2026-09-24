package dev.sample.quiz.archive.infrastructure;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import dev.sample.quiz.archive.application.Archiver;
/** Runs one archive pass after a fixed delay following the previous pass (value in the limits table). */
@Component
public class ArchiveWorker {
    private final Archiver archiver;
    public ArchiveWorker(Archiver archiver) { this.archiver = archiver; }
    @Scheduled(fixedDelay = 1000) public void drain() { archiver.drain(); }
}
