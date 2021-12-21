package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.model.repository.GamesRepository;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@AllArgsConstructor
public class DeleteAllMyData extends ListenerAdapter {

    private static final String DELETE = "!delete";
    private static final String DELETE_WITH_CODE = "!delete\\s.+";
    private final JSONParsers jsonParsers = new JSONParsers();
    private final GamesRepository gamesRepository;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        try {
            if (event.isFromType(ChannelType.TEXT)) {
                if (CheckPermissions.isHasPermissionToWrite(event.getTextChannel())) return;
            }

            buildMessage(event, event.getMessage().getContentRaw(), event.getAuthor().getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildMessage(@NotNull MessageReceivedEvent event, String message, String authorId) {

        String[] split = message.split(" ", 2);

        if (message.equals(DELETE)) {
            String code = UUID.randomUUID().toString().replaceAll("-", "");
            BotStartConfig.getSecretCode().put(authorId, code);

            event.getChannel().sendMessage(jsonParsers.getLocale("restore_Data", authorId)).queue();

            event.getAuthor().openPrivateChannel()
                    .flatMap(channel -> channel.sendMessage(
                            jsonParsers.getLocale("restore_Data_PM", authorId).replaceAll("\\{0}", code)))
                    .queue();
            return;
        }

        if (message.matches(DELETE_WITH_CODE)
                && (BotStartConfig.getSecretCode().get(authorId) == null || !BotStartConfig.getSecretCode().get(authorId).equals(split[1]))) {
            event.getChannel().sendMessage(jsonParsers.getLocale("restore_Data_Failure", authorId)).queue();
            return;
        }

        if (split.length > 1 && message.matches(DELETE_WITH_CODE)
                && BotStartConfig.getSecretCode().get(authorId) != null
                && BotStartConfig.getSecretCode().get(authorId).equals(split[1])) {
            event.getChannel().sendMessage(jsonParsers.getLocale("restore_Data_Success", authorId)).queue();
            BotStartConfig.getMapGameLanguages().remove(authorId);
            BotStartConfig.getMapLanguages().remove(authorId);

            gamesRepository.deleteAllMyData(event.getAuthor().getIdLong());
        }
    }
}