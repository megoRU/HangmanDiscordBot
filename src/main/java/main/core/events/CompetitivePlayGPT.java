package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.game.Hangman;
import main.game.HangmanBuilder;
import main.game.HangmanDataSaving;
import main.game.HangmanPlayer;
import main.game.api.HangmanAPI;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CompetitivePlayGPT {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final HangmanAPI hangmanAPI;
    private final HangmanDataSaving hangmanDataSaving;

    public void competitive(@NotNull ButtonInteractionEvent event) {
        var userId = event.getUser().getIdLong();
        long messageChannel = event.getMessageChannel().getIdLong();

        String gameLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getIdLong());
        UserSettings.GameLanguage userGameLanguage = BotStartConfig.getMapGameLanguages().get(userId);

        if (userGameLanguage == null) {
            event.reply(gameLanguage)
                    .addActionRow(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH)
                    .addActionRow(HangmanUtils.getButtonPlayCompetitiveAgain(userId))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        HangmanRegistry hangmanRegistry = HangmanRegistry.getInstance();


        if (!hangmanRegistry.hasHangman(userId) && !hangmanRegistry.hasHangman(-userId)) {
            HangmanBuilder.Builder hangmanUser = new HangmanBuilder.Builder();


            try {
                String word = hangmanAPI.getWord(userId);

                HangmanBuilder.Builder hangmanBuilderGPT = new HangmanBuilder.Builder();
                HangmanPlayer hangmanPlayerGPT = new HangmanPlayer(-userId, null, null);
                HangmanPlayer hangmanPlayer = new HangmanPlayer(userId, null, messageChannel, userGameLanguage);

                hangmanBuilderGPT.setCompetitive(true);
                hangmanBuilderGPT.addHangmanPlayer(hangmanPlayerGPT);
                hangmanBuilderGPT.setAgainstPlayerId(userId);

                hangmanUser.addHangmanPlayer(hangmanPlayer);
                hangmanUser.setAgainstPlayerId(-userId);
                hangmanUser.setCompetitive(true);

                //Build
                Hangman build = hangmanUser.build();
                Hangman buildGPT = hangmanBuilderGPT.build();

                hangmanRegistry.setHangman(userId, build);
                hangmanRegistry.setHangman(-userId, buildGPT);


                String createGame = jsonParsers.getLocale("create_game", userId);
                event.reply(createGame).queue();

                build.startGame(event.getMessageChannel(), word, hangmanDataSaving);
                buildGPT.startGame(word, hangmanDataSaving);
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        } else if (hangmanRegistry.hasHangman(userId)) {
            String youArePlayNow = jsonParsers.getLocale("Hangman_Listener_You_Play", event.getUser().getIdLong());
            event.reply(youArePlayNow)
                    .setActionRow(HangmanUtils.getButtonStop(userId))
                    .queue();
        } else if (hangmanRegistry.hasHangman(-userId)) {
            String hangmanBotPlay = jsonParsers.getLocale("hangman_bot_play", event.getUser().getIdLong());
            event.reply(hangmanBotPlay)
                    .setActionRow(HangmanUtils.getButtonStop(userId))
                    .queue();
        }
    }
}
