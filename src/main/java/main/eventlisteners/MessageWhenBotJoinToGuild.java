package main.eventlisteners;

import lombok.AllArgsConstructor;
import main.jsonparser.JSONParsers;
import main.model.repository.PrefixRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class MessageWhenBotJoinToGuild extends ListenerAdapter {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final PrefixRepository prefixRepository;

    //bot join msg
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {

        try {
            if (event.getGuild().getDefaultChannel() == null) return;

            if (CheckPermissions.isHasPermissionsWriteAndEmbedLinks(event.getGuild().getDefaultChannel())) {
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
                welcome.addField("List of commands", "Use **!help** for a list of commands.", false);
                welcome.addField("Support server", ":helmet_with_cross: [Discord server](https://discord.com/invite/UrWG3R683d)\n", false);
                welcome.addField("Vote", ":boom: [Vote for this bot](https://top.gg/bot/808277484524011531/vote)", false);

                List<Button> buttons = new ArrayList<>();

                buttons.add(Button.success(ReactionsButton.BUTTON_HELP, jsonParsers.getLocale("button_Help", event.getGuild().getId())));
                buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

                event.getGuild().getDefaultChannel().sendMessageEmbeds(welcome.build()).setActionRow(buttons).queue();
                welcome.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if ((event.getGuild().getSelfMember().hasPermission(Permission.USE_APPLICATION_COMMANDS))) {

                List<OptionData> options = new ArrayList<>();
                options.add(new OptionData(OptionType.STRING, "game", "Setting the Game language")
                        .addChoice("eng", "eng")
                        .addChoice("rus", "rus")
                        .setRequired(true));

                options.add(new OptionData(OptionType.STRING, "bot", "Setting the bot language")
                        .addChoice("eng", "eng")
                        .addChoice("rus", "rus")
                        .setRequired(true));

                event.getGuild().upsertCommand("language", "Setting language").addOptions(options).queue();
                event.getGuild().upsertCommand("hg", "Start the game").queue();
                event.getGuild().upsertCommand("stop", "Stop the game").queue();
                event.getGuild().upsertCommand("help", "Bot commands").queue();
                event.getGuild().upsertCommand("stats", "Get your statistics").queue();
                event.getGuild().upsertCommand("mystats", "Find out the number of your wins and losses").queue();
                event.getGuild().upsertCommand("allstats", "Find out the statistics of all the bot's games").queue();
                event.getGuild().upsertCommand("delete", "Deleting your data").queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        try {
            System.out.println("Удаляем данные после удаления бота из Guild");
            prefixRepository.deletePrefix(event.getGuild().getId());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}