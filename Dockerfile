FROM maven:3.9.6-amazoncorretto-21-debian

WORKDIR /app

COPY . .

RUN ["mvn", "install"]

ENTRYPOINT ["java", "-jar", "./target/hangman.discord.bot-0.0.1-SNAPSHOT.jar"]