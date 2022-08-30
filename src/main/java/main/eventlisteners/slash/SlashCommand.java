package main.eventlisteners.slash;

import lombok.RequiredArgsConstructor;
import main.config.BotStartConfig;
import main.enums.Buttons;
import main.enums.Statistic;
import main.eventlisteners.DeleteAllMyData;
import main.eventlisteners.buildClass.Help;
import main.eventlisteners.buildClass.MessageStats;
import main.hangman.Hangman;
import main.hangman.HangmanRegistry;
import main.hangman.impl.HangmanHelper;
import main.jsonparser.JSONParsers;
import main.model.entity.GameLanguage;
import main.model.entity.GameMode;
import main.model.entity.Language;
import main.model.repository.*;
import main.statistic.CreatorGraph;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.logging.Logger;

@RequiredArgsConstructor
@Service
public class SlashCommand extends ListenerAdapter {

    //Language
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);

    //REPO
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;
    private final GameLanguageRepository gameLanguageRepository;
    private final LanguageRepository languageRepository;
    private final GameModeRepository gameModeRepository;

    private final static Logger LOGGER = Logger.getLogger(SlashCommand.class.getName());

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        try {
            if (event.getUser().isBot()) return;

            if (event.getGuild() != null
                    && !event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_SEND)
                    && !event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_MANAGE)
                    && !event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.VIEW_CHANNEL)) {
                return;
            }

            long userIdLong = event.getUser().getIdLong();

            LOGGER.info("\nSlash Command name: " + event.getName());

            if (event.getName().equals("hg")) {
                event.getChannel().sendTyping().queue();
                //Проверяем установлен ли язык. Если нет - то возвращаем в чат ошибку
                if (BotStartConfig.getMapGameLanguages().get(userIdLong) == null
                        || BotStartConfig.getMapGameMode().get(userIdLong) == null) {

                    String hangmanListenerNeedSetLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userIdLong);

                    EmbedBuilder needSetLanguage = new EmbedBuilder();

                    needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    needSetLanguage.setColor(0x00FF00);
                    needSetLanguage.setDescription(hangmanListenerNeedSetLanguage);

                    event.replyEmbeds(needSetLanguage.build())
                            .addActionRow(
                                    Button.secondary(Buttons.BUTTON_RUS.name(), "Кириллица")
                                            .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")),
                                    Button.secondary(Buttons.BUTTON_ENG.name(), "Latin")
                                            .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")))
                            .addActionRow(
                                    Button.danger(Buttons.BUTTON_SELECT_MENU.name(), "Guild/DM: SelectMenu"),
                                    Button.success(Buttons.BUTTON_DM.name(), "(Recommended) Only DM: One letter in chat"))
                            .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play"))
                            .queue();
                } else if (event.isFromGuild() && BotStartConfig.getMapGameMode().get(userIdLong).equals("direct-message")) {
                    try {
                        event.reply("Got your message").setEphemeral(true).queue();
                        PrivateChannel privateChannel = event.getUser().openPrivateChannel().submit().get();

                        Hangman hangman = new Hangman(userIdLong,
                                null,
                                privateChannel.getIdLong(),
                                hangmanGameRepository,
                                gamesRepository,
                                playerRepository);

                        HangmanRegistry.getInstance().setHangman(userIdLong, hangman);

                        hangman.startGame(privateChannel, event.getUser().getAvatarUrl(), event.getUser().getName());
                    } catch (Exception e) {
                        if (e.getMessage().equals("50007: Cannot send messages to this user")) {
                            event.reply("I couldn't send a message to you in DM").queue();
                            HangmanRegistry.getInstance().removeHangman(userIdLong);
                        } else {
                            e.printStackTrace();
                        }
                    }
                    //Проверяем если игрок уже играет. То присылаем в чат уведомление
                } else if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    String hangmanListenerYouPlay = jsonParsers.getLocale("Hangman_Listener_You_Play", userIdLong);

                    EmbedBuilder youPlay = new EmbedBuilder();

                    youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    youPlay.setColor(0x00FF00);

                    youPlay.setDescription(hangmanListenerYouPlay);

                    event.replyEmbeds(youPlay.build())
                            .addActionRow(Button.danger(Buttons.BUTTON_STOP.name(), "Stop game"))
                            .queue();
                    //Если всё хорошо, создаем игру
                } else {
                    Hangman hangman;
                    if (event.getGuild() != null) {
                        HangmanRegistry.getInstance().setHangman(userIdLong,
                                hangman = new Hangman(userIdLong,
                                        event.getGuild().getIdLong(),
                                        event.getChannel().getIdLong(),
                                        hangmanGameRepository,
                                        gamesRepository,
                                        playerRepository));
                    } else {
                        HangmanRegistry.getInstance().setHangman(userIdLong,
                                hangman = new Hangman(userIdLong,
                                        null,
                                        event.getChannel().getIdLong(),
                                        hangmanGameRepository,
                                        gamesRepository,
                                        playerRepository));
                    }
                    event.reply("Got your message").setEphemeral(true).queue();
                    hangman.startGame(event.getChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
                }
                return;
            }

            if (event.getName().equals("stop")) {
                //Проверяем играет ли сейчас игрок. Если да удаляем игру.
                if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    String hangmanEngGame = jsonParsers.getLocale("Hangman_Eng_game", userIdLong);
                    String hangmanEngGame1 = jsonGameParsers.getLocale("Hangman_Eng_game", userIdLong);

                    event.reply(hangmanEngGame)
                            .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                            .queue();

                    var embedBuilder = HangmanRegistry.getInstance().getActiveHangman().get(userIdLong)
                            .embedBuilder(Color.GREEN,
                                    hangmanEngGame1,
                                    false,
                                    false,
                                    null
                            );

                    HangmanHelper.editMessage(embedBuilder, userIdLong);
                    HangmanRegistry.getInstance().getActiveHangman().remove(userIdLong);
                    hangmanGameRepository.deleteActiveGame(userIdLong);
                    //Если игрок не играет, а хочет завершить игру, то нужно ему это прислать уведомление, что он сейчас не играет
                } else {
                    String hangmanYouAreNotPlay = jsonParsers.getLocale("Hangman_You_Are_Not_Play", userIdLong);

                    event.reply(hangmanYouAreNotPlay)
                            .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                            .queue();
                }
                return;
            }
            //Если игрок сейчас играет сменить язык не даст
            if (event.getName().equals("language") && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", userIdLong);

                EmbedBuilder whenPlay = new EmbedBuilder();

                whenPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                whenPlay.setColor(0x00FF00);
                whenPlay.setDescription(reactionsButtonWhenPlay);

                event.replyEmbeds(whenPlay.build())
                        .addActionRow(Button.danger(Buttons.BUTTON_STOP.name(), "Stop game"))
                        .queue();
                return;
            }

            //0 - game | 1 - bot
            if (event.getName().equals("language") && event.getOptions().size() == 2) {
                String opOne = event.getOptions().get(0).getAsString();
                String opTwo = event.getOptions().get(1).getAsString();

                BotStartConfig.getMapGameLanguages().put(userIdLong, opOne);
                BotStartConfig.getMapLanguages().put(userIdLong, opTwo);

                String slashLanguage = String.format(jsonParsers.getLocale("slash_language", userIdLong), opOne, opTwo);

                event.reply(slashLanguage)
                        .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                        .queue();
                GameLanguage gameLanguage = new GameLanguage();
                gameLanguage.setUserIdLong(userIdLong);
                gameLanguage.setLanguage(opOne);
                gameLanguageRepository.save(gameLanguage);

                Language language = new Language();
                language.setUserIdLong(userIdLong);
                language.setLanguage(opTwo);
                languageRepository.save(language);
            }

            if (event.getName().equals("set-game")) {
                String mode = event.getOption("mode", OptionMapping::getAsString);
                BotStartConfig.getMapGameMode().put(userIdLong, mode);
                String gameMode1 = String.format(jsonParsers.getLocale("game_mode", userIdLong), mode);

                event.reply(gameMode1)
                        .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                        .setEphemeral(true)
                        .queue();

                GameMode gameMode = new GameMode();
                gameMode.setUserIdLong(userIdLong);
                gameMode.setMode(mode);
                gameModeRepository.save(gameMode);
                return;
            }

            if (event.getName().equals("word")) {
                if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    String gotYourWord = jsonParsers.getLocale("got_your_word", userIdLong);

                    String word = event.getOption("guess", OptionMapping::getAsString);

                    if (word != null) {
                        word.toLowerCase();
                    }

                    event.reply(gotYourWord)
                            .setEphemeral(true)
                            .queue();

                    HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).fullWord(word);
                } else {
                    String hangmanYouAreNotPlay = jsonParsers.getLocale("Hangman_You_Are_Not_Play", userIdLong);

                    event.reply(hangmanYouAreNotPlay)
                            .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                            .queue();
                }
                return;
            }

            if (event.getName().equals("delete")) {
                DeleteAllMyData deleteAllMyData = new DeleteAllMyData(
                        gamesRepository,
                        languageRepository,
                        gameLanguageRepository,
                        gameModeRepository);
                deleteAllMyData.buildMessage(event, event.getUser());
                return;
            }

            if (event.getName().equals("help")) {
                Help help = new Help();
                help.send(
                        null,
                        event,
                        event.getUser().getAvatarUrl(),
                        userIdLong,
                        event.getUser().getName());
                return;
            }

            if (event.getName().equals("stats")) {
                MessageStats messageStats = new MessageStats(gamesRepository);
                messageStats.sendStats(
                        null,
                        event,
                        event.getUser().getAvatarUrl(),
                        userIdLong,
                        event.getUser().getName());

                return;
            }

            if (event.getName().equals("allstats")) {
                CreatorGraph creatorGraph = new CreatorGraph(
                        gamesRepository,
                        event.getChannel().getId(),
                        event.getUser().getId(),
                        event.getUser().getName(),
                        event.getUser().getAvatarUrl(),
                        event);
                creatorGraph.createGraph(Statistic.GLOBAL);
                return;
            }

            if (event.getName().equals("mystats")) {
                CreatorGraph creatorGraph = new CreatorGraph(
                        gamesRepository,
                        event.getChannel().getId(),
                        event.getUser().getId(),
                        event.getUser().getName(),
                        event.getUser().getAvatarUrl(),
                        event);
                creatorGraph.createGraph(Statistic.MY);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unknown interaction");
        }
    }
}