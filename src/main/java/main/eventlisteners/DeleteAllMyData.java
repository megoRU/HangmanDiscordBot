package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.model.repository.GameLanguageRepository;
import main.model.repository.GameModeRepository;
import main.model.repository.GamesRepository;
import main.model.repository.LanguageRepository;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.UUID;

@AllArgsConstructor
@Service
public class DeleteAllMyData extends ListenerAdapter {

    private static final String DELETE = "!delete";
    private static final String DELETE_WITH_CODE = "!delete\\s.+";
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final GamesRepository gamesRepository;
    private final LanguageRepository languageRepository;
    private final GameLanguageRepository gameLanguageRepository;
    private final GameModeRepository gameModeRepository;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;

            if (event.isFromType(ChannelType.TEXT)) {
                if (CheckPermissions.isHasPermissionToWrite(event.getTextChannel())) return;
            }

            buildMessage(event.getChannel(), event.getAuthor(), event.getMessage().getContentRaw());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildMessage(@NotNull SlashCommandInteractionEvent event, @NotNull User user) {

        String code = UUID.randomUUID().toString().replaceAll("-", "");
        BotStartConfig.getSecretCode().put(user.getId(), code);

        event.reply(jsonParsers.getLocale("restore_Data", user.getId())).queue();

        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessage(
                        jsonParsers.getLocale("restore_Data_PM", user.getId()).replaceAll("\\{0}", code)))
                .queue(null, (exception) -> event.getChannel().sendMessage("I couldn't send you a message to the DM." +
                        "\nYou may have banned sending you messages\nor I am on your blacklist").queue());
    }

    public void buildMessage(@NotNull MessageChannel messageChannel, @NotNull User user, String message) {

        String[] split = message.split(" ", 2);

        if (message.equals(DELETE)) {
            String code = UUID.randomUUID().toString().replaceAll("-", "");
            BotStartConfig.getSecretCode().put(user.getId(), code);

            messageChannel.sendMessage(jsonParsers.getLocale("restore_Data", user.getId())).queue();

            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessage(
                            jsonParsers.getLocale("restore_Data_PM", user.getId()).replaceAll("\\{0}", code)))
                    .queue();
            return;
        }

        if (message.matches(DELETE_WITH_CODE)
                && (BotStartConfig.getSecretCode().get(user.getId()) == null || !BotStartConfig.getSecretCode().get(user.getId()).equals(split[1]))) {
            messageChannel.sendMessage(jsonParsers.getLocale("restore_Data_Failure", user.getId())).queue();
            return;
        }

        if (split.length > 1 && message.matches(DELETE_WITH_CODE)
                && BotStartConfig.getSecretCode().get(user.getId()) != null
                && BotStartConfig.getSecretCode().get(user.getId()).equals(split[1])) {

            messageChannel.sendMessage(jsonParsers.getLocale("restore_Data_Success", user.getId())).queue();
            BotStartConfig.getMapGameLanguages().remove(user.getId());
            BotStartConfig.getMapLanguages().remove(user.getId());

            gamesRepository.deleteAllMyData(user.getIdLong());
            languageRepository.deleteLanguage(user.getId());
            gameLanguageRepository.deleteGameLanguage(user.getId());
            gameModeRepository.deleteGameMode(user.getId());
        }
    }
}