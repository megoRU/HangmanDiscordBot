package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.model.repository.CategoryRepository;
import main.model.repository.GameLanguageRepository;
import main.model.repository.GamesRepository;
import main.model.repository.LanguageRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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
    private final CategoryRepository categoryRepository;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;

            if (event.isFromType(ChannelType.TEXT) && !event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_SEND))
                return;

            buildMessage(event.getChannel(), event.getAuthor(), event.getMessage().getContentRaw());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildMessage(@NotNull SlashCommandInteractionEvent event, @NotNull User user) {
        String code = UUID.randomUUID().toString().replaceAll("-", "");
        BotStartConfig.getSecretCode().put(user.getIdLong(), code);

        String restoreData = jsonParsers.getLocale("restore_Data", user.getIdLong());
        String restoreDataPm = String.format(jsonParsers.getLocale("restore_Data_PM", user.getIdLong()), code);

        event.reply(restoreData).queue();


        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessage(restoreDataPm))
                .queue(null, (exception) -> event.getChannel().sendMessage("""
                        I couldn't send you a message to the DM.
                        You may have banned sending you messages
                        or I am on your blacklist""").queue());
    }

    public void buildMessage(@NotNull MessageChannel messageChannel, @NotNull User user, String message) {

        String[] split = message.split(" ", 2);

        if (message.equals(DELETE)) {
            String code = UUID.randomUUID().toString().replaceAll("-", "");
            BotStartConfig.getSecretCode().put(user.getIdLong(), code);

            String restoreData = jsonParsers.getLocale("restore_Data", user.getIdLong());
            String restoreDataPm = String.format(jsonParsers.getLocale("restore_Data_PM", user.getIdLong()), code);

            messageChannel.sendMessage(restoreData).queue();


            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessage(restoreDataPm))
                    .queue();
            return;
        }

        if (message.matches(DELETE_WITH_CODE)
                && (BotStartConfig.getSecretCode().get(user.getIdLong()) == null || !BotStartConfig.getSecretCode().get(user.getIdLong()).equals(split[1]))) {
            String restoreDataFailure = jsonParsers.getLocale("restore_Data_Failure", user.getIdLong());

            messageChannel.sendMessage(restoreDataFailure).queue();
            return;
        }

        if (split.length > 1 && message.matches(DELETE_WITH_CODE)
                && BotStartConfig.getSecretCode().get(user.getIdLong()) != null
                && BotStartConfig.getSecretCode().get(user.getIdLong()).equals(split[1])) {

            String restoreDataSuccess = jsonParsers.getLocale("restore_Data_Success", user.getIdLong());

            messageChannel.sendMessage(restoreDataSuccess).queue();
            BotStartConfig.getMapGameLanguages().remove(user.getIdLong());
            BotStartConfig.getMapLanguages().remove(user.getIdLong());

            gamesRepository.deleteAllMyData(user.getIdLong());
            languageRepository.deleteLanguage(user.getIdLong());
            gameLanguageRepository.deleteGameLanguage(user.getIdLong());
            categoryRepository.deleteCategory(user.getIdLong());
        }
    }
}