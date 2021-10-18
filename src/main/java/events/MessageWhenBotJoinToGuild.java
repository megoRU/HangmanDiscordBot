package events;

import hangman.ReactionsButton;
import db.DataBase;
import jsonparser.JSONParsers;
import messagesevents.CheckPermissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MessageWhenBotJoinToGuild extends ListenerAdapter {

    private static final JSONParsers jsonParsers = new JSONParsers();

    //bot join msg
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {

        try {
            if (event.getGuild().getDefaultChannel() == null) return;

            if (!new CheckPermissions(event.getGuild().getDefaultChannel()).checkMessageWriteAndEmbedLinks()) {
                return;
            }

            EmbedBuilder welcome = new EmbedBuilder();
            welcome.setColor(Color.GREEN);
            welcome.addField("hangman", "Thanks for adding " +
                    "**"
                    + event.getGuild().getSelfMember().getUser().getName() +
                    "** " + "bot to " + event.getGuild().getName() +
                    "!\n", false);
            welcome.addField("List of commands", "Use **!help** for a list of commands.", false);
            welcome.addField("Support server", ":helmet_with_cross: [Discord server](https://discord.com/invite/UrWG3R683d)\n", false);
            welcome.addField("Vote", ":boom: [Vote for this bot](https://top.gg/bot/808277484524011531/vote)", false);

            List<Button> buttons = new ArrayList<>();

            buttons.add(Button.success(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_HELP,
                    jsonParsers.getLocale("button_Help", event.getGuild().getId())));

            buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));


            event.getGuild().getDefaultChannel().sendMessageEmbeds(welcome.build()).setActionRow(buttons).queue();
            welcome.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        try {
            System.out.println("Удаляем данные после удаления бота из Guild");
            DataBase.getInstance().removePrefixFromDB(event.getGuild().getId());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}