package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.enums.Buttons;
import main.hangman.HangmanRegistry;
import main.jsonparser.JSONParsers;
import main.model.entity.GameLanguage;
import main.model.repository.GameLanguageRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class GameLanguageChange extends ListenerAdapter {

    private static final String LANG_RUS = "!game rus";
    private static final String LANG_ENG = "!game eng";
    private final JSONParsers jsonParsers = new JSONParsers();
    private final GameLanguageRepository gameLanguageRepository;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;

            if (!event.isFromType(ChannelType.TEXT)) return;

            if (CheckPermissions.isHasPermissionsWriteAndEmbedLinks(event.getTextChannel())) return;

            String message = event.getMessage().getContentRaw().toLowerCase().trim();
            String[] messages = message.split(" ", 2);
            String prefix_LANG_RUS = LANG_RUS;
            String prefix_LANG_ENG = LANG_ENG;


            if (BotStartConfig.getMapPrefix().containsKey(event.getAuthor().getId())) {
                prefix_LANG_RUS = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "game rus";
                prefix_LANG_ENG = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "game eng";
            }

            if (message.equals(prefix_LANG_RUS) || message.equals(prefix_LANG_ENG)) {
                if (HangmanRegistry.getInstance().hasHangman(event.getAuthor().getIdLong())) {
                    EmbedBuilder whenPlay = new EmbedBuilder();

                    whenPlay.setAuthor(event.getAuthor().getName(), null, event.getAuthor().getAvatarUrl());
                    whenPlay.setColor(0x00FF00);
                    whenPlay.setDescription(jsonParsers.getLocale("ReactionsButton_When_Play", event.getAuthor().getId()));

                    event.getChannel().sendMessageEmbeds(whenPlay.build())
                            .setActionRow(Button.danger(Buttons.BUTTON_STOP.name(), "Stop game"))
                            .queue();
                } else {
                    BotStartConfig.getMapGameLanguages().put(event.getAuthor().getId(), messages[1]);

                    changeGameLanguage(messages[1], event.getAuthor().getId());

                    event.getChannel()
                            .sendMessage(jsonParsers.getLocale("ReactionsButton_Save", event.getAuthor().getId())
                                    .replaceAll("\\{0}", messages[1].equals("rus") ? "Русский" : "English")).queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeGameLanguage(String message, String userIdLong) {
        try {
            BotStartConfig.getMapGameLanguages().put(userIdLong, message);
            GameLanguage gameLanguage = new GameLanguage();
            gameLanguage.setUserIdLong(userIdLong);
            gameLanguage.setLanguage(message);
            gameLanguageRepository.save(gameLanguage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

