package main.eventlisteners;

import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public class ChecksClass {

    public static final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    public static boolean check(@NotNull SlashCommandInteractionEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE || event.getGuild() == null) return true;

        Member selfMember = event.getGuild().getSelfMember();
        StringBuilder stringBuilder = new StringBuilder();

        MessageChannelUnion channel = event.getChannel();
        boolean canWrite = true;
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
                permissions = hasPermission(channel.asThreadChannel(), selfMember, stringBuilder);
            }
        }

        String checkPermissions =
                String.format(jsonParsers.getLocale("check_permissions", event.getUser().getIdLong()),
                        event.getGuild().getId(),
                        stringBuilder);

        if (!permissions || !canWrite) {
            event.reply(checkPermissions).queue();
            return false;
        }

        return true;
    }

    public static boolean check(@NotNull MessageReceivedEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE || event.getGuild() == null) return true;

        Member selfMember = event.getGuild().getSelfMember();
        StringBuilder stringBuilder = new StringBuilder();

        MessageChannelUnion channel = event.getChannel();
        boolean canWrite = true;
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
                permissions = hasPermission(channel.asThreadChannel(), selfMember, stringBuilder);
            }
        }

        String checkPermissions =
                String.format(jsonParsers.getLocale("check_permissions", event.getAuthor().getIdLong()),
                        event.getGuild().getId(),
                        stringBuilder);

        if (!permissions || !canWrite) {
            event.getChannel().sendMessage(checkPermissions).queue();
            return false;
        }

        return true;
    }

    public static boolean check(ButtonInteractionEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE || event.getGuild() == null) return true;

        Member selfMember = event.getGuild().getSelfMember();
        StringBuilder stringBuilder = new StringBuilder();

        MessageChannelUnion channel = event.getChannel();
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

        String checkPermissions =
                String.format(jsonParsers.getLocale("check_permissions", event.getUser().getIdLong()),
                        event.getGuild().getId(),
                        stringBuilder);

        if (!permissions || !canWrite || !canWriteThreads) {
            event.reply(checkPermissions).queue();
            return false;
        }

        return true;
    }

    private static boolean canWriteThreads(GuildChannel channel, Member selfMember, StringBuilder stringBuilder) {
        boolean canWrite = true;

        if (!selfMember.hasPermission(channel, Permission.MESSAGE_SEND_IN_THREADS)) {
            stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.MESSAGE_SEND_IN_THREADS`" : ", `Permission.MESSAGE_SEND_IN_THREADS`");
            canWrite = false;
        }

        return canWrite;
    }

    private static boolean canWrite(GuildChannel channel, Member selfMember, StringBuilder stringBuilder) {
        boolean canWrite = true;

        if (!selfMember.hasPermission(channel, Permission.MESSAGE_SEND)) {
            stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.MESSAGE_SEND`" : ", `Permission.MESSAGE_SEND`");
            canWrite = false;
        }

        if (!selfMember.hasPermission(channel, Permission.VIEW_CHANNEL)) {
            stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.VIEW_CHANNEL`" : ", `Permission.VIEW_CHANNEL`");
            canWrite = false;
        }

        return canWrite;
    }

    private static boolean hasPermission(GuildChannel channel, Member selfMember, StringBuilder stringBuilder) {
        boolean bool = true;

        if (!selfMember.hasPermission(channel, Permission.MESSAGE_HISTORY)) {
            stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.MESSAGE_HISTORY`" : ", `Permission.MESSAGE_HISTORY`");
            bool = false;
        }

        if (!selfMember.hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
            stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.MESSAGE_EMBED_LINKS`" : ", `Permission.MESSAGE_EMBED_LINKS`");
            bool = false;
        }

        return bool;
    }
}