package main.eventlisteners.buttons;

import lombok.RequiredArgsConstructor;
import main.config.BotStartConfig;
import main.enums.Buttons;
import main.eventlisteners.ChecksClass;
import main.eventlisteners.buildClass.GameLanguageChange;
import main.eventlisteners.buildClass.Help;
import main.eventlisteners.buildClass.MessageStats;
import main.hangman.Hangman;
import main.hangman.HangmanBuilder;
import main.hangman.HangmanEmbedUtils;
import main.hangman.HangmanRegistry;
import main.hangman.impl.ButtonIMpl;
import main.hangman.impl.HangmanHelper;
import main.jsonparser.JSONParsers;
import main.model.entity.GameLanguage;
import main.model.entity.Language;
import main.model.repository.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.logging.Logger;

@RequiredArgsConstructor
@Service
public class ButtonReactions extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);

    private final GameLanguageRepository gameLanguageRepository;
    private final LanguageRepository languageRepository;
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;

    private final static Logger LOGGER = Logger.getLogger(ButtonReactions.class.getName());

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        try {
            if (event.getUser().isBot()) return;

            boolean permission = ChecksClass.check(event);
            if (!permission) return;

            long userIdLong = event.getUser().getIdLong();

            //Проверяем смену языка при активной игре. Если игра активна и идет сменя языка - запрещать.
            //Если игры нет изменяем
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_CHANGE_GAME_LANGUAGE.name())) {
                event.editButton(event.getButton().asDisabled()).queue();

                if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
                    String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", event.getUser().getIdLong());
                    event.getHook().sendMessage(reactionsButtonWhenPlay).setEphemeral(true).queue();
                } else {
                    if (event.getButton().getEmoji() != null) {
                        String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";
                        String reactionsButton = jsonParsers.getLocale("ReactionsButton_Save", event.getUser().getIdLong());
                        String reactionsButtonSave = String.format(reactionsButton, event.getButton().getLabel());
                        BotStartConfig.getMapGameLanguages().put(event.getUser().getIdLong(), buttonName);
                        event.getHook().sendMessage(reactionsButtonSave).setEphemeral(true).queue();

                        GameLanguage gameLanguage = new GameLanguage();
                        gameLanguage.setUserIdLong(event.getUser().getIdLong());
                        gameLanguage.setLanguage(buttonName);
                        gameLanguageRepository.save(gameLanguage);
                    }
                }
                return;
            }

            //Нельзя менять язык во время игры
            if (((Objects.equals(event.getButton().getId(), Buttons.BUTTON_RUS.name())
                    || Objects.equals(event.getButton().getId(), Buttons.BUTTON_ENG.name())
                    || Objects.equals(event.getButton().getId(), Buttons.BUTTON_CHANGE_GAME_LANGUAGE.name()))
                    && HangmanRegistry.getInstance().hasHangman(userIdLong))) {
                event.editButton(event.getButton().asDisabled()).queue();

                String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", event.getUser().getIdLong());

                EmbedBuilder youPlay = new EmbedBuilder();
                youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                youPlay.setColor(0x00FF00);
                youPlay.setDescription(reactionsButtonWhenPlay);

                event.getHook().sendMessageEmbeds(youPlay.build())
                        .setEphemeral(true)
                        .addActionRow(ButtonIMpl.BUTTON_STOP)
                        .queue();
                return;
            }

            //Смена языка по кнопке
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_RUS.name())
                    || Objects.equals(event.getButton().getId(), Buttons.BUTTON_ENG.name())) {
                event.editButton(event.getButton().asDisabled()).queue();
                GameLanguageChange gameLanguageChange = new GameLanguageChange(gameLanguageRepository);
                String languageChangeLang;
                if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_RUS.name())) {
                    gameLanguageChange.set("rus", event.getUser().getIdLong());
                    String languageChange = jsonParsers.getLocale("language_change_lang", event.getUser().getIdLong());
                    languageChangeLang = String.format(languageChange, "Кириллица");
                } else {
                    gameLanguageChange.set("eng", event.getUser().getIdLong());
                    String languageChange = jsonParsers.getLocale("language_change_lang", event.getUser().getIdLong());
                    languageChangeLang = String.format(languageChange, "Latin");
                }
                event.getHook().sendMessage(languageChangeLang).setEphemeral(true).queue();
                return;
            }

            //Меняем язык на тот что был в кнопке
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_CHANGE_LANGUAGE.name())) {
                event.editButton(event.getButton().asDisabled()).queue();
                if (event.getButton().getEmoji() != null) {
                    String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";
                    BotStartConfig.getMapLanguages().put(event.getUser().getIdLong(), buttonName);
                    String buttonValue = buttonName.equals("rus") ? "Русский" : "English";
                    String languageChange = jsonParsers.getLocale("language_change_lang", event.getUser().getIdLong());
                    String languageChangeLang = String.format(languageChange, buttonValue);

                    event.getHook().sendMessage(languageChangeLang).setEphemeral(true).queue();
                    Language language = new Language();
                    language.setUserIdLong(event.getUser().getIdLong());
                    language.setLanguage(buttonName);
                    languageRepository.save(language);
                }
                return;
            }

            //При нажатии на кнопку HELP, бот присылает в чат информацию
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_HELP.name())) {
                event.editButton(event.getButton().asDisabled()).queue();
                Help help = new Help(event.getHook(), userIdLong);
                help.send();
                return;
            }

            //Если нажата кнопка START, и нет активной игры, то создаем
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_START_NEW_GAME.name())
                    || (event.getButton().getId() != null
                    && event.getButton().getId().matches("BUTTON_START_NEW_GAME_\\d+_\\d+"))) {
                event.editButton(event.getButton().asDisabled()).queue();
                String gameLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getIdLong());
                String userGameLanguage = BotStartConfig.getMapGameLanguages().get(userIdLong);

                if (userGameLanguage == null) {
                    event.getHook().sendMessage(gameLanguage)
                            .addActionRow(ButtonIMpl.BUTTON_RUSSIAN, ButtonIMpl.BUTTON_ENGLISH)
                            .addActionRow(ButtonIMpl.BUTTON_PLAY_AGAIN)
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                if (!HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    event.getChannel().sendTyping().queue();

                    HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder()
                            .setUserIdLong(userIdLong)
                            .setHangmanGameRepository(hangmanGameRepository)
                            .setGamesRepository(gamesRepository)
                            .setPlayerRepository(playerRepository);

                    Hangman hangman;
                    //Guild Play
                    boolean matches = event.getButton().getId().matches("BUTTON_START_NEW_GAME_\\d+_\\d+");
                    boolean isFromGuild = event.getGuild() != null;

                    System.out.println("isFromGuild: " + isFromGuild);
                    System.out.println("matches: " + matches);

                    if (event.getGuild() != null) {

                        hangmanBuilder.setGuildIdLong(event.getGuild().getIdLong());
                        hangmanBuilder.setChannelId(event.getGuildChannel().getIdLong());

                        if (matches) {
                            String[] split = event.getButton().getId()
                                    .replace("BUTTON_START_NEW_GAME_", "")
                                    .split("_");

                            long secondUser = 0L;

                            for (String userId : split) {
                                System.out.println("Split users: " + userId);
                                if (userIdLong != Long.parseLong(userId)) {
                                    secondUser = Long.parseLong(userId);
                                    boolean hasHangmanSecondUser = HangmanRegistry.getInstance().hasHangman(secondUser);
                                    if (hasHangmanSecondUser) {
                                        String secondPlayerAlreadyPlaying = jsonParsers.getLocale("second_player_already_playing", userIdLong);
                                        event.getHook().sendMessage(secondPlayerAlreadyPlaying).setEphemeral(true).queue();
                                        return;
                                    }
                                }
                            }

                            hangmanBuilder.setSecondUserIdLong(secondUser);

                            hangman = hangmanBuilder.build();

                            HangmanRegistry.getInstance().setHangman(userIdLong, hangman);
                            HangmanRegistry.getInstance().setHangman(secondUser, hangman);

                            hangman.startGame(event.getChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
                        } else {
                            hangman = hangmanBuilder.build();

                            HangmanRegistry.getInstance().setHangman(userIdLong, hangman);

                            hangman.startGame(event.getChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
                        }
                        //DM play
                    } else {
                        hangmanBuilder.setChannelId(event.getChannel().getIdLong());
                        hangmanBuilder.setGuildIdLong(null);

                        hangman = hangmanBuilder.build();

                        HangmanRegistry.getInstance().setHangman(userIdLong, hangman);
                        hangman.startGame(event.getChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
                    }
                } else {
                    String hangmanListenerYouPlay = jsonParsers.getLocale("Hangman_Listener_You_Play", event.getUser().getIdLong());

                    EmbedBuilder youPlay = new EmbedBuilder();
                    youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    youPlay.setColor(0x00FF00);

                    youPlay.setDescription(hangmanListenerYouPlay);

                    event.getHook()
                            .sendMessageEmbeds(youPlay.build())
                            .setActionRow(Button.danger(Buttons.BUTTON_STOP.name(), "Stop game"))
                            .queue();
                }
                return;
            }

            //Если нажата кнопка STOP, и игрок сейчас играет, завершаем
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_STOP.name())) {
                event.editButton(event.getButton().asDisabled()).queue();
                Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
                if (hangman != null) {
                    long userId = hangman.getUserId();
                    long secondPlayer = hangman.getSecondPlayer();

                    String hangmanEngGame = jsonParsers.getLocale("Hangman_Eng_game", userId);
                    String hangmanEngGame1 = jsonGameParsers.getLocale("Hangman_Eng_game", userId);

                    if (secondPlayer != 0L) {
                        event.getHook()
                                .sendMessage(hangmanEngGame)
                                .addActionRow(ButtonIMpl.getButtonPlayAgainWithUsers(userId, secondPlayer))
                                .queue();
                    } else {
                        event.getHook().sendMessage(hangmanEngGame)
                                .addActionRow(ButtonIMpl.BUTTON_PLAY_AGAIN)
                                .queue();
                    }

                    EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanPattern(userId, hangmanEngGame1);

                    HangmanHelper.editMessage(embedBuilder, userId);
                    HangmanRegistry.getInstance().removeHangman(userId);
                    hangmanGameRepository.deleteActiveGame(userId);
                    //Если нажата кнопка STOP, и игрок сейчас не играет, присылаем в час уведомление
                } else {
                    String hangmanYouAreNotPlay = jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getUser().getIdLong());
                    event.getHook()
                            .sendMessage(hangmanYouAreNotPlay)
                            .setActionRow(ButtonIMpl.BUTTON_PLAY_AGAIN)
                            .queue();
                }
                return;
            }

            //Получаем статистику по нажатии на кнопку
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_MY_STATS.name())) {
                event.editButton(event.getButton().asDisabled()).queue();

                MessageStats messageStats = new MessageStats(
                        gamesRepository,
                        event.getHook(),
                        event.getUser().getAvatarUrl(),
                        event.getUser().getIdLong(),
                        event.getUser().getName());

                messageStats.send();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}