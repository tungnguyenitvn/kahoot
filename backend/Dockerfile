# Development and test image. The Gradle wrapper is committed in backend/, so the container only needs a JDK;
# the wrapper downloads its distribution into /root/.gradle, which compose mounts from ./.cache/gradle.
FROM eclipse-temurin:24-jdk AS development
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY docker-entrypoint.sh /usr/local/bin/quiz-backend
RUN chmod +x /usr/local/bin/quiz-backend
WORKDIR /app
ENTRYPOINT ["quiz-backend"]
CMD ["bootRun", "--no-daemon"]
