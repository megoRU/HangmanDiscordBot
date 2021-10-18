package messagesevents;

import db.DataBase;
import hangman.HangmanRegistry;
import hangman.ReactionsButton;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class GameLanguageChange extends ListenerAdapter {

    private static final String LANG_RUS = "!game rus";
    private static final String LANG_ENG = "!game eng";
    private final JSONParsers jsonParsers = new JSONParsers();

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (!new CheckPermissions(event.getChannel()).checkMessageWriteAndEmbedLinks()) {
            return;
        }

        String message = event.getMessage().getContentRaw().toLowerCase().trim();
        String[] messages = message.split(" ", 2);
        String prefix_LANG_RUS = LANG_RUS;
        String prefix_LANG_ENG = LANG_ENG;


        if (BotStart.getMapPrefix().containsKey(event.getAuthor().getId())) {
            prefix_LANG_RUS = BotStart.getMapPrefix().get(event.getGuild().getId()) + "game rus";
            prefix_LANG_ENG = BotStart.getMapPrefix().get(event.getGuild().getId()) + "game eng";
        }

        if (message.equals(prefix_LANG_RUS) || message.equals(prefix_LANG_ENG)) {
            if (HangmanRegistry.getInstance().hasHangman(event.getAuthor().getIdLong())) {
                EmbedBuilder whenPlay = new EmbedBuilder();

                whenPlay.setAuthor(event.getAuthor().getName(), null, event.getAuthor().getAvatarUrl());
                whenPlay.setColor(0x00FF00);
                whenPlay.setDescription(jsonParsers.getLocale("ReactionsButton_When_Play", event.getAuthor().getId()));

                event.getChannel().sendMessageEmbeds(whenPlay.build())
                        .setActionRow(Button.danger(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_STOP, "Stop game"))
                        .queue();
            } else {
                BotStart.getMapGameLanguages().put(event.getAuthor().getId(), messages[1]);
                DataBase.getInstance().addGameLanguageToDB(event.getAuthor().getId(), messages[1]);
                event.getChannel()
                        .sendMessage(jsonParsers.getLocale("language_change_lang", event.getAuthor().getId())
                                + "`" + messages[1].toUpperCase() + "`").queue();
            }
        }
    }

    public void changeGameLanguage(String message, String userIdLong) {
        BotStart.getMapGameLanguages().put(userIdLong, message);
        DataBase.getInstance().addGameLanguageToDB(userIdLong, message);
    }
}

