package main.hangman;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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
                List<Message> list = localMessageList
                        .stream()
                        .filter(message -> message.getChannel() == messageChannelUnion)
                        .filter(message -> message.getGuild().getSelfMember().hasPermission(message.getGuildChannel(), Permission.MESSAGE_MANAGE))
                        .toList();

                try {
                    if (!list.isEmpty()) {
                        List<CompletableFuture<Void>> completableFutures = messageChannelUnion.purgeMessages(list);
                        for (CompletableFuture<Void> completableFuture : completableFutures) {
                            CompletableFuture.allOf(completableFuture);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void addMessageToDelete(Message message) {
        messageList.add(message);
    }
}