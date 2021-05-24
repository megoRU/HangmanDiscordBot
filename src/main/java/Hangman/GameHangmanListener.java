package Hangman;

import jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class GameHangmanListener extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();
    private static final String HG = "!hg";
    private static final String HG_STOP = "!hg stop";
    private static final String HG_ONE_LETTER = "[А-Яа-я]";
    private static final String HG_ONE_LETTER_ENG = "[A-Za-z]";

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
            return;
        }
        String message = event.getMessage().getContentRaw().trim().toLowerCase();

        String prefix = HG;
        String prefix2 = HG_STOP;
        String prefix3 = "!";

        if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) + "hg";
            prefix2 = BotStart.getMapPrefix().get(event.getGuild().getId()) + "hg stop";
            prefix3 = BotStart.getMapPrefix().get(event.getGuild().getId());
        }


        long userIdLong = event.getAuthor().getIdLong();
        if ((message.matches(HG_ONE_LETTER) || message.matches(HG_ONE_LETTER_ENG)) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
            HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).logic(message);
            return;
        }

        if (message.equals(prefix) || message.equals(prefix2)) {
            if (BotStart.getMapGameLanguages().get(event.getAuthor().getId()) == null) {
                event.getChannel().sendMessage(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language",
                        event.getAuthor().getId()).replaceAll("\\{0}", prefix3)).queue();
                return;
            }

            if (message.equals(prefix) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.getChannel().sendMessage(jsonParsers.getLocale("Hangman_Listener_You_Play",
                        event.getAuthor().getId()).replaceAll("\\{0}", prefix)).queue();
                return;
            }

            if (message.equals(prefix2) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                HangmanRegistry.getInstance().getActiveHangman().remove(userIdLong);
                event.getChannel().sendMessage(jsonParsers.getLocale("Hangman_Eng_game",
                        event.getAuthor().getId()).replaceAll("\\{0}", prefix)).queue();
                return;
            }

            if (message.equals(prefix2) && !HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.getChannel().sendMessage(jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getAuthor().getId())).queue();
                return;
            }

            if (!HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                HangmanRegistry.getInstance().setHangman(userIdLong, new Hangman(event.getGuild(), event.getChannel(), event.getMember().getUser()));
                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).startGame(event.getChannel(), event.getMember().getUser());
            }
        }
    }
}