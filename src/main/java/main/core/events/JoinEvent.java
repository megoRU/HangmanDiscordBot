package main.core.events;

import main.hangman.impl.ButtonIMpl;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;

@Service
public class JoinEvent {

    public void join(@NotNull GuildJoinEvent event) {
        try {
            if (event.getGuild().getDefaultChannel() == null) return;
            if (!event.getGuild().getSelfMember().hasPermission(event.getGuild().getDefaultChannel(),
                    Permission.MESSAGE_SEND,
                    Permission.MESSAGE_EMBED_LINKS,
                    Permission.VIEW_CHANNEL)) {
                return;
            }

            if (event.getGuild().getSelfMember().hasPermission(event.getGuild().getDefaultChannel(), Permission.MESSAGE_SEND)) {
                EmbedBuilder welcome = new EmbedBuilder();
                welcome.setColor(Color.GREEN);
                welcome.addField("Hangman", "Thanks for adding **Hangman** bot to " + event.getGuild().getName() + "!\n", false);
                welcome.addField("List of commands", "Use </help:940560633504604163> for a list of commands.", false);
                welcome.addField("Support server", ":helmet_with_cross: [Discord server](https://discord.com/invite/UrWG3R683d)\n", false);
                welcome.addField("Information", "We are actively writing about new updates or problems in our discord. We recommend that you follow up.", false);
                welcome.addField("One more Thing", "If you are not satisfied with something in the bot, please let us know, we will fix it!", false);
                welcome.addField("Vote", ":boom: [Vote for this bot](https://boticord.top/bot/845974873682608129)", false);

                DefaultGuildChannelUnion defaultChannel = event.getGuild().getDefaultChannel();
                MessageChannel messageChannel = null;
                if (defaultChannel != null) {
                    ChannelType type = defaultChannel.getType();
                    if (type == ChannelType.NEWS) {
                        messageChannel = defaultChannel.asNewsChannel();
                    } else if (type == ChannelType.TEXT) {
                        messageChannel = defaultChannel.asTextChannel();
                    }
                    if (messageChannel != null) {
                        messageChannel
                                .sendMessageEmbeds(welcome.build())
                                .setActionRow(ButtonIMpl.BUTTON_HELP, ButtonIMpl.BUTTON_SUPPORT)
                                .queue();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}