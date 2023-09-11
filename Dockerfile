FROM maven:3.9.4-eclipse-temurin-20 as build

WORKDIR /app

COPY . .

RUN ["mvn", "install"]

ENTRYPOINT ["java", "-jar", "./target/hangman.discord.bot-0.0.1-SNAPSHOT.jar"]