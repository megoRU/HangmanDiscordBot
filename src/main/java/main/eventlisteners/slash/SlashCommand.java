package main.eventlisteners.slash;

import lombok.RequiredArgsConstructor;
import main.config.BotStartConfig;
import main.enums.Buttons;
import main.enums.Statistic;
import main.eventlisteners.CheckPermissions;
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
import net.dv8tion.jda.api.entities.Emoji;
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
            if (event.getGuild() != null && CheckPermissions.isHasPermissionsWriteAndEmbedLinks(event.getTextChannel()))
                return;

            LOGGER.info("\nSlash Command name: " + event.getName());

            if (event.getName().equals("hg")) {
                event.getChannel().sendTyping().queue();
                //Проверяем установлен ли язык. Если нет - то возвращаем в чат ошибку
                if (BotStartConfig.getMapGameLanguages().get(event.getUser().getId()) == null
                        || BotStartConfig.getMapGameMode().get(event.getUser().getId()) == null) {
                    EmbedBuilder needSetLanguage = new EmbedBuilder();

                    needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    needSetLanguage.setColor(0x00FF00);
                    needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getId()));

                    event.replyEmbeds(needSetLanguage.build())
                            .addActionRow(
                                    Button.secondary(Buttons.BUTTON_RUS.name(), "Кириллица")
                                            .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")),
                                    Button.secondary(Buttons.BUTTON_ENG.name(), "Latin")
                                            .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")))
                            .addActionRow(
                                    Button.secondary(Buttons.BUTTON_SELECT_MENU.name(), "Guild/DM: SelectMenu"),
                                    Button.secondary(Buttons.BUTTON_DM.name(), "Only DM: One letter in chat"))
                            .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play"))
                            .queue();
                } else if (event.isFromGuild() && BotStartConfig.getMapGameMode().get(event.getUser().getId()).equals("direct-message")) {
                    event.reply(jsonParsers.getLocale("game_only_dm", event.getUser().getId()))
                            .setEphemeral(true)
                            .queue();
                    //Проверяем если игрок уже играет. То присылаем в чат уведомление
                } else if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {

                    EmbedBuilder youPlay = new EmbedBuilder();

                    youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    youPlay.setColor(0x00FF00);

                    youPlay.setDescription(jsonParsers.getLocale("Hangman_Listener_You_Play", event.getUser().getId()));


                    event.replyEmbeds(youPlay.build())
                            .addActionRow(Button.danger(Buttons.BUTTON_STOP.name(), "Stop game"))
                            .queue();
                    //Если всё хорошо, создаем игру
                } else {
                    if (event.getGuild() != null) {
                        HangmanRegistry.getInstance().setHangman(event.getUser().getIdLong(),
                                new Hangman(event.getUser().getId(),
                                        event.getGuild().getId(),
                                        event.getChannel().getIdLong(),
                                        hangmanGameRepository,
                                        gamesRepository,
                                        playerRepository));
                    } else {
                        HangmanRegistry.getInstance().setHangman(event.getUser().getIdLong(),
                                new Hangman(event.getUser().getId(),
                                        null,
                                        event.getChannel().getIdLong(),
                                        hangmanGameRepository,
                                        gamesRepository,
                                        playerRepository));
                    }
                    HangmanRegistry.getInstance().getActiveHangman().get(event.getUser().getIdLong()).startGame(event);
                }
                return;
            }

            if (event.getName().equals("stop")) {
                //Проверяем играет ли сейчас игрок. Если да удаляем игру.
                if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {

                    event.reply(jsonParsers.getLocale("Hangman_Eng_game", event.getUser().getId()))
                            .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                            .queue();

                    var embedBuilder = HangmanRegistry.getInstance().getActiveHangman().get(event.getUser().getIdLong())
                            .embedBuilder(Color.GREEN,
                                    "<@" + event.getUser().getIdLong() + ">",
                                    jsonGameParsers.getLocale("Hangman_Eng_game", event.getUser().getId()),
                                    false,
                                    false,
                                    null
                            );

                    HangmanHelper.editMessage(embedBuilder, event.getUser().getIdLong());
                    HangmanRegistry.getInstance().getActiveHangman().remove(event.getUser().getIdLong());
                    hangmanGameRepository.deleteActiveGame(event.getUser().getIdLong());
                    //Если игрок не играет, а хочет завершить игру, то нужно ему это прислать уведомление, что он сейчас не играет
                } else {
                    event.reply(jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getUser().getId()))
                            .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                            .queue();
                }
                return;
            }
            //Если игрок сейчас играет сменить язык не даст
            if (event.getName().equals("language") && HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
                EmbedBuilder whenPlay = new EmbedBuilder();

                whenPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                whenPlay.setColor(0x00FF00);
                whenPlay.setDescription(jsonParsers.getLocale("ReactionsButton_When_Play", event.getUser().getId()));

                event.replyEmbeds(whenPlay.build())
                        .addActionRow(Button.danger(Buttons.BUTTON_STOP.name(), "Stop game"))
                        .queue();
                return;
            }

            //0 - game | 1 - bot
            if (event.getName().equals("language") && event.getOptions().size() == 2) {
                BotStartConfig.getMapGameLanguages().put(event.getUser().getId(), event.getOptions().get(0).getAsString());
                BotStartConfig.getMapLanguages().put(event.getUser().getId(), event.getOptions().get(1).getAsString());

                event.reply(jsonParsers.getLocale("slash_language", event.getUser().getId())
                                .replaceAll("\\{0}", event.getOptions().get(0).getAsString())
                                .replaceAll("\\{1}", event.getOptions().get(1).getAsString()))
                        .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                        .queue();
                GameLanguage gameLanguage = new GameLanguage();
                gameLanguage.setUserIdLong(event.getUser().getId());
                gameLanguage.setLanguage(event.getOptions().get(0).getAsString());
                gameLanguageRepository.save(gameLanguage);

                Language language = new Language();
                language.setUserIdLong(event.getUser().getId());
                language.setLanguage(event.getOptions().get(1).getAsString());
                languageRepository.save(language);
            }

            if (event.getName().equals("set-game")) {
                String mode = event.getOption("mode", OptionMapping::getAsString);
                BotStartConfig.getMapGameMode().put(event.getUser().getId(), mode);

                event.reply(jsonParsers.getLocale("game_mode", event.getUser().getId()) +  mode)
                        .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                        .setEphemeral(true)
                        .queue();

                GameMode gameMode = new GameMode();
                gameMode.setUserIdLong(event.getUser().getId());
                gameMode.setMode(mode);
                gameModeRepository.save(gameMode);
                return;
            }

            if (event.getName().equals("word")) {
                if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
                    String word = event.getOption("guess", OptionMapping::getAsString).toLowerCase();
                    long userIdLong = event.getUser().getIdLong();
                    event.reply(jsonParsers.getLocale("got_your_word", event.getUser().getId()))
                            .setEphemeral(true)
                            .queue();

                    HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).fullWord(word);
                } else {
                    event.reply(jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getUser().getId()))
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
                        event.getUser().getId(),
                        event.getUser().getName());
                return;
            }

            if (event.getName().equals("stats")) {
                MessageStats messageStats = new MessageStats(gamesRepository);
                messageStats.sendStats(
                        null,
                        event,
                        event.getUser().getAvatarUrl(),
                        event.getUser().getId(),
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