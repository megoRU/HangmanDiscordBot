package main.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

@Service
public class SlashService {

    private static final Logger LOGGER = Logger.getLogger(SlashService.class.getName());

    public void updateSlash(JDA jda) {
        try {
            CommandListUpdateAction commands = jda.updateCommands();
            List<OptionData> language = new ArrayList<>();

            language.add(new OptionData(STRING, "game", "Setting the Game language")
                    .addChoice("english", "EN")
                    .addChoice("russian", "RU")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка игры")
            );

            language.add(new OptionData(STRING, "bot", "Setting the bot language")
                    .addChoice("english", "EN")
                    .addChoice("russian", "RU")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка бота")
            );

            List<OptionData> multi = new ArrayList<>();
            multi.add(new OptionData(USER, "user", "@Mention player to play with him")
                    .setRequired(true)
                    .setName("user")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "@Упомяните игрока, чтобы поиграть с ним")
            );

            List<OptionData> multiple = new ArrayList<>();
            multiple.add(new OptionData(STRING, "users", "@Mention the players separated by a space to play with them")
                    .setRequired(true)
                    .setName("users")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "@Упомяните игроков через пробел, чтобы поиграть с ними")
            );

            List<OptionData> category = new ArrayList<>();
            category.add(new OptionData(STRING, "category", "Select a category")
                    .addChoice("any", "any")
                    .addChoice("colors", "COLORS")
                    .addChoice("fruits", "FRUITS")
                    .addChoice("flowers", "FLOWERS")
                    .setRequired(true)
                    .setName("category")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Выбрать категорию")
            );

            commands.addCommands(Commands.slash("multiple", "Play Hangman with your friends")
                    .addOptions(multiple)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Играйте в Hangman с друзьями"));

            commands.addCommands(Commands.slash("competitive", "Compete with other players")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Соревноваться с другими игроками"));

            commands.addCommands(Commands.slash("language", "Setting language")
                    .addOptions(language)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка"));

            commands.addCommands(Commands.slash("hg", "Start the game (deprecated: use /play)")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Начать игру (Устарело: используй /play)"));

            commands.addCommands(Commands.slash("play", "Play Hangman")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Играть в Виселицу"));

            commands.addCommands(Commands.slash("stop", "Stop the game (deprecated: use /quit)")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Остановить игру (Устарело: используй /quit)"));

            commands.addCommands(Commands.slash("quit", "Leave from singleplayer/multiplayer game")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Выход из одиночной/многопользовательской игры"));

            commands.addCommands(Commands.slash("help", "Bot commands")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Команды бота"));

            commands.addCommands(Commands.slash("leadboard", "Leadboard")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Доска почёта"));

            commands.addCommands(Commands.slash("statistics", "Get your statistics")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Получить свою статистику"));

            commands.addCommands(Commands.slash("mystats", "Find out the number of your wins and losses")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Узнайте количество ваших побед и поражений"));

            commands.addCommands(Commands.slash("allstats", "Find out the statistics of all the bot games")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Узнайте статистику всех игр бота"));

            commands.addCommands(Commands.slash("delete", "Deleting your data")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удаление ваших данных"));

            commands.addCommands(Commands.slash("multi", "Play Hangman with another player")
                    .setGuildOnly(true)
                    .addOptions(multi)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Играйте в Hangman с другим игроком"));

            //Context Menu
            commands.addCommands(Commands.context(Command.Type.USER, "Play multi")
                    .setName("multi")
                    .setGuildOnly(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "Играть вместе")
            );

            commands.addCommands(Commands.slash("category", "Set a category for words")
                    .addOptions(category)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установите категорию для слов"));

            commands.queue();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }
}