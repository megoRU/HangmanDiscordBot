package messagesevents;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;

public interface SenderMessage {

    default void sendMessage(EmbedBuilder embedBuilder, TextChannel textChannel) {
        try {
            textChannel.sendMessageEmbeds(embedBuilder.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
