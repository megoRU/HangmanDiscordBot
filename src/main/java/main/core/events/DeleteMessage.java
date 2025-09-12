package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.game.core.HangmanRegistry;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import main.model.repository.GamesRepository;
import main.model.repository.UserSettingsRepository;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class DeleteMessage {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final GamesRepository gamesRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final static HangmanRegistry instance = HangmanRegistry.getInstance();

    public void delete(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        var userIdLong = event.getAuthor().getIdLong();
        MessageChannelUnion channel = event.getChannel();
        String[] split = message.split(" ", 2);
        String secretCode = BotStartConfig.getSecretCode().get(userIdLong);

        if (secretCode != null && secretCode.length() > 1 && secretCode.equals(split[1])) {
            String restoreDataSuccess = jsonParsers.getLocale("restore_Data_Success", userIdLong);
            channel.sendMessage(restoreDataSuccess).queue();

            Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;

            userSettingsMap.remove(userIdLong);
            BotStartConfig.getSecretCode().remove(userIdLong);
            instance.removeHangman(userIdLong);

            gamesRepository.deleteGameByUserIdLong(userIdLong);
            userSettingsRepository.deleteByUserIdLong(userIdLong);
        } else {
            String errorsTitle = jsonParsers.getLocale("errors_title", userIdLong);
            channel.sendMessage(errorsTitle).queue();
        }
    }
}