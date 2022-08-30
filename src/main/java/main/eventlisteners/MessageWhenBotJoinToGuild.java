package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.enums.Buttons;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.List;

@AllArgsConstructor
@Service
public class MessageWhenBotJoinToGuild extends ListenerAdapter {

    private static final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);

    //bot join msg
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        try {
            if (event.getGuild().getDefaultChannel() == null) return;

            if (!event.getGuild().getSelfMember().hasPermission(event.getGuild().getDefaultChannel(), Permission.MESSAGE_EMBED_LINKS)) {
                return;
            }

            if (event.getGuild().getSelfMember().hasPermission(event.getGuild().getDefaultChannel(), Permission.MESSAGE_SEND)) {
                EmbedBuilder welcome = new EmbedBuilder();
                welcome.setColor(Color.GREEN);
                welcome.addField("Hangman", "Thanks for adding " +
                        "**"
                        + event.getGuild().getSelfMember().getUser().getName() +
                        "** " + "bot to " + event.getGuild().getName() +
                        "!\n", false);
                welcome.addField("List of commands", "Use **/help** for a list of commands.", false);
                welcome.addField("Support server", ":helmet_with_cross: [Discord server](https://discord.com/invite/UrWG3R683d)\n", false);
                welcome.addField("Information", "We are actively writing about new updates or problems in our discord. We recommend that you follow up.", false);
                welcome.addField("One more Thing", "If you are not satisfied with something in the bot, please let us know, we will fix it!", false);
                welcome.addField("Vote", ":boom: [Vote for this bot](https://boticord.top/bot/845974873682608129)", false);

                event.getGuild().getDefaultChannel().asTextChannel().sendMessageEmbeds(welcome.build())
                        .setActionRow(
                                List.of(Button.success(Buttons.BUTTON_HELP.name(), jsonParsers.getLocale("button_Help", event.getGuild().getIdLong())),
                                        Button.link("https://discord.gg/UrWG3R683d", "Support"))
                        ).queue();
                welcome.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        System.out.println("Удаляем данные после удаления бота из Guild");
    }
}