package messagesevents;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public interface SenderMessage {

  default void sendMessage(EmbedBuilder embedBuilder, @NotNull MessageReceivedEvent event) {
    try {
      event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
