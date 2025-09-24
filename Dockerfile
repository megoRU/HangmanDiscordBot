# Этап сборки приложения
FROM maven:3.9.9-amazoncorretto-21-al2023 AS builder
WORKDIR /app
COPY . .
RUN ["mvn", "install", "-Dmaven.test.skip=true"]

# Этап запуска приложения
FROM openjdk:25
ENV LANG=C.UTF-8
WORKDIR /app
COPY --from=builder /app/target/HangmanDiscordBot-0.0.1-SNAPSHOT.jar .
ENTRYPOINT ["java", "-jar", "./HangmanDiscordBot-0.0.1-SNAPSHOT.jar"]