package hangman;

import db.DataBase;
import jsonparser.JSONParsers;
import messagesevents.CheckPermissions;
import messagesevents.GameLanguageChange;
import messagesevents.MessageInfoHelp;
import messagesevents.MessageStats;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

import java.util.Objects;

public class ReactionsButton extends ListenerAdapter {

    public static final String START_NEW_GAME = "NEW_GAME";
    public static final String START_CHANGE_GAME_LANGUAGE = "CHANGE_GAME_LANGUAGE";
    public static final String MY_STATS = "MY_STATS";
    public static final String BUTTON_HELP = "BUTTON_HELP";
    public static final String BUTTON_RUS = "LANGUAGE_RUS";
    public static final String BUTTON_ENG = "LANGUAGE_ENG";
    public static final String BUTTON_STOP = "BUTTON_STOP";
    public static final String CHANGE_LANGUAGE = "CHANGE_LANGUAGE";
    private final JSONParsers jsonParsers = new JSONParsers();

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        try {
            if (event.getUser().isBot()) return;
            if (event.getButton() == null) return;
            if (event.getGuild() == null || event.getMember() == null) return;

            if (!new CheckPermissions(event.getTextChannel()).checkMessageWriteAndEmbedLinks()) {
                return;
            }

            //Проверяем смену языка при активной игре. Если игра активна и идет сменя языка - запрещать
            if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong()) &&
                    (Objects.equals(event.getButton().getId(), event.getMember().getUser().getId() + ":" + START_NEW_GAME)
                            || Objects.equals(event.getButton().getId(), event.getMember().getUser().getId() + ":" + START_CHANGE_GAME_LANGUAGE))) {
                event.deferEdit().queue();
                event.getHook().sendMessage(jsonParsers
                                .getLocale("ReactionsButton_When_Play", event.getMember().getId()))
                        .setEphemeral(true).queue();
                return;
            }

            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_RUS)) {
                event.deferEdit().queue();
                new GameLanguageChange().changeGameLanguage("rus", event.getUser().getId());
                event.getHook().sendMessage(jsonParsers
                                .getLocale("language_change_lang", event.getMember().getId()) + "Кириллица")
                        .setEphemeral(true).queue();
                return;
            }

            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_ENG)) {
                event.deferEdit().queue();
                new GameLanguageChange().changeGameLanguage("eng", event.getUser().getId());
                event.getHook().sendMessage(jsonParsers
                                .getLocale("language_change_lang", event.getMember().getId()) + "Latin")
                        .setEphemeral(true).queue();
                return;
            }

            //Меняем язык на тот что был в кнопке
            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + CHANGE_LANGUAGE)) {
                event.deferEdit().queue();
                String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";
                BotStart.getMapLanguages().put(event.getMember().getId(), buttonName);

                event.getHook().sendMessage(jsonParsers
                                .getLocale("ReactionsButton_Save", event.getMember().getId())
                                .replaceAll("\\{0}", buttonName.toUpperCase()))
                        .setEphemeral(true).queue();

                DataBase.getInstance().addLanguageToDB(event.getMember().getId(), buttonName);
                return;
            }

            //При нажатии на кнопку HELP, бот присылает в чат информацию
            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_HELP)) {

                event.deferEdit().queue();
                MessageInfoHelp messageInfoHelp = new MessageInfoHelp();
                messageInfoHelp.buildMessage(
                        BotStart.getMapPrefix().get(event.getGuild().getId()) == null ? "!" : BotStart.getMapPrefix().get(event.getGuild().getId()),
                        event.getTextChannel(),
                        event.getUser().getAvatarUrl(),
                        event.getUser().getId(),
                        event.getUser().getName(),
                        event.getGuild().getId());
                return;
            }

            long userIdLong = event.getUser().getIdLong();

            //Если нажата кнопка START, и нет активной игры, то создаем
            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + START_NEW_GAME) && !HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.deferEdit().queue();
                HangmanRegistry.getInstance().setHangman(userIdLong, new Hangman(event.getUser().getId(), event.getGuild().getId(), event.getTextChannel().getIdLong()));
                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).startGame(event.getTextChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
                return;
            }

            //Если нажата кнопка START, и игрок сейчас играет, присылаем в час уведомление
            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + START_NEW_GAME) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.deferEdit().queue();

                EmbedBuilder youPlay = new EmbedBuilder();
                youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                youPlay.setColor(0x00FF00);
                youPlay.setDescription(jsonParsers.getLocale("Hangman_Listener_You_Play",
                        event.getUser().getId()).replaceAll("\\{0}",
                        BotStart.getMapPrefix().get(event.getGuild().getId()) == null
                                ? "!hg"
                                : BotStart.getMapPrefix().get(event.getGuild().getId()) + "hg"));

                event.getChannel().sendMessageEmbeds(youPlay.build())
                        .setActionRow(Button.danger(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_STOP, "Stop game")).queue();
                return;
            }

            //Если нажата кнопка STOP, и игрок сейчас играет, завершаем
            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_STOP)) {
                event.deferEdit().queue();

                if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    HangmanRegistry.getInstance().getActiveHangman().remove(event.getUser().getIdLong());

                    event.getHook().sendMessage(jsonParsers.getLocale("Hangman_Eng_game",
                                    event.getUser().getId()).replaceAll("\\{0}",
                                    BotStart.getMapPrefix().get(event.getGuild().getId()) == null ? "!hg" : BotStart.getMapPrefix().get(event.getGuild().getId())))
                            .addActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.START_NEW_GAME, "Play again"))
                            .queue();

                    DataBase.getInstance().deleteActiveGame(event.getUser().getId());
                    //Если нажата кнопка STOP, и игрок сейчас не играет, присылаем в час уведомление
                } else {
                    event.getChannel().sendMessage(jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getUser().getId()))
                            .setActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.START_NEW_GAME, "Play again"))
                            .queue();
                }
                return;
            }
            //Меняем язык на тот что был в кнопке
            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + START_CHANGE_GAME_LANGUAGE)) {
                event.deferEdit().queue();
                String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";
                BotStart.getMapGameLanguages().put(event.getMember().getId(), buttonName);
                event.getHook().sendMessage(jsonParsers
                                .getLocale("ReactionsButton_Save", event.getMember().getId())
                                .replaceAll("\\{0}", buttonName.toUpperCase()))
                        .setEphemeral(true).queue();
                DataBase.getInstance().addGameLanguageToDB(event.getMember().getId(), buttonName);
                return;
            }
            //Получаем статистику по нажатии на кнопку
            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + MY_STATS)) {
                event.deferEdit().queue();
                new MessageStats().sendStats(
                        event.getTextChannel(),
                        event.getMember().getUser().getAvatarUrl(),
                        event.getUser().getId(),
                        event.getMember().getUser().getName());
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}