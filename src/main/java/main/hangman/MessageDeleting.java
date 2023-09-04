package main.hangman;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageDeleting extends TimerTask {

    private static final Map<String, List<Message>> messageList = new ConcurrentHashMap<>();

    static {
        Timer timer = new Timer();
        timer.schedule(new MessageDeleting(), 0, 5000);
    }

    @Override
    public void run() {
        messageList.forEach((channelId, messages) -> {
            int messageSize = messages.size();
            if (messageSize >= 2) {
                Message message = messages.get(0);
                GuildMessageChannelUnion guildChannel = message.getGuildChannel();
                boolean hasPermission = message.getGuild().getSelfMember().hasPermission(guildChannel, Permission.MESSAGE_MANAGE);
                if (hasPermission) {
                    List<String> listMessages = messages.stream().map(ISnowflake::getId).toList();
                    guildChannel
                            .deleteMessagesByIds(listMessages)
                            .queue();
                    messageList.remove(channelId);
                }
            }
        });
    }

    public static void addMessageToDelete(Message message) {
        if (message.isFromGuild()) {
            String channelID = message.getChannel().getId();
            List<Message> messages = messageList.get(channelID);
            if (messages == null) {
                messages = new ArrayList<>(15);
            }
            messages.add(message);
            messageList.put(channelID, messages);
        }
    }
}