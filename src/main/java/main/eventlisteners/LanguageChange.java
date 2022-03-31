package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.model.entity.Language;
import main.model.repository.LanguageRepository;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class LanguageChange extends ListenerAdapter {

    private static final String LANG_RUS = "!lang rus";
    private static final String LANG_ENG = "!lang eng";
    private final JSONParsers jsonParsers = new JSONParsers();
    private final LanguageRepository languageRepository;

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

            if (BotStartConfig.getMapPrefix().containsKey(event.getGuild().getId())) {
                prefix_LANG_RUS = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "lang rus";
                prefix_LANG_ENG = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "lang eng";
            }

            if (message.equals(prefix_LANG_RUS) || message.equals(prefix_LANG_ENG)) {
                BotStartConfig.getMapLanguages().put(event.getAuthor().getId(), messages[1]);

                languageRepository.deleteLanguage(event.getAuthor().getId());
                Language languageRepo = new Language();
                languageRepo.setUserIdLong(event.getAuthor().getId());
                languageRepo.setLanguage(messages[1]);

                languageRepository.save(languageRepo);

                String language;

                if (messages[1].equals("rus")) {
                    language = "Русский";
                } else {
                    language = "English";
                }

                event.getChannel()
                        .sendMessage(jsonParsers.getLocale("language_change_lang", event.getAuthor().getId())
                                .replaceAll("\\{0}", language)).queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
