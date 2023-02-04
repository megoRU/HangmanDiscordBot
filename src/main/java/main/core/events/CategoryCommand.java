package main.core.events;

import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.model.entity.Category;
import main.model.repository.CategoryRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryCommand {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryCommand(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void category(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();

        String categorySlash = event.getOption("category", OptionMapping::getAsString);
        String gameCategory = jsonParsers.getLocale("game_category", userIdLong);

        if (categorySlash != null && categorySlash.equals("any")) {
            BotStartConfig.mapGameCategory.remove(userIdLong);
            categoryRepository.deleteCategory(userIdLong);
            event.reply(gameCategory).setEphemeral(true).queue();
            return;
        }
        Category category = new Category();
        category.setUserIdLong(userIdLong);
        category.setCategory(categorySlash);
        categoryRepository.save(category);
        BotStartConfig.mapGameCategory.put(userIdLong, categorySlash);
        event.reply(gameCategory).setEphemeral(true).queue();
    }
}
