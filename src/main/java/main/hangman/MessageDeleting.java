package main.hangman;

import main.config.BotStartConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageDeleting extends TimerTask {

    private static final Queue<Message> messageList = new ConcurrentLinkedQueue<>();

    static {
        Timer timer = new Timer();
        timer.schedule(new MessageDeleting(), 0, 5000);
    }

    @Override
    public void run() {
        while (!messageList.isEmpty()) {
            Message poll = messageList.poll();

            try {
                if (poll != null) {
                    if (poll.isFromGuild()) {
                        Guild guild = BotStartConfig.jda.getGuildById(poll.getGuild().getId());
                        if (guild != null) {
                            ChannelType channelType = poll.getChannelType();
                            GuildChannel guildChannel = null;
                            switch (channelType) {
                                case TEXT -> guildChannel = poll.getChannel().asTextChannel();
                                case GUILD_PUBLIC_THREAD, GUILD_NEWS_THREAD, GUILD_PRIVATE_THREAD ->
                                        guildChannel = poll.getChannel().asThreadChannel();
                                case NEWS -> guildChannel = poll.getChannel().asNewsChannel();
                            }

                            if (guildChannel != null) {
                                boolean hasPermission = guild.getSelfMember().hasPermission(guildChannel, Permission.MESSAGE_MANAGE);
                                if (hasPermission) {
                                    poll.delete().queue();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void addMessageToDelete(Message message) {
        messageList.add(message);
    }
}