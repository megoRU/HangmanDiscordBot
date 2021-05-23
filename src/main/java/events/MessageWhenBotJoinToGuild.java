package events;

import db.DataBase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import java.awt.*;

public class MessageWhenBotJoinToGuild extends ListenerAdapter {

  //bot join msg
  @Override
  public void onGuildJoin(@NotNull GuildJoinEvent event) {

    try {
      if (!event.getGuild().getSelfMember().hasPermission(event.getGuild().getDefaultChannel(),
          Permission.MESSAGE_WRITE)) {
        return;
      }
      EmbedBuilder welcome = new EmbedBuilder();
      welcome.setColor(Color.GREEN);
      welcome.addField("Hangman", "Thanks for adding " +
          "**"
          + event.getGuild().getSelfMember().getUser().getName() +
          "** " + "bot to " + event.getGuild().getName() +
          "!\n", false);
      welcome.addField("List of commands", "Use **!help** for a list of commands.", false);
      welcome.addField("Support server", ":helmet_with_cross: [Discord server](https://discord.com/invite/UrWG3R683d)\n", false);
      welcome.addField("Vote", ":boom: [Vote for this bot](https://top.gg/bot/808277484524011531/vote)"
          , false);

      event.getGuild().getDefaultChannel().sendMessage(welcome.build()).queue();
      welcome.clear();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onGuildLeave(@NotNull GuildLeaveEvent event) {
    try {
      System.out.println("Удаляем данные после удаления бота из Guild");
      DataBase.getInstance().removePrefixFromDB(event.getGuild().getId());
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}