# Этап сборки приложения
FROM maven:3.9.11-amazoncorretto-25-al2023 AS builder
WORKDIR /app
COPY . .
RUN ["mvn", "install", "-Dmaven.test.skip=true"]

# Этап запуска приложения
FROM amazoncorretto:25-jdk
ENV LANG=C.UTF-8
WORKDIR /app
COPY --from=builder /app/target/HangmanDiscordBot-0.0.1-SNAPSHOT.jar .
ENTRYPOINT ["java", "-jar", "./HangmanDiscordBot-0.0.1-SNAPSHOT.jar"]