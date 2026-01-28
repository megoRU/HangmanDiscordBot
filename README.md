# HangmanDiscordBot

[![Java CI](https://github.com/megoRU/HangmanDiscordBot/actions/workflows/ci_cd.yml/badge.svg)](https://github.com/megoRU/HangmanDiscordBot/actions/workflows/ci_cd.yml)
[![Docker Pulls](https://badgen.net/docker/pulls/megoru/hangman?icon=docker\&label=pulls)](https://hub.docker.com/r/megoru/hangman/)
[![Docker Image Size](https://badgen.net/docker/size/megoru/hangman?icon=docker\&label=image%20size)](https://hub.docker.com/r/megoru/hangman)

A Discord bot that brings the classic Hangman game to your server.

---

## ✨ Features

* Multiplayer Hangman game in Discord
* Persistent game state with MariaDB
* Dockerized for easy deployment
* CI/CD via GitHub Actions

---

## 🚀 Quick Start

### Add the bot to your server

[Click here to invite](https://discord.com/oauth2/authorize?client_id=845974873682608129)

### Run with Docker

1. Place `docker-compose.yml` at the root of your VPS (`/root` or other).
2. Fill in your configuration values (DB credentials, tokens, etc.).
3. Import `DiscordBotHangman.sql` into your MariaDB instance.
4. Start the container:

```bash
docker-compose up -d
```

5. Update the bot (pull latest image):

```bash
docker-compose pull && docker-compose up -d
```

6. Stop the bot:

```bash
docker-compose stop
```

---

## 🛠 Tech Stack

* Java 20
* Spring Boot
* Hibernate
* MariaDB
* Docker
* Maven
* [JDA](https://github.com/DV8FromTheWorld/JDA)

---

## 📄 License

This project is licensed under the [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.en.html).

---

## 🔒 Privacy

Details on data storage and usage are described in the [Privacy Policy](https://github.com/megoRU/HangmanDiscordBot/blob/main/.github/privacy.md).
