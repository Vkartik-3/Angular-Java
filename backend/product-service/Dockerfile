FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system --gid 1001 app && useradd --system --uid 1001 --gid app --no-create-home app
COPY --chown=app:app target/*.jar app.jar
USER 1001
EXPOSE 8084
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
