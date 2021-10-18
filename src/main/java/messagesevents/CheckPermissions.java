package messagesevents;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import startbot.BotStart;

@AllArgsConstructor
public class CheckPermissions {

    private final TextChannel textChannel;

    public boolean checkMessageWrite() {
        try {
            if (BotStart.getShardManager()
                    .getGuildById(textChannel.getGuild().getId())
                    .getSelfMember()
                    .hasPermission(textChannel, Permission.MESSAGE_WRITE)) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("checkMessageWrite()");
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public boolean checkMessageWriteAndEmbedLinks() {
        try {
            if (BotStart.getShardManager()
                    .getGuildById(textChannel.getGuild().getId())
                    .getSelfMember()
                    .hasPermission(textChannel, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS)) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("checkMessageWriteAndEmbedLinks()");
            e.printStackTrace();
            return false;
        }
        return false;
    }
}