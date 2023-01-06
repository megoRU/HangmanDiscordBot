package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.hangman.Hangman;
import main.hangman.HangmanBuilder;
import main.hangman.HangmanRegistry;
import main.hangman.impl.ButtonIMpl;
import main.jsonparser.JSONParsers;
import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.PlayerRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Map;

@AllArgsConstructor
public class ContextMenuListener extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        if (event.getName().equals("multi")) {
            Member targetMember = event.getTargetMember();
            if (targetMember == null) {
                event.reply("User is `null`").setEphemeral(true).queue();
                return;
            }

            User destUser = targetMember.getUser();
            User user = event.getUser();
            long userIdLong = user.getIdLong();

            Map<Long, String> mapGameLanguages = BotStartConfig.getMapGameLanguages();
            if (!mapGameLanguages.containsKey(user.getIdLong())) {
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
            } else if (HangmanRegistry.getInstance().hasHangman(user.getIdLong())) {
                String hangmanListenerYouPlay = jsonParsers.getLocale("Hangman_Listener_You_Play", userIdLong);

                EmbedBuilder youPlay = new EmbedBuilder();
                youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                youPlay.setColor(Color.GREEN);
                youPlay.setDescription(hangmanListenerYouPlay);

                event.replyEmbeds(youPlay.build()).addActionRow(ButtonIMpl.BUTTON_STOP).queue();
                return;
            }

            if (destUser.isBot()) {
                String playWithBot = jsonParsers.getLocale("play_with_bot", userIdLong);
                event.reply(playWithBot).setEphemeral(true).queue();
            } else if (user.getIdLong() == destUser.getIdLong()) {
                String playWithYourself = jsonParsers.getLocale("play_with_yourself", userIdLong);
                event.reply(playWithYourself).setEphemeral(true).queue();
            } else if (HangmanRegistry.getInstance().hasHangman(destUser.getIdLong())) {
                String secondPlayerAlreadyPlaying = jsonParsers.getLocale("second_player_already_playing", userIdLong);
                event.reply(secondPlayerAlreadyPlaying).setEphemeral(true).queue();
            } else {
                //Если всё хорошо, создаем игру
                HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder()
                        .setChannelId(event.getChannel().getIdLong()) //NPE
                        .setHangmanGameRepository(hangmanGameRepository)
                        .setGamesRepository(gamesRepository)
                        .setPlayerRepository(playerRepository)
                        .setGuildIdLong(event.getGuild().getIdLong()) //NO NPE
                        .setUserIdLong(user.getIdLong())
                        .setSecondUserIdLong(destUser.getIdLong());

                String createGame = jsonParsers.getLocale("create_game", userIdLong);
                event.reply(createGame).setEphemeral(true).queue();

                Hangman hangman = hangmanBuilder.build();
                //Заполнение коллекции
                HangmanRegistry.getInstance().setHangman(user.getIdLong(), hangman);
                HangmanRegistry.getInstance().setHangman(destUser.getIdLong(), hangman);
                hangman.startGame(event.getMessageChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
            }
        }
    }
}