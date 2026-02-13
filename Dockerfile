# Stage 1: build
FROM maven:3.9.11-amazoncorretto-25-al2023 AS builder
WORKDIR /app

# Кэш зависимостей
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Исходники
COPY src ./src
RUN mvn -B package -DskipTests

# Stage 2: runtime
FROM amazoncorretto:25-jdk
ENV LANG=C.UTF-8
WORKDIR /app

COPY --from=builder /app/target/HangmanDiscordBot-0.0.1-SNAPSHOT.jar ./HangmanDiscordBot.jar

ENTRYPOINT ["java", "-jar", "HangmanDiscordBot.jar"]