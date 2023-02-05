package main.core.events;

import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.model.repository.CategoryRepository;
import main.model.repository.GameLanguageRepository;
import main.model.repository.GamesRepository;
import main.model.repository.LanguageRepository;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeleteMessage {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final GamesRepository gamesRepository;
    private final LanguageRepository languageRepository;
    private final GameLanguageRepository gameLanguageRepository;
    private final CategoryRepository categoryRepository;

    @Autowired
    public DeleteMessage(GamesRepository gamesRepository, LanguageRepository languageRepository, GameLanguageRepository gameLanguageRepository, CategoryRepository categoryRepository) {
        this.gamesRepository = gamesRepository;
        this.languageRepository = languageRepository;
        this.gameLanguageRepository = gameLanguageRepository;
        this.categoryRepository = categoryRepository;
    }

    public void delete(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        var userIdLong = event.getAuthor().getIdLong();
        MessageChannelUnion channel = event.getChannel();
        String[] split = message.split(" ", 2);
        String secretCode = BotStartConfig.getSecretCode().get(userIdLong);

        if (secretCode != null && secretCode.equals(split[1])) {
            String restoreDataSuccess = jsonParsers.getLocale("restore_Data_Success", userIdLong);

            channel.sendMessage(restoreDataSuccess).queue();
            BotStartConfig.getMapGameLanguages().remove(userIdLong);
            BotStartConfig.getMapLanguages().remove(userIdLong);
            BotStartConfig.getSecretCode().remove(userIdLong);

            gamesRepository.deleteAllMyData(userIdLong);
            languageRepository.deleteLanguage(userIdLong);
            gameLanguageRepository.deleteGameLanguage(userIdLong);
            categoryRepository.deleteCategory(userIdLong);
        }
    }
}