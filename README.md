# HangmanDiscordBot
[![Java CI](https://github.com/megoRU/HangmanDiscordBot/actions/workflows/ci_cd.yml/badge.svg)](https://github.com/megoRU/HangmanDiscordBot/actions/workflows/ci_cd.yml)
[![Discord](https://img.shields.io/discord/779317239722672128?label=Discord)](https://discord.gg/UrWG3R683d)
[![Add](https://img.shields.io/badge/invite-Hangman-blue?logo=discord)](https://top.gg/bot/845974873682608129/invite/)
[![top](https://img.shields.io/badge/TOP.GG-pink?logo=discord)](https://top.gg/bot/845974873682608129) 
[![Discord Bots](https://top.gg/api/widget/servers/845974873682608129.svg)](https://top.gg/bot/845974873682608129)

## LICENSE

<a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-nc-sa/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/4.0/">Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License</a>.

## Add bot to your guild
[![Discord Bots](https://top.gg/api/widget/845974873682608129.svg)](https://top.gg/bot/845974873682608129)

## Technologies used

- Java 11
- MySQL
- Docker
- Maven

## Running on your server

1. Clone this repository to your PC
2. Fill in the fields with your data in the class `Config.java `* `(See in point 3 in PS)`
3. Create on [hub.docker.com](https://hub.docker.com) private repository
4. Open CMD in the project folder or use the terminal in IntelliJ IDEA
5. Docker must be running on the PC
6. Enter the command into the terminal: `docker build -t megoru/hangman .`
7. After completing step 6: `docker push megoru/hangman`
8. On your Linux server must be installed: Docker & Docker Compose
9. Upload the docker-compose file to the Linux server and fill it with your data. `(See in point 4 in P. S.)`
10. Log in to your Linux server account: `docker login`
11. On the Linux server, enter the command: `docker-compose pull`
12. Then this one: `docker-compose up -d`

P. S. To launch the project
1. Where **megoru** - this is the login [hub.docker.com](https://hub.docker.com)
2. Where **hangman** - the name of the private repository on [hub.docker.com](https://hub.docker.com)
3. HANGMAN_NAME = ""; // login DB <br>
   BOT_ID = ""; // The bot ID can be obtained here: [Discord developers portal](https://discord.com/developers/applications)
4. PROD_TOKEN // The bot token can be obtained here: [Discord developers portal](https://discord.com/developers/applications) <br>
   TOP_GG_API_TOKEN // The token can be obtained in the bot settings in the menu webhooks: [top.gg](https://top.gg)

## Copyright Notice

1. The bot is made using the library: [JDA](https://github.com/DV8FromTheWorld/JDA)
2. Used the library and slightly rewritten: [Statcord wrapper](https://github.com/pvhil/unofficial-statcord-wrapper)

## Privacy Policy

Here you can read more about what we store and how we store it. [Privacy Policy](https://github.com/megoRU/HangmanDiscordBot/blob/main/.github/privacy.md)
