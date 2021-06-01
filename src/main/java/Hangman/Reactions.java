package Hangman;

import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Reactions extends ListenerAdapter {

    public static final String emojiNextTrack = "⏭️";

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {

        if (event.getUser().isBot()) {
            return;
        }

        if (HangmanRegistry.getInstance().hasHangman(event.getUserIdLong())) {
            return;
        }
        try {
            if (event.getReactionEmote().isEmoji()) {
                String emoji = event.getReactionEmote().getEmoji();

                long userIdLong = event.getUserIdLong();
                if (emoji.equals(emojiNextTrack)) {
                    HangmanRegistry.getInstance().setHangman(userIdLong, new Hangman(event.getGuild(), event.getChannel(), event.getMember().getUser()));
                    HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).startGame(event.getChannel(), event.getMember().getUser());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}