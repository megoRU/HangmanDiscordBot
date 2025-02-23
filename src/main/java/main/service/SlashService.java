package main.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
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
                    .addChoice("English", "EN")
                    .addChoice("Русский", "RU")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "язык-игры")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка игры")
            );

            language.add(new OptionData(STRING, "bot", "Setting the bot language")
                    .addChoice("English", "EN")
                    .addChoice("Русский", "RU")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "бот")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка бота")
            );

            List<OptionData> multi = new ArrayList<>();
            multi.add(new OptionData(USER, "user", "@Mention player to play with him")
                    .setRequired(true)
                    .setName("user")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "пользователь")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "@Упомяните игрока, чтобы поиграть с ним")
            );

            List<OptionData> multiple = new ArrayList<>();
            multiple.add(new OptionData(STRING, "users", "@Mention the players separated by a space to play with them")
                    .setRequired(true)
                    .setName("users")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "пользователи")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "@Упомяните игроков через пробел, чтобы поиграть с ними")
            );

            Command.Choice choiceAny = new Command.Choice("Any", "any").setNameLocalization(DiscordLocale.RUSSIAN, "Любая");
            Command.Choice choiceColors = new Command.Choice("Colors", "COLORS").setNameLocalization(DiscordLocale.RUSSIAN, "Цвета");
            Command.Choice choiceFruits = new Command.Choice("Fruits", "FRUITS").setNameLocalization(DiscordLocale.RUSSIAN, "Фрукты");
            Command.Choice choiceFlowers = new Command.Choice("Flowers", "FLOWERS").setNameLocalization(DiscordLocale.RUSSIAN, "Цветы");

            List<OptionData> category = new ArrayList<>();
            category.add(new OptionData(STRING, "select", "Select a category")
                    .addChoices(choiceAny, choiceColors, choiceFruits, choiceFlowers)
                    .setRequired(true)
                    .setName("category")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "выбрать")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Выбрать категорию")
            );

            /*
             * Команды
             */

            CommandData multipleCommand = Commands.slash("multiple", "Play Hangman with your friends")
                    .addOptions(multiple)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Играйте в Hangman с друзьями");

            CommandData competitiveCommand = Commands.slash("competitive", "Compete with other players")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Соревноваться с другими игроками");

            CommandData languageCommand = Commands.slash("language", "Setting language")
                    .addOptions(language)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка");

            CommandData playCommand = Commands.slash("play", "Play Hangman")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Играть в Виселицу");

            CommandData chatgptCommand = Commands.slash("chatgpt", "Play Hangman vs bot")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Играть в Виселицу против бота");

            CommandData quitCommand = Commands.slash("quit", "Leave from singleplayer/multiplayer game")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Выход из одиночной/многопользовательской игры");

            CommandData helpCommand = Commands.slash("help", "Bot commands")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Команды бота");

            CommandData leadboardCommand = Commands.slash("leadboard", "Leadboard")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Доска почёта");

            CommandData statisticsCommand = Commands.slash("statistics", "Get your statistics")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Получить свою статистику");

            CommandData botStatisticsCommand = Commands.slash("bot-statistics", "Find out the statistics of all the bot games")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Узнайте статистику всех игр бота");

            CommandData deleteCommand = Commands.slash("delete", "Deleting your data")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удаление ваших данных");

            CommandData multiCommand = Commands.slash("multi", "Play Hangman with another player")
                    .setContexts(InteractionContextType.GUILD)
                    .addOptions(multi)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Играйте в Hangman с другим игроком");

            //Context Menu
            CommandData playContextCommand = Commands.context(Command.Type.USER, "Play multi")
                    .setName("multi")
                    .setContexts(InteractionContextType.GUILD)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "Играть вместе");

            CommandData categoryCommand = Commands.slash("category", "Set a category for words")
                    .addOptions(category)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установите категорию для слов");

            commands.addCommands(
                            multipleCommand, competitiveCommand, languageCommand,
                            playCommand, chatgptCommand, quitCommand, helpCommand,
                            leadboardCommand, statisticsCommand, botStatisticsCommand, deleteCommand,
                            multiCommand, categoryCommand, playContextCommand)
                    .queue();
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }
}