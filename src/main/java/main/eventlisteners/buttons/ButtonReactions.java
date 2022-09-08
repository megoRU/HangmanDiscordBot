package main.eventlisteners.buttons;

import lombok.RequiredArgsConstructor;
import main.config.BotStartConfig;
import main.enums.Buttons;
import main.eventlisteners.buildClass.GameLanguageChange;
import main.eventlisteners.buildClass.Help;
import main.eventlisteners.buildClass.MessageStats;
import main.hangman.Hangman;
import main.hangman.HangmanRegistry;
import main.hangman.impl.HangmanHelper;
import main.jsonparser.JSONParsers;
import main.model.entity.GameLanguage;
import main.model.entity.Language;
import main.model.repository.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.List;
import java.util.Objects;

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

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        try {
            if (event.getUser().isBot()) return;

            if (event.getGuild() != null
                    && !event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_SEND)
                    && !event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_MANAGE)
                    && !event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.VIEW_CHANNEL)) {
                return;
            }

            long userIdLong = event.getUser().getIdLong();

            //Проверяем смену языка при активной игре. Если игра активна и идет сменя языка - запрещать.
            //Если игры нет изменяем
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_CHANGE_GAME_LANGUAGE.name())) {
                event.deferEdit().queue();
                event.editButton(event.getButton().asDisabled()).queue();

                if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
                    String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", event.getUser().getIdLong());

                    event.getHook()
                            .sendMessage(reactionsButtonWhenPlay)
                            .setEphemeral(true).queue();
                } else {
                    String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";
                    String reactionsButtonSave = String.format(jsonParsers.getLocale("ReactionsButton_Save", event.getUser().getIdLong()), event.getButton().getLabel());

                    BotStartConfig.getMapGameLanguages().put(event.getUser().getIdLong(), buttonName);
                    event.getHook().sendMessage(reactionsButtonSave).setEphemeral(true).queue();

                    GameLanguage gameLanguage = new GameLanguage();
                    gameLanguage.setUserIdLong(event.getUser().getIdLong());
                    gameLanguage.setLanguage(buttonName);
                    gameLanguageRepository.save(gameLanguage);
                }
                return;
            }

            if (((Objects.equals(event.getButton().getId(), Buttons.BUTTON_RUS.name())
                    || Objects.equals(event.getButton().getId(), Buttons.BUTTON_ENG.name())
                    || Objects.equals(event.getButton().getId(), Buttons.BUTTON_CHANGE_GAME_LANGUAGE.name()))
                    && HangmanRegistry.getInstance().hasHangman(userIdLong))) {
                event.deferEdit().queue();
                event.editButton(event.getButton().asDisabled()).queue();

                String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", event.getUser().getIdLong());

                EmbedBuilder youPlay = new EmbedBuilder();
                youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                youPlay.setColor(0x00FF00);
                youPlay.setDescription(reactionsButtonWhenPlay);

                event.getHook().sendMessageEmbeds(youPlay.build())
                        .setEphemeral(true)
                        .addActionRow(List.of(Button.danger(Buttons.BUTTON_STOP.name(), "Stop game")))
                        .queue();
                return;
            }

            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_RUS.name())) {
                event.deferEdit().queue();
                event.editButton(event.getButton().asDisabled()).queue();
                new GameLanguageChange(gameLanguageRepository).set("rus", event.getUser().getIdLong());

                String languageChangeLang = String.format(jsonParsers.getLocale("language_change_lang", event.getUser().getIdLong()), "Кириллица");

                event.getHook().sendMessage(languageChangeLang).setEphemeral(true).queue();
                return;
            }

            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_ENG.name())) {
                event.deferEdit().queue();
                event.editButton(event.getButton().asDisabled()).queue();
                new GameLanguageChange(gameLanguageRepository).set("eng", event.getUser().getIdLong());

                String languageChangeLang = String.format(jsonParsers.getLocale("language_change_lang", event.getUser().getIdLong()), "Latin");

                event.getHook().sendMessage(languageChangeLang)
                        .setEphemeral(true).queue();
                return;
            }

            //Меняем язык на тот что был в кнопке
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_CHANGE_LANGUAGE.name())) {
                event.deferEdit().queue();
                event.editButton(event.getButton().asDisabled()).queue();
                String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";
                BotStartConfig.getMapLanguages().put(event.getUser().getIdLong(), buttonName);

                String languageChangeLang = String.format(jsonParsers.getLocale("language_change_lang", event.getUser().getIdLong()),
                        buttonName.equals("rus") ? "Русский" : "English");

                event.getHook().sendMessage(languageChangeLang).setEphemeral(true).queue();
                Language language = new Language();
                language.setUserIdLong(event.getUser().getIdLong());
                language.setLanguage(buttonName);
                languageRepository.save(language);
                return;
            }

            //При нажатии на кнопку HELP, бот присылает в чат информацию
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_HELP.name())) {
                event.deferEdit().queue(); //Можно удалить так как editButton() решает эту проблему
                event.editButton(event.getButton().asDisabled()).queue();

                Help help = new Help();
                help.send(
                        event.getChannel(),
                        null,
                        event.getUser().getAvatarUrl(),
                        event.getUser().getIdLong(),
                        event.getUser().getName());
                return;
            }

            //Если нажата кнопка START, и нет активной игры, то создаем
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_START_NEW_GAME.name())) {
                event.deferEdit().queue();
                event.editButton(event.getButton().asDisabled()).queue();
                Long userIdLongString = event.getUser().getIdLong();
                String needSetupMode = jsonParsers.getLocale("need_setup_mode", event.getUser().getIdLong());

                if (!BotStartConfig.getMapGameLanguages().containsKey(userIdLong)) {
                    event.getHook().sendMessage(needSetupMode)
                            .addActionRow(
                                    Button.secondary(Buttons.BUTTON_RUS.name(), "Кириллица")
                                            .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")),
                                    Button.secondary(Buttons.BUTTON_ENG.name(), "Latin")
                                            .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")))
                            .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play"))
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                if (!HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    event.getChannel().sendTyping().queue();

                    if (event.getGuild() != null) {
                        boolean hasPermission = event.getGuild().getSelfMember()
                                .hasPermission(
                                        event.getGuildChannel(),
                                        Permission.MESSAGE_SEND,
                                        Permission.MESSAGE_MANAGE,
                                        Permission.VIEW_CHANNEL);
                        if (!hasPermission) return;

                        HangmanRegistry.getInstance().setHangman(userIdLong,
                                new Hangman(
                                        event.getUser().getIdLong(),
                                        event.getGuild().getIdLong(),
                                        event.getGuildChannel().getIdLong(),
                                        hangmanGameRepository,
                                        gamesRepository,
                                        playerRepository));
                    } else {
                        HangmanRegistry.getInstance().setHangman(
                                userIdLong,
                                new Hangman(
                                        event.getUser().getIdLong(),
                                        null,
                                        event.getChannel().getIdLong(),
                                        hangmanGameRepository,
                                        gamesRepository,
                                        playerRepository));
                    }
                    HangmanRegistry.getInstance().getActiveHangman(userIdLong)
                            .startGame(
                                    event.getChannel(),
                                    event.getUser().getAvatarUrl(),
                                    event.getUser().getName());
                } else {
                    String hangmanListenerYouPlay = jsonParsers.getLocale("Hangman_Listener_You_Play", event.getUser().getIdLong());

                    EmbedBuilder youPlay = new EmbedBuilder();
                    youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    youPlay.setColor(0x00FF00);

                    youPlay.setDescription(hangmanListenerYouPlay);

                    event.getChannel().sendMessageEmbeds(youPlay.build())
                            .setActionRow(Button.danger(Buttons.BUTTON_STOP.name(), "Stop game")).queue();
                }
                return;
            }

            //Если нажата кнопка STOP, и игрок сейчас играет, завершаем
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_STOP.name())) {
                event.deferEdit().queue();
                event.editButton(event.getButton().asDisabled()).queue();

                if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    String hangmanEngGame = jsonParsers.getLocale("Hangman_Eng_game", event.getUser().getIdLong());
                    String hangmanEngGame1 = jsonGameParsers.getLocale("Hangman_Eng_game", event.getUser().getIdLong());

                    event.getHook().sendMessage(hangmanEngGame)
                            .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                            .queue();

                    var embedBuilder = HangmanRegistry.getInstance().getActiveHangman(event.getUser().getIdLong())
                            .embedBuilder(Color.GREEN,
                                    hangmanEngGame1,
                                    false,
                                    false,
                                    null
                            );

                    HangmanHelper.editMessage(embedBuilder, event.getUser().getIdLong());
                    HangmanRegistry.getInstance().removeHangman(event.getUser().getIdLong());
                    hangmanGameRepository.deleteActiveGame(event.getUser().getIdLong());
                    //Если нажата кнопка STOP, и игрок сейчас не играет, присылаем в час уведомление
                } else {
                    String hangmanYouAreNotPlay = jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getUser().getIdLong());
                    event.getChannel().sendMessage(hangmanYouAreNotPlay)
                            .setActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                            .queue();
                }
                return;
            }

            //Получаем статистику по нажатии на кнопку
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_MY_STATS.name())) {
                event.deferEdit().queue();
                event.editButton(event.getButton().asDisabled()).queue();

                MessageStats messageStats = new MessageStats(
                        gamesRepository,
                        event.getHook(),
                        event.getUser().getAvatarUrl(),
                        event.getUser().getIdLong(),
                        event.getUser().getName());

                messageStats.sendStats();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}