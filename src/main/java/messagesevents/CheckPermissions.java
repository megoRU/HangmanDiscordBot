package messagesevents;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import startbot.BotStart;

public class CheckPermissions {

    public static boolean isHasPermissionToWrite(final TextChannel textChannel) {
        try {
            if (BotStart.getShardManager()
                    .getGuildById(textChannel.getGuild().getId())
                    .getSelfMember()
                    .hasPermission(textChannel, Permission.MESSAGE_SEND)) {
                return false;
            }
        } catch (Exception e) {
            System.out.println("checkMessageWrite()");
            e.printStackTrace();
            return true;
        }
        return true;
    }

    public static boolean isHasPermissionsWriteAndEmbedLinks(final TextChannel textChannel) {
        try {
            if (BotStart.getShardManager()
                    .getGuildById(textChannel.getGuild().getId())
                    .getSelfMember()
                    .hasPermission(textChannel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                return false;
            }
        } catch (Exception e) {
            System.out.println("checkMessageWriteAndEmbedLinks()");
            e.printStackTrace();
            return true;
        }
        return true;
    }
}