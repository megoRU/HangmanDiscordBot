package main.eventlisteners;

import lombok.RequiredArgsConstructor;
import main.config.BotStartConfig;
import main.enums.Buttons;
import main.hangman.Hangman;
import main.hangman.HangmanRegistry;
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

@RequiredArgsConstructor
@Service
public class ReactionsButton extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();
    private final GameLanguageRepository gameLanguageRepository;
    private final LanguageRepository languageRepository;
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        try {
            if (event.getUser().isBot()) return;
            if (event.getButton() == null) return;
            if (event.getGuild() == null || event.getMember() == null) return;
            if (CheckPermissions.isHasPermissionsWriteAndEmbedLinks(event.getTextChannel())) return;

            long userIdLong = event.getUser().getIdLong();

            //Проверяем смену языка при активной игре. Если игра активна и идет сменя языка - запрещать
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_CHANGE_GAME_LANGUAGE.name())
                    && HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
                event.deferEdit().queue();
                event.getHook().sendMessage(jsonParsers
                                .getLocale("ReactionsButton_When_Play", event.getMember().getId()))
                        .setEphemeral(true).queue();
                return;
            }

            if ((Objects.equals(event.getButton().getId(), Buttons.BUTTON_RUS.name()) || Objects.equals(event.getButton().getId(), Buttons.BUTTON_ENG.name()))
                    && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.deferEdit().queue();
                event.getHook().sendMessage(jsonParsers
                                .getLocale("ReactionsButton_When_Play", event.getMember().getId()))
                        .setEphemeral(true).queue();
                return;
            }

            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_RUS.name())) {
                event.deferEdit().queue();
                new GameLanguageChange(gameLanguageRepository).changeGameLanguage("rus", event.getUser().getId());
                event.getHook().sendMessage(jsonParsers
                                .getLocale("language_change_lang", event.getMember().getId()).replaceAll("\\{0}", "Кириллица"))
                        .setEphemeral(true).queue();
                return;
            }

            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_ENG.name())) {
                event.deferEdit().queue();
                new GameLanguageChange(gameLanguageRepository).changeGameLanguage("eng", event.getUser().getId());
                event.getHook().sendMessage(jsonParsers
                                .getLocale("language_change_lang", event.getMember().getId()).replaceAll("\\{0}", "Latin"))
                        .setEphemeral(true).queue();
                return;
            }

            //Меняем язык на тот что был в кнопке
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_CHANGE_LANGUAGE.name())) {
                event.deferEdit().queue();
                String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";
                BotStartConfig.getMapLanguages().put(event.getMember().getId(), buttonName);

                event.getHook().sendMessage(jsonParsers
                                .getLocale("language_change_lang", event.getMember().getId())
                                .replaceAll("\\{0}", buttonName.equals("rus") ? "Русский" : "English"))
                        .setEphemeral(true).queue();
                Language language = new Language();
                language.setUserIdLong(event.getUser().getId());
                language.setLanguage(buttonName);
                languageRepository.save(language);
                return;
            }

            //При нажатии на кнопку HELP, бот присылает в чат информацию
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_HELP.name())) {

                event.deferEdit().queue();
                MessageInfoHelp messageInfoHelp = new MessageInfoHelp();
                messageInfoHelp.buildMessage(
                        BotStartConfig.getMapPrefix().get(event.getGuild().getId()) == null ? "!" : BotStartConfig.getMapPrefix().get(event.getGuild().getId()),
                        event.getTextChannel(),
                        null,
                        event.getUser().getAvatarUrl(),
                        event.getUser().getId(),
                        event.getUser().getName());
                return;
            }

            //Если нажата кнопка START, и нет активной игры, то создаем
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_START_NEW_GAME.name()) && !HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.deferEdit().queue();
                event.getChannel().sendTyping().queue();
                HangmanRegistry.getInstance().setHangman(userIdLong, new Hangman(event.getUser().getId(), event.getGuild().getId(), event.getTextChannel().getIdLong(), hangmanGameRepository, gamesRepository, playerRepository));
                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).startGame(event.getTextChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
                return;
            }

            //Если нажата кнопка START, и игрок сейчас играет, присылаем в час уведомление
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_START_NEW_GAME.name()) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.deferEdit().queue();

                EmbedBuilder youPlay = new EmbedBuilder();
                youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                youPlay.setColor(0x00FF00);
                youPlay.setDescription(jsonParsers.getLocale("Hangman_Listener_You_Play",
                        event.getUser().getId()).replaceAll("\\{0}",
                        BotStartConfig.getMapPrefix().get(event.getGuild().getId()) == null
                                ? "!hg"
                                : BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "hg"));

                event.getChannel().sendMessageEmbeds(youPlay.build())
                        .setActionRow(net.dv8tion.jda.api.interactions.components.buttons.Button.danger(Buttons.BUTTON_STOP.name(), "Stop game")).queue();
                return;
            }

            //Если нажата кнопка STOP, и игрок сейчас играет, завершаем
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_STOP.name())) {
                event.deferEdit().queue();

                if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    HangmanRegistry.getInstance().getActiveHangman().remove(event.getUser().getIdLong());

                    event.getHook().sendMessage(jsonParsers.getLocale("Hangman_Eng_game",
                                    event.getUser().getId()).replaceAll("\\{0}",
                                    BotStartConfig.getMapPrefix().get(event.getGuild().getId()) == null ? "!hg" : BotStartConfig.getMapPrefix().get(event.getGuild().getId())))
                            .addActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                            .queue();
                    hangmanGameRepository.deleteActiveGame(event.getUser().getIdLong());
                    //Если нажата кнопка STOP, и игрок сейчас не играет, присылаем в час уведомление
                } else {
                    event.getChannel().sendMessage(jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getUser().getId()))
                            .setActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                            .queue();
                }
                return;
            }
            //Меняем язык на тот что был в кнопке
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_CHANGE_GAME_LANGUAGE.name())) {
                event.deferEdit().queue();
                String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";

                BotStartConfig.getMapGameLanguages().put(event.getMember().getId(), buttonName);
                event.getHook().sendMessage(jsonParsers
                                .getLocale("ReactionsButton_Save", event.getMember().getId())
                                .replaceAll("\\{0}", event.getButton().getLabel()))
                        .setEphemeral(true).queue();

                GameLanguage gameLanguage = new GameLanguage();
                gameLanguage.setUserIdLong(event.getUser().getId());
                gameLanguage.setLanguage(buttonName);
                gameLanguageRepository.save(gameLanguage);
                return;
            }
            //Получаем статистику по нажатии на кнопку
            if (Objects.equals(event.getButton().getId(), Buttons.BUTTON_MY_STATS.name())) {
                event.deferEdit().queue();
                new MessageStats(gamesRepository).sendStats(
                        event.getTextChannel(),
                        null,
                        event.getMember().getUser().getAvatarUrl(),
                        event.getUser().getId(),
                        event.getMember().getUser().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}