package main.core.events;

import main.config.BotStartConfig;
import main.controller.UpdateController;
import main.hangman.Hangman;
import main.hangman.HangmanBuilder;
import main.hangman.HangmanRegistry;
import main.hangman.impl.ButtonIMpl;
import main.jsonparser.JSONParsers;
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
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            slashEvent.getChannel().sendTyping().queue();
        }
        //Проверяем установлен ли язык. Если нет - то возвращаем в чат ошибку
        Map<Long, String> mapGameLanguages = BotStartConfig.getMapGameLanguages();
        if (!mapGameLanguages.containsKey(userIdLong)) {
            String hangmanListenerNeedSetLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userIdLong);

            EmbedBuilder needSetLanguage = new EmbedBuilder();
            needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
            needSetLanguage.setColor(Color.GREEN);
            needSetLanguage.setDescription(hangmanListenerNeedSetLanguage);

            event.replyEmbeds(needSetLanguage.build())
                    .addActionRow(ButtonIMpl.BUTTON_RUSSIAN, ButtonIMpl.BUTTON_ENGLISH)
                    .addActionRow(ButtonIMpl.BUTTON_PLAY_AGAIN)
                    .queue();
            return;
            //Проверяем если игрок уже играет. То присылаем в чат уведомление
        } else if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
            String hangmanListenerYouPlay = jsonParsers.getLocale("Hangman_Listener_You_Play", userIdLong);

            EmbedBuilder youPlay = new EmbedBuilder();
            youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
            youPlay.setColor(Color.GREEN);
            youPlay.setDescription(hangmanListenerYouPlay);

            event.replyEmbeds(youPlay.build()).addActionRow(ButtonIMpl.BUTTON_STOP).queue();
            //Если всё хорошо, создаем игру
        } else {
            HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder()
                    .setUserIdLong(userIdLong)
                    .setChannelId(event.getMessageChannel().getIdLong())
                    .setUpdateController(updateController)
                    .setUserIdLong(userIdLong);

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
                } else if (HangmanRegistry.getInstance().hasHangman(user.getIdLong())) {
                    String secondPlayerAlreadyPlaying = jsonParsers.getLocale("second_player_already_playing", userIdLong);
                    event.reply(secondPlayerAlreadyPlaying).setEphemeral(true).queue();
                    return;
                } else {
                    hangmanBuilder.setSecondUserIdLong(user.getIdLong());
                }
            }

            if (event.getGuild() != null) {
                hangmanBuilder.setGuildIdLong(event.getGuild().getIdLong());
            } else {
                hangmanBuilder.setGuildIdLong(null);
            }

            String createGame = jsonParsers.getLocale("create_game", userIdLong);
            event.reply(createGame).setEphemeral(true).queue();

            Hangman hangman = hangmanBuilder.build();

            //Заполнение коллекции
            HangmanRegistry.getInstance().setHangman(userIdLong, hangman);
            if (hangmanBuilder.getSecondPlayer() != 0) {
                HangmanRegistry.getInstance().setHangman(hangmanBuilder.getSecondPlayer(), hangman);
            }

            hangman.startGame(event.getMessageChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
        }
    }
}
