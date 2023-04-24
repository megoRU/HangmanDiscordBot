package main.hangman;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

import java.util.*;

public class MessageDeleting extends TimerTask {

    private static final Queue<Message> messageList = new ArrayDeque<>();

    static {
        Timer timer = new Timer();
        timer.schedule(new MessageDeleting(), 0, 5000);
    }

    @Override
    public void run() {
        List<Message> localMessageList = new ArrayList<>();
        Set<MessageChannelUnion> messageChannelUnions = new HashSet<>();

        synchronized (messageList) {
            for (Message message : messageList) {
                MessageChannelUnion channel = message.getChannel();

                localMessageList.add(message);
                messageChannelUnions.add(channel);
            }

            for (MessageChannelUnion messageChannelUnion : messageChannelUnions) {
                List<String> list = localMessageList
                        .stream()
                        .filter(message -> message.getChannel() == messageChannelUnion)
                        .filter(message -> message.getGuild().getSelfMember().hasPermission(message.getGuildChannel(), Permission.MESSAGE_MANAGE))
                        .map(ISnowflake::getId)
                        .toList();

                if (list.size() > 1) {
                    try {
                        switch (messageChannelUnion.getType()) {
                            case TEXT -> messageChannelUnion.asTextChannel().deleteMessagesByIds(list).queue();
                            case GUILD_PUBLIC_THREAD, GUILD_NEWS_THREAD, GUILD_PRIVATE_THREAD ->
                                    messageChannelUnion.asThreadChannel().deleteMessagesByIds(list).queue();
                            case NEWS -> messageChannelUnion.asNewsChannel().deleteMessagesByIds(list).queue();
                            default -> {
                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    switch (messageChannelUnion.getType()) {
                        case TEXT -> messageChannelUnion.asTextChannel().deleteMessageById(list.get(0)).queue();
                        case GUILD_PUBLIC_THREAD, GUILD_NEWS_THREAD, GUILD_PRIVATE_THREAD ->
                                messageChannelUnion.asThreadChannel().deleteMessageById(list.get(0)).queue();
                        case NEWS -> messageChannelUnion.asNewsChannel().deleteMessageById(list.get(0)).queue();
                        default -> {
                            return;
                        }
                    }
                }
            }
        }
    }

    public static void addMessageToDelete(Message message) {
        messageList.add(message);
    }
}