package main.core.events;

import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeleteCommand {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    public void delete(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();

        String code = UUID.randomUUID().toString().replaceAll("-", "");
        BotStartConfig.getSecretCode().put(userIdLong, code);

        String restoreData = jsonParsers.getLocale("restore_Data", userIdLong);
        String restoreDataPm = String.format(jsonParsers.getLocale("restore_Data_PM", userIdLong), code);

        event.reply(restoreData).queue();

        event.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendMessage(restoreDataPm))
                .queue(null, (exception) -> event.getChannel().sendMessage(
                        """
                                I couldn't send you a message to the DM.
                                You may have banned sending you messages
                                or I am on your blacklist
                                """).queue());
    }
}
