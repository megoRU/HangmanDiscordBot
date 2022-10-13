package main.eventlisteners;

import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ChecksClass {

    public static final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    public static boolean canSendHG(MessageChannel channelUnion, Event event) {
        if (channelUnion instanceof PrivateChannel) {
            return true;
        } else {
            GuildChannel guildChannel = (GuildChannel) channelUnion;

            Member selfMember = guildChannel.getGuild().getSelfMember();
            StringBuilder stringBuilder = new StringBuilder();

            boolean bool = true;
            boolean canWrite = true;

            if (!selfMember.hasPermission(guildChannel, Permission.MESSAGE_SEND)) {
                stringBuilder.append("`Permission.MESSAGE_SEND`");
                bool = false;
                canWrite = false;
            }

            if (!selfMember.hasPermission(guildChannel, Permission.VIEW_CHANNEL)) {
                stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.VIEW_CHANNEL`" : ", `Permission.VIEW_CHANNEL`");
                bool = false;
                canWrite = false;
            }

            if (!selfMember.hasPermission(guildChannel, Permission.MESSAGE_HISTORY)) {
                stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.MESSAGE_HISTORY`" : ", `Permission.MESSAGE_HISTORY`");
                bool = false;
            }

            if (!selfMember.hasPermission(guildChannel, Permission.MESSAGE_EMBED_LINKS)) {
                stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.MESSAGE_EMBED_LINKS`" : ", `Permission.MESSAGE_EMBED_LINKS`");
                bool = false;
            }

            String checkPermissions =
                    String.format(jsonParsers.getLocale("check_permissions", 0L),
                    guildChannel.getId(),
                    stringBuilder);

            if (event instanceof MessageReceivedEvent) {
                MessageReceivedEvent mre = (MessageReceivedEvent) event;
                if (!bool) {
                    if (canWrite) {
                        mre.getChannel().sendMessage(checkPermissions).queue();
                    }
                    return false;
                }
            } else if (event instanceof GenericCommandInteractionEvent) {
                GenericCommandInteractionEvent gcie = (GenericCommandInteractionEvent) event;
                if (!bool && gcie.getGuild() != null) {
                    gcie.reply(checkPermissions).queue();
                    return false;
                }
            }

            return bool;
        }
    }
}