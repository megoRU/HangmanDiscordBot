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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
@AllArgsConstructor
public class CompetitivePlayGPT {

    private final static Logger LOGGER = LoggerFactory.getLogger(CompetitivePlayGPT.class.getName());
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final HangmanAPI hangmanAPI;
    private final HangmanDataSaving hangmanDataSaving;
    private final static HangmanRegistry instance = HangmanRegistry.getInstance();

    public void competitive(@NotNull ButtonInteractionEvent event) {
        var userId = event.getUser().getIdLong();
        long messageChannel = event.getMessageChannel().getIdLong();

        String gameLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getIdLong());

        Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
        UserSettings userSettings = userSettingsMap.get(userId);

        if (userSettings == null) {
            event.getHook().sendMessage(gameLanguage)
                    .addActionRow(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH)
                    .addActionRow(HangmanUtils.getButtonPlayCompetitiveAgain(userId))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        UserSettings.GameLanguage userGameLanguage = userSettings.getGameLanguage();

        if (!instance.hasHangman(userId) && !instance.hasHangman(-userId)) {
            HangmanBuilder.Builder hangmanUser = new HangmanBuilder.Builder();
            try {
                String word = hangmanAPI.getWord(userId);
                HangmanBuilder.Builder hangmanBuilderGPT = new HangmanBuilder.Builder();
                HangmanPlayer hangmanPlayerGPT = new HangmanPlayer(-userId, null, null, userGameLanguage);
                HangmanPlayer hangmanPlayer = new HangmanPlayer(userId, event.getGuild() != null ? event.getGuild().getIdLong() : null, messageChannel, userGameLanguage);

                hangmanBuilderGPT.setCompetitive(true);
                hangmanBuilderGPT.addHangmanPlayer(hangmanPlayerGPT);
                hangmanBuilderGPT.setAgainstPlayerId(userId);

                hangmanUser.addHangmanPlayer(hangmanPlayer);
                hangmanUser.setAgainstPlayerId(-userId);
                hangmanUser.setCompetitive(true);

                //Build
                Hangman build = hangmanUser.build();
                Hangman buildGPT = hangmanBuilderGPT.build();

                instance.setHangman(userId, build);
                instance.setHangman(-userId, buildGPT);

                String createGame = jsonParsers.getLocale("create_game", userId);
                event.getHook().sendMessage(createGame).queue();

                build.startGame(event.getMessageChannel(), word, hangmanDataSaving);
                buildGPT.startGame(word, hangmanDataSaving);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        } else if (instance.hasHangman(userId)) {
            String youArePlayNow = jsonParsers.getLocale("Hangman_Listener_You_Play", event.getUser().getIdLong());
            event.getHook().sendMessage(youArePlayNow)
                    .setActionRow(HangmanUtils.getButtonStop(userId))
                    .queue();
        } else if (instance.hasHangman(-userId)) {
            String hangmanBotPlay = jsonParsers.getLocale("hangman_bot_play", event.getUser().getIdLong());
            event.getHook().sendMessage(hangmanBotPlay)
                    .setActionRow(HangmanUtils.getButtonGPT(userId))
                    .queue();
        }
    }
}
