package main.core.events;

import main.config.BotStartConfig;
import main.controller.UpdateController;
import main.enums.Buttons;
import main.hangman.*;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class HangmanButton {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    public void hangman(@NotNull ButtonInteractionEvent event, UpdateController updateController) {
        event.editButton(event.getButton().asDisabled()).queue();

        var userIdLong = event.getUser().getIdLong();
        var channelIdLong = event.getChannel().getIdLong();

        String gameLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getIdLong());
        String userGameLanguage = BotStartConfig.getMapGameLanguages().get(userIdLong);

        if (userGameLanguage == null) {
            event.getHook().sendMessage(gameLanguage)
                    .addActionRow(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH)
                    .addActionRow(HangmanUtils.BUTTON_PLAY_AGAIN)
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!HangmanRegistry.getInstance().hasHangman(userIdLong)) {
            event.getChannel().sendTyping().queue();

            HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder();
            hangmanBuilder.setUpdateController(updateController);

            Hangman hangman;
            //Guild Play
            boolean matches = event.getButton().getId().matches("BUTTON_START_NEW_GAME_\\d+_\\d+");
            boolean isFromGuild = event.getGuild() != null;

            System.out.println("isFromGuild: " + isFromGuild);
            System.out.println("matches: " + matches);

            if (event.getGuild() != null) {
                long guildIdLong = event.getGuild().getIdLong();

                HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, guildIdLong, channelIdLong);
                hangmanBuilder.addHangmanPlayer(hangmanPlayer);

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

                    HangmanPlayer hangmanPlayerSecond = new HangmanPlayer(secondUser, guildIdLong, channelIdLong);
                    hangmanBuilder.addHangmanPlayer(hangmanPlayerSecond);

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
                HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, null, channelIdLong);
                hangmanBuilder.addHangmanPlayer(hangmanPlayer);

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
    }
}

