package hangman;

import db.DataBase;
import jsonparser.JSONParsers;
import messagesevents.CheckPermissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class GameHangmanListener extends ListenerAdapter {

    private static final String HG = "!hg";
    private static final String HG_STOP = "!hg stop";
    private static final String HG_ONE_LETTER = "[А-ЯЁа-яё]";
    private static final String HG_ONE_LETTER_ENG = "[A-Za-z]";
    private static final JSONParsers jsonParsers = new JSONParsers();

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (CheckPermissions.isHasPermissionsWriteAndEmbedLinks(event.getChannel())) {
            return;
        }

        String message = event.getMessage().getContentRaw().trim().toLowerCase();

        String prefix = HG;
        String prefix2 = HG_STOP;

        if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) + "hg";
            prefix2 = BotStart.getMapPrefix().get(event.getGuild().getId()) + "hg stop";
        }

        long userIdLong = event.getAuthor().getIdLong();
        if ((message.matches(HG_ONE_LETTER) || message.matches(HG_ONE_LETTER_ENG)) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
            HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).logic(message, event.getMessage());
            return;
        }

        if (message.equals(prefix) || message.equals(prefix2)) {
            if (BotStart.getMapGameLanguages().get(event.getAuthor().getId()) == null) {
                EmbedBuilder needSetLanguage = new EmbedBuilder();

                needSetLanguage.setAuthor(event.getAuthor().getName(), null, event.getAuthor().getAvatarUrl());
                needSetLanguage.setColor(0x00FF00);
                needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getAuthor().getId()));

                event.getChannel().sendMessageEmbeds(needSetLanguage.build())
                        .setActionRow(
                                Button.secondary(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_RUS, "Кириллица")
                                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")),

                                Button.secondary(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_ENG, "Latin")
                                        .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")),
                                Button.success(event.getGuild().getId() + ":" + ReactionsButton.START_NEW_GAME, "Play"))
                        .queue();

                return;
            }

            if (message.equals(prefix) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.getChannel().sendTyping().queue();
                EmbedBuilder youPlay = new EmbedBuilder();

                youPlay.setAuthor(event.getAuthor().getName(), null, event.getAuthor().getAvatarUrl());
                youPlay.setColor(0x00FF00);
                youPlay.setDescription(jsonParsers.getLocale("Hangman_Listener_You_Play",
                        event.getAuthor().getId()).replaceAll("\\{0}", prefix));

                event.getChannel().sendMessageEmbeds(youPlay.build())
                        .setActionRow(Button.danger(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_STOP, "Stop game"))
                        .queue();
                return;
            }

            if (message.equals(prefix2) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                HangmanRegistry.getInstance().getActiveHangman().remove(userIdLong);

                event.getChannel().sendMessage(jsonParsers.getLocale("Hangman_Eng_game",
                                event.getAuthor().getId()).replaceAll("\\{0}", prefix))
                        .setActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.START_NEW_GAME, "Play again"))
                        .queue();
                DataBase.getInstance().deleteActiveGame(String.valueOf(userIdLong));
                return;
            }

            if (message.equals(prefix2) && !HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.getChannel().sendMessage(jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getAuthor().getId()))
                        .setActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.START_NEW_GAME, "Play again"))
                        .queue();
                return;
            }

            if (!HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.getChannel().sendTyping().queue();
                HangmanRegistry.getInstance().setHangman(userIdLong, new Hangman(event.getAuthor().getId(), event.getGuild().getId(), event.getChannel().getIdLong()));
                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).startGame(event.getChannel(), event.getAuthor().getAvatarUrl(), event.getAuthor().getName());
            }
        }
    }
}