package main.core;

import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChecksClass {

    public static final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    public static boolean check(@NotNull Event event) {
        Guild guild = getGuild(event);
        if (guild == null || getType(event) == ChannelType.PRIVATE) return true;

        Member selfMember = guild.getSelfMember();
        StringBuilder stringBuilder = new StringBuilder();

        MessageChannelUnion channel = getChannel(event);

        boolean canWrite = true;
        boolean canWriteThreads = true;
        boolean permissions = true;

        switch (channel.getType()) {
            case NEWS -> {
                canWrite = canWrite(channel.asNewsChannel(), selfMember, stringBuilder);
                permissions = hasPermission(channel.asNewsChannel(), selfMember, stringBuilder);
            }
            case TEXT -> {
                canWrite = canWrite(channel.asTextChannel(), selfMember, stringBuilder);
                permissions = hasPermission(channel.asTextChannel(), selfMember, stringBuilder);
            }
            case GUILD_PUBLIC_THREAD, GUILD_NEWS_THREAD, GUILD_PRIVATE_THREAD -> {
                canWrite = canWrite(channel.asThreadChannel(), selfMember, stringBuilder);
                canWriteThreads = canWriteThreads(channel.asThreadChannel(), selfMember, stringBuilder);
                permissions = hasPermission(channel.asThreadChannel(), selfMember, stringBuilder);
            }
        }

        String checkPermissions = String.format(jsonParsers.getLocale("check_permissions", getUser(event).getIdLong()), stringBuilder);
        boolean allTrue = canWrite && canWriteThreads && permissions;

        if (!allTrue) {
            sendMessage(event, checkPermissions, canWrite);
            return false;
        } else {
            return true;
        }
    }

    private static void sendMessage(@NotNull Event event, String checkPermissions, boolean canWrite) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            slashEvent.reply(checkPermissions).queue();
        } else if (event instanceof UserContextInteractionEvent contextEvent) {
            contextEvent.reply(checkPermissions).queue();
        } else if (event instanceof ButtonInteractionEvent buttonInteractionEvent) {
            buttonInteractionEvent.reply(checkPermissions).queue();
        } else {
            if (canWrite) {
                MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;
                messageReceivedEvent.getChannel().sendMessage(checkPermissions).queue();
            }
        }
    }

    private static MessageChannelUnion getChannel(@NotNull Event event) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            return slashEvent.getChannel();
        } else if (event instanceof UserContextInteractionEvent contextEvent) {
            return (MessageChannelUnion) contextEvent.getMessageChannel();
        } else if (event instanceof ButtonInteractionEvent buttonInteractionEvent) {
            return buttonInteractionEvent.getChannel();
        } else {
            MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;
            return messageReceivedEvent.getChannel();
        }
    }

    private static User getUser(@NotNull Event event) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            return slashEvent.getUser();
        } else if (event instanceof UserContextInteractionEvent contextEvent) {
            return contextEvent.getUser();
        } else if (event instanceof ButtonInteractionEvent buttonInteractionEvent) {
            return buttonInteractionEvent.getUser();
        } else {
            MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;
            return messageReceivedEvent.getAuthor();
        }
    }

    @Nullable
    private static Guild getGuild(@NotNull Event event) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            return slashEvent.getGuild();
        } else if (event instanceof UserContextInteractionEvent contextEvent) {
            return contextEvent.getGuild();
        } else if (event instanceof ButtonInteractionEvent buttonInteractionEvent) {
            return buttonInteractionEvent.getGuild();
        } else {
            MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;
            boolean isFromGuild = messageReceivedEvent.isFromGuild();
            if (isFromGuild) return messageReceivedEvent.getGuild();
            else return null;
        }
    }

    private static ChannelType getType(@NotNull Event event) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            return slashEvent.getChannelType();
        } else if (event instanceof UserContextInteractionEvent contextEvent) {
            return contextEvent.getChannelType();
        } else if (event instanceof ButtonInteractionEvent buttonInteractionEvent) {
            return buttonInteractionEvent.getChannelType();
        } else {
            MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;
            return messageReceivedEvent.getChannelType();
        }
    }

    private static boolean canWriteThreads(GuildChannel channel, Member selfMember, StringBuilder stringBuilder) {
        boolean canWrite = true;

        if (!selfMember.hasPermission(channel, Permission.MESSAGE_SEND_IN_THREADS)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.MESSAGE_SEND_IN_THREADS`" : ",\n`Permission.MESSAGE_SEND_IN_THREADS`");
            canWrite = false;
        }

        return canWrite;
    }

    private static boolean canWrite(GuildChannel channel, Member selfMember, StringBuilder stringBuilder) {
        boolean canWrite = true;

        if (!selfMember.hasPermission(channel, Permission.MESSAGE_SEND)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.MESSAGE_SEND`" : ",\n`Permission.MESSAGE_SEND`");
            canWrite = false;
        }

        if (!selfMember.hasPermission(channel, Permission.VIEW_CHANNEL)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.VIEW_CHANNEL`" : ",\n`Permission.VIEW_CHANNEL`");
            canWrite = false;
        }

        return canWrite;
    }

    private static boolean hasPermission(GuildChannel channel, Member selfMember, StringBuilder stringBuilder) {
        boolean bool = true;

        if (!selfMember.hasPermission(channel, Permission.MESSAGE_HISTORY)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.MESSAGE_HISTORY`" : ",\n`Permission.MESSAGE_HISTORY`");
            bool = false;
        }

        if (!selfMember.hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.MESSAGE_EMBED_LINKS`" : ",\n`Permission.MESSAGE_EMBED_LINKS`");
            bool = false;
        }

        return bool;
    }
}