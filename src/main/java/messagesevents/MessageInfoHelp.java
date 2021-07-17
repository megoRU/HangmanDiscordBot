package messagesevents;

import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;
import java.util.concurrent.TimeUnit;

public class MessageInfoHelp extends ListenerAdapter implements SenderMessage {

  private static final String HELP = "!help";
  private final JSONParsers jsonParsers = new JSONParsers();

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }

    if (event.isFromType(ChannelType.PRIVATE) && event.getMessage().getContentDisplay().trim().equals("!help")) {
      buildMessage(ChannelType.PRIVATE, event);
      return;
    }

    if (!event.isFromType(ChannelType.PRIVATE)) {
      buildMessage(ChannelType.TEXT, event);
    }
  }

  private void buildMessage(ChannelType channelType, @NotNull MessageReceivedEvent event) {
    String p = "!";
    String prefix = HELP;
    if (!channelType.equals(ChannelType.PRIVATE)) {

      if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
        p = BotStart.getMapPrefix().get(event.getGuild().getId());
        prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) + "help";
      }
    }

    if (event.getMessage().getContentDisplay().trim().equals(prefix)) {
      String avatarUrl = null;
      String avatarFromEvent = event.getMessage().getAuthor().getAvatarUrl();
      if (avatarFromEvent == null) {
        avatarUrl = "https://cdn.discordapp.com/avatars/754093698681274369/dc4b416065569253bc6323efb6296703.png";
      }
      if (avatarFromEvent != null) {
        avatarUrl = avatarFromEvent;
      }
      String userIdLong = !channelType.equals(ChannelType.PRIVATE) ? event.getAuthor().getId() : "0000000000000";

      EmbedBuilder info = new EmbedBuilder();
      info.setColor(0xa224db);
      info.setAuthor(event.getAuthor().getName(), null, avatarUrl);
      info.addField(jsonParsers.getLocale("messages_events_Prefix", userIdLong),
          jsonParsers.getLocale("messages_events_Changes_Prefix", userIdLong) +
              jsonParsers.getLocale("messages_events_Reset_Prefix", userIdLong), false);

      info.addField(jsonParsers.getLocale("messages_events_Language_Title", userIdLong), "`"
              + p + jsonParsers.getLocale("messages_events_Language", userIdLong) + "`"
              + p + jsonParsers.getLocale("messages_events_Game_Language", userIdLong), false);

      info.addField(jsonParsers.getLocale("messages_events_Title", userIdLong), "`"
              + p + jsonParsers.getLocale("messages_events_Start_Hangman", userIdLong) + "`"
              + p + jsonParsers.getLocale("messages_events_Stop_Hangman", userIdLong) + "`"
              + p + jsonParsers.getLocale("messages_events_Stats_Hangman", userIdLong), false);

      info.addField(jsonParsers.getLocale("messages_events_Links", userIdLong),
          jsonParsers.getLocale("messages_events_Site", userIdLong) +
              jsonParsers.getLocale("messages_events_Add_Me_To_Other_Guilds", userIdLong) +
              jsonParsers.getLocale("messages_events_Vote_For_This_Bot", userIdLong), false);

      info.addField(
          jsonParsers.getLocale("messages_events_Bot_Creator", userIdLong),
          jsonParsers.getLocale("messages_events_Bot_Creator_Url_Steam", userIdLong), false);

      info.addField(
          jsonParsers.getLocale("messages_events_Support", userIdLong),
          jsonParsers.getLocale("messages_events_Support_Url_Discord", userIdLong), false);
      try {
        switch (channelType) {
          case TEXT -> {
            if (!event.getGuild().getSelfMember()
                .hasPermission(event.getGuild().getTextChannelById(event.getChannel().getId()), Permission.MESSAGE_WRITE)) {
              return;
            }
            event.getChannel().sendMessage(jsonParsers.getLocale("messages_events_Send_Private_Message",
                    userIdLong)).delay(5, TimeUnit.SECONDS)
                .flatMap(Message::delete).queue();
            event.getMember().getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(info.build()))
                .queue(null, error -> event.getChannel()
                    .sendMessage(jsonParsers.getLocale("messages_events_Failed_To_Send_Message", event.getGuild().getId())).queue());
          }
          case PRIVATE -> {
            sendMessage(info, event);
            info.clear();
          }

        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}