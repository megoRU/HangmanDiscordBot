package main.core.events;

import main.config.BotStartConfig;
import main.controller.UpdateController;
import main.hangman.*;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Map;

@Service
public class HangmanCommand {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    public void hangman(@NotNull GenericCommandInteractionEvent event, UpdateController updateController) {
        long userIdLong = event.getUser().getIdLong();
        long channelIdLong = event.getMessageChannel().getIdLong();
        Long guildIdLong = null;

        if (event.getGuild() != null) {
            guildIdLong = event.getGuild().getIdLong();
        }

        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            slashEvent.getChannel().sendTyping().queue();
        }

        HangmanRegistry instance = HangmanRegistry.getInstance();
        //Проверяем установлен ли язык. Если нет - то возвращаем в чат ошибку
        Map<Long, UserSettings.GameLanguage> mapGameLanguages = BotStartConfig.getMapGameLanguages();
        if (!mapGameLanguages.containsKey(userIdLong)) {
            String hangmanListenerNeedSetLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userIdLong);

            EmbedBuilder needSetLanguage = new EmbedBuilder();
            needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
            needSetLanguage.setColor(Color.GREEN);
            needSetLanguage.setDescription(hangmanListenerNeedSetLanguage);

            event.replyEmbeds(needSetLanguage.build())
                    .addActionRow(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH)
                    .addActionRow(HangmanUtils.BUTTON_PLAY_AGAIN)
                    .queue();
            //Проверяем если игрок уже играет. То присылаем в чат уведомление
        } else if (instance.hasCompetitive(userIdLong)) {
            String youArePlayNow = jsonParsers.getLocale("you_are_play_now", userIdLong);
            event.reply(youArePlayNow)
                    .queue();
        } else if (instance.hasHangman(userIdLong)) {
            String hangmanListenerYouPlay = jsonParsers.getLocale("Hangman_Listener_You_Play", userIdLong);

            EmbedBuilder youPlay = new EmbedBuilder();
            youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
            youPlay.setColor(Color.GREEN);
            youPlay.setDescription(hangmanListenerYouPlay);

            event.replyEmbeds(youPlay.build()).addActionRow(HangmanUtils.BUTTON_STOP).queue();
            //Если всё хорошо, создаем игру
        } else {
            HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, guildIdLong, channelIdLong);
            HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder();
            hangmanBuilder.addHangmanPlayer(hangmanPlayer);
            hangmanBuilder.setUpdateController(updateController);

            if (event.getName().equals("multi")) {
                User user = null;
                if (event instanceof SlashCommandInteractionEvent) {
                    user = event.getOption("user", OptionMapping::getAsUser);
                } else if (event instanceof UserContextInteractionEvent userContextEvent) {
                    Member targetMember = userContextEvent.getTargetMember();
                    if (targetMember == null) {
                        event.reply("User is `null`").setEphemeral(true).queue();
                        return;
                    }
                    user = targetMember.getUser();
                }

                if (user == null || event.getGuild() == null) {
                    event.reply("User is `null`").setEphemeral(true).queue();
                    return;
                } else if (user.isBot()) {
                    String playWithBot = jsonParsers.getLocale("play_with_bot", userIdLong);
                    event.reply(playWithBot).setEphemeral(true).queue();
                    return;
                } else if (user.getIdLong() == userIdLong) {
                    String playWithYourself = jsonParsers.getLocale("play_with_yourself", userIdLong);
                    event.reply(playWithYourself).setEphemeral(true).queue();
                    return;
                } else if (instance.hasHangman(user.getIdLong())) {
                    String secondPlayerAlreadyPlaying = jsonParsers.getLocale("second_player_already_playing", userIdLong);
                    event.reply(secondPlayerAlreadyPlaying).setEphemeral(true).queue();
                    return;
                } else {
                    HangmanPlayer hangmanPlayerSecond = new HangmanPlayer(user.getIdLong(), guildIdLong, channelIdLong);
                    hangmanBuilder.addHangmanPlayer(hangmanPlayerSecond);
                }
            }

            String createGame = jsonParsers.getLocale("create_game", userIdLong);
            event.reply(createGame).setEphemeral(true).queue();

            Hangman hangman = hangmanBuilder.build();

            //Заполнение коллекции
            instance.setHangman(userIdLong, hangman);

            if (hangman.getHangmanPlayers().length > 1) {
                HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
                HangmanPlayer hangmanPlayerSecond = hangmanPlayers[1];
                instance.setHangman(hangmanPlayerSecond.getUserId(), hangman);
            }

            hangman.startGame(event.getMessageChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
        }
    }
}
