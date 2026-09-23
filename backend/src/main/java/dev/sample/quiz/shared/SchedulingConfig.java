package dev.sample.quiz.shared;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
@Configuration
public class SchedulingConfig {
    @Bean ThreadPoolTaskScheduler taskScheduler(){var scheduler=new ThreadPoolTaskScheduler();scheduler.setPoolSize(3);scheduler.setThreadNamePrefix("quiz-jobs-");return scheduler;}
}
