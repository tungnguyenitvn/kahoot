FROM gradle:8.14.3-jdk21 AS distribution
FROM eclipse-temurin:24-jdk AS development
COPY --from=distribution /opt/gradle /opt/gradle
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
ENV PATH="/opt/gradle/bin:${PATH}"
WORKDIR /opt/wrapper
RUN touch settings.gradle && gradle --no-daemon wrapper --gradle-version 8.14.3 --distribution-type bin
COPY docker-entrypoint.sh /usr/local/bin/quiz-backend
RUN chmod +x /usr/local/bin/quiz-backend
WORKDIR /app
ENTRYPOINT ["quiz-backend"]
CMD ["bootRun", "--no-daemon"]
