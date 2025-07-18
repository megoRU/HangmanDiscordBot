package main.core;

import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
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

    public static boolean check(@NotNull GuildMessageChannel channel) {
        return hasPermission(channel);
    }

    public static boolean check(@NotNull Event event) {
        Guild guild = getGuild(event);
        if (guild == null || getType(event) == ChannelType.PRIVATE) return true;

        StringBuilder stringBuilder = new StringBuilder();
        MessageChannelUnion channel = getChannel(event);

        boolean canWrite = true;
        boolean canWriteThreads = true;
        boolean permissions = true;

        switch (channel.getType()) {
            case NEWS -> {
                canWrite = canWrite(channel.asNewsChannel(), stringBuilder);
                permissions = hasPermission(channel.asNewsChannel(), stringBuilder);
            }
            case TEXT -> {
                canWrite = canWrite(channel.asTextChannel(), stringBuilder);
                permissions = hasPermission(channel.asTextChannel(), stringBuilder);
            }
            case GUILD_PUBLIC_THREAD, GUILD_NEWS_THREAD, GUILD_PRIVATE_THREAD -> {
                canWrite = canWrite(channel.asThreadChannel(), stringBuilder);
                canWriteThreads = canWriteThreads(channel.asThreadChannel(), stringBuilder);
                permissions = hasPermission(channel.asThreadChannel(), stringBuilder);
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
        switch (event) {
            case SlashCommandInteractionEvent slashEvent -> slashEvent.reply(checkPermissions).queue();
            case UserContextInteractionEvent contextEvent -> contextEvent.reply(checkPermissions).queue();
            case ButtonInteractionEvent buttonInteractionEvent ->
                    buttonInteractionEvent.reply(checkPermissions).queue();
            default -> {
                if (canWrite) {
                    MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;
                    messageReceivedEvent.getChannel().sendMessage(checkPermissions).queue();
                }
            }
        }
    }

    private static MessageChannelUnion getChannel(@NotNull Event event) {
        switch (event) {
            case SlashCommandInteractionEvent slashEvent -> {
                return slashEvent.getChannel();
            }
            case UserContextInteractionEvent contextEvent -> {
                return (MessageChannelUnion) contextEvent.getMessageChannel();
            }
            case ButtonInteractionEvent buttonInteractionEvent -> {
                return buttonInteractionEvent.getChannel();
            }
            default -> {
                MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;
                return messageReceivedEvent.getChannel();
            }
        }
    }

    private static User getUser(@NotNull Event event) {
        switch (event) {
            case SlashCommandInteractionEvent slashEvent -> {
                return slashEvent.getUser();
            }
            case UserContextInteractionEvent contextEvent -> {
                return contextEvent.getUser();
            }
            case ButtonInteractionEvent buttonInteractionEvent -> {
                return buttonInteractionEvent.getUser();
            }
            default -> {
                MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;
                return messageReceivedEvent.getAuthor();
            }
        }
    }

    @Nullable
    private static Guild getGuild(@NotNull Event event) {
        switch (event) {
            case SlashCommandInteractionEvent slashEvent -> {
                return slashEvent.getGuild();
            }
            case UserContextInteractionEvent contextEvent -> {
                return contextEvent.getGuild();
            }
            case ButtonInteractionEvent buttonInteractionEvent -> {
                return buttonInteractionEvent.getGuild();
            }
            default -> {
                MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;
                boolean isFromGuild = messageReceivedEvent.isFromGuild();
                if (isFromGuild) return messageReceivedEvent.getGuild();
                else return null;
            }
        }
    }

    private static ChannelType getType(@NotNull Event event) {
        switch (event) {
            case SlashCommandInteractionEvent slashEvent -> {
                return slashEvent.getChannelType();
            }
            case UserContextInteractionEvent contextEvent -> {
                return contextEvent.getChannelType();
            }
            case ButtonInteractionEvent buttonInteractionEvent -> {
                return buttonInteractionEvent.getChannelType();
            }
            default -> {
                MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;
                return messageReceivedEvent.getChannelType();
            }
        }
    }

    private static boolean canWriteThreads(GuildChannel channel, StringBuilder stringBuilder) {
        boolean canWrite = true;
        Member selfMember = channel.getGuild().getSelfMember();

        if (!selfMember.hasPermission(channel, Permission.MESSAGE_SEND_IN_THREADS)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.MESSAGE_SEND_IN_THREADS`" : ",\n`Permission.MESSAGE_SEND_IN_THREADS`");
            canWrite = false;
        }

        return canWrite;
    }

    private static boolean canWrite(GuildChannel channel, StringBuilder stringBuilder) {
        boolean canWrite = true;
        Member selfMember = channel.getGuild().getSelfMember();

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

    private static boolean hasPermission(GuildChannel channel) {
        Member selfMember = channel.getGuild().getSelfMember();
        return selfMember.hasPermission(channel, Permission.VIEW_CHANNEL);
    }

    private static boolean hasPermission(GuildChannel channel, StringBuilder stringBuilder) {
        boolean bool = true;
        Member selfMember = channel.getGuild().getSelfMember();

        if (!selfMember.hasPermission(channel, Permission.MESSAGE_HISTORY)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.MESSAGE_HISTORY`" : ",\n`Permission.MESSAGE_HISTORY`");
            bool = false;
        }

        if (!selfMember.hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.MESSAGE_EMBED_LINKS`" : ",\n`Permission.MESSAGE_EMBED_LINKS`");
            bool = false;
        }

        if (!selfMember.hasPermission(channel, Permission.VIEW_CHANNEL)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.VIEW_CHANNEL`" : ",\n`Permission.VIEW_CHANNEL`");
            bool = false;
        }

        return bool;
    }
}