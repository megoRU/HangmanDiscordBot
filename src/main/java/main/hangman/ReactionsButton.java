package main.hangman;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.eventlisteners.CheckPermissions;
import main.eventlisteners.GameLanguageChange;
import main.eventlisteners.MessageInfoHelp;
import main.model.repository.GameLanguageRepository;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@AllArgsConstructor
public class ReactionsButton extends ListenerAdapter {

    public static final String BUTTON_START_NEW_GAME = "BUTTON_START_NEW_GAME";
    public static final String BUTTON_CHANGE_GAME_LANGUAGE = "BUTTON_CHANGE_GAME_LANGUAGE";
    public static final String BUTTON_MY_STATS = "BUTTON_MY_STATS";
    public static final String BUTTON_HELP = "BUTTON_HELP";
    public static final String BUTTON_RUS = "BUTTON_RUS";
    public static final String BUTTON_ENG = "BUTTON_ENG";
    public static final String BUTTON_STOP = "BUTTON_STOP";
    public static final String BUTTON_CHANGE_LANGUAGE = "BUTTON_CHANGE_LANGUAGE";
    private final JSONParsers jsonParsers = new JSONParsers();
    private final GameLanguageRepository gameLanguageRepository;
    private final HangmanGameRepository hangmanGameRepository;

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        try {
            if (event.getUser().isBot()) return;
            if (event.getButton() == null) return;
            if (event.getGuild() == null || event.getMember() == null) return;

            if (CheckPermissions.isHasPermissionsWriteAndEmbedLinks(event.getTextChannel())) {
                return;
            }

            //Проверяем смену языка при активной игре. Если игра активна и идет сменя языка - запрещать
            if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong()) &&
                    (Objects.equals(event.getButton().getId(), BUTTON_START_NEW_GAME) ||
                            Objects.equals(event.getButton().getId(), BUTTON_CHANGE_GAME_LANGUAGE))) {
                event.deferEdit().queue();
                event.getHook().sendMessage(jsonParsers
                                .getLocale("ReactionsButton_When_Play", event.getMember().getId()))
                        .setEphemeral(true).queue();
                return;
            }

            if (Objects.equals(event.getButton().getId(), BUTTON_RUS)) {
                event.deferEdit().queue();
                new GameLanguageChange(gameLanguageRepository).changeGameLanguage("rus", event.getUser().getId());
                event.getHook().sendMessage(jsonParsers
                                .getLocale("language_change_lang", event.getMember().getId()).replaceAll("\\{0}", "Кириллица"))
                        .setEphemeral(true).queue();
                return;
            }

            if (Objects.equals(event.getButton().getId(), BUTTON_ENG)) {
                event.deferEdit().queue();
                new GameLanguageChange(gameLanguageRepository).changeGameLanguage("eng", event.getUser().getId());
                event.getHook().sendMessage(jsonParsers
                                .getLocale("language_change_lang", event.getMember().getId()).replaceAll("\\{0}", "Latin"))
                        .setEphemeral(true).queue();
                return;
            }

            //Меняем язык на тот что был в кнопке
            if (Objects.equals(event.getButton().getId(), BUTTON_CHANGE_LANGUAGE)) {
                event.deferEdit().queue();
                String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";
                BotStartConfig.getMapLanguages().put(event.getMember().getId(), buttonName);

                event.getHook().sendMessage(jsonParsers
                                .getLocale("language_change_lang", event.getMember().getId())
                                .replaceAll("\\{0}", buttonName.equals("rus") ? "Русский" : "English"))
                        .setEphemeral(true).queue();
                //TODO: Сделать через репозитории
//                DataBase.getInstance().addLanguageToDB(event.getMember().getId(), buttonName);
                return;
            }

            //При нажатии на кнопку HELP, бот присылает в чат информацию
            if (Objects.equals(event.getButton().getId(), BUTTON_HELP)) {

                event.deferEdit().queue();
                MessageInfoHelp messageInfoHelp = new MessageInfoHelp();
                messageInfoHelp.buildMessage(
                        BotStartConfig.getMapPrefix().get(event.getGuild().getId()) == null ? "!" : BotStartConfig.getMapPrefix().get(event.getGuild().getId()),
                        event.getTextChannel(),
                        event.getUser().getAvatarUrl(),
                        event.getUser().getId(),
                        event.getUser().getName(),
                        event.getGuild().getId());
                return;
            }

            long userIdLong = event.getUser().getIdLong();

            //Если нажата кнопка START, и нет активной игры, то создаем
            if (Objects.equals(event.getButton().getId(), BUTTON_START_NEW_GAME) && !HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.deferEdit().queue();
                event.getChannel().sendTyping().queue();
                HangmanRegistry.getInstance().setHangman(userIdLong, new Hangman(event.getUser().getId(), event.getGuild().getId(), event.getTextChannel().getIdLong(), hangmanGameRepository));
                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).startGame(event.getTextChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
                return;
            }

            //Если нажата кнопка START, и игрок сейчас играет, присылаем в час уведомление
            if (Objects.equals(event.getButton().getId(), BUTTON_START_NEW_GAME) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
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
                        .setActionRow(Button.danger(ReactionsButton.BUTTON_STOP, "Stop game")).queue();
                return;
            }

            //Если нажата кнопка STOP, и игрок сейчас играет, завершаем
            if (Objects.equals(event.getButton().getId(), BUTTON_STOP)) {
                event.deferEdit().queue();

                if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    HangmanRegistry.getInstance().getActiveHangman().remove(event.getUser().getIdLong());

                    event.getHook().sendMessage(jsonParsers.getLocale("Hangman_Eng_game",
                                    event.getUser().getId()).replaceAll("\\{0}",
                                    BotStartConfig.getMapPrefix().get(event.getGuild().getId()) == null ? "!hg" : BotStartConfig.getMapPrefix().get(event.getGuild().getId())))
                            .addActionRow(Button.success(ReactionsButton.BUTTON_START_NEW_GAME, "Play again"))
                            .queue();
                    //TODO: Сделать через репозитории

//                    DataBase.getInstance().deleteActiveGame(event.getUser().getId());

                    //Если нажата кнопка STOP, и игрок сейчас не играет, присылаем в час уведомление
                } else {
                    event.getChannel().sendMessage(jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getUser().getId()))
                            .setActionRow(Button.success(ReactionsButton.BUTTON_START_NEW_GAME, "Play again"))
                            .queue();
                }
                return;
            }
            //Меняем язык на тот что был в кнопке
            if (Objects.equals(event.getButton().getId(), BUTTON_CHANGE_GAME_LANGUAGE)) {
                event.deferEdit().queue();
                String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";

                BotStartConfig.getMapGameLanguages().put(event.getMember().getId(), buttonName);
                event.getHook().sendMessage(jsonParsers
                                .getLocale("ReactionsButton_Save", event.getMember().getId())
                                .replaceAll("\\{0}", event.getButton().getLabel()))
                        .setEphemeral(true).queue();

                //TODO: Сделать через репозитории
//                BotStartConfig.getInstance().addGameLanguageToDB(event.getMember().getId(), buttonName);

                return;
            }
            //Получаем статистику по нажатии на кнопку
            if (Objects.equals(event.getButton().getId(), BUTTON_MY_STATS)) {
                event.deferEdit().queue();
                //TODO: восстановить

//                new MessageStats().sendStats(
//                        event.getTextChannel(),
//                        event.getMember().getUser().getAvatarUrl(),
//                        event.getUser().getId(),
//                        event.getMember().getUser().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}