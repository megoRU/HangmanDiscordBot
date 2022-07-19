package main.eventlisteners;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DeprecatedCommands extends ListenerAdapter {

    private static final String LANG_RUS = "game rus";
    private static final String LANG_ENG = "game eng";
    private static final String STATS = "hg stats";
    private static final String ALL_STATS = "hg allstats";
    private static final String MY_STATS = "hg mystats";
    private static final String HELP = "help";
    private static final String HG = "hg";
    private static final String HG_STOP = "hg stop";

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (event.isFromType(ChannelType.TEXT) && CheckPermissions.isHasPermissionToWrite(event.getTextChannel()))
            return;

        String message = event.getMessage().getContentRaw().toLowerCase().trim();

        if (message.length() < 2) return;

        int length = message.length();
        String messageWithOutPrefix = message.substring(1, length);

        if (messageWithOutPrefix.matches(LANG_RUS)
                || messageWithOutPrefix.matches(LANG_ENG)
                || messageWithOutPrefix.matches(STATS)
                || messageWithOutPrefix.matches(ALL_STATS)
                || messageWithOutPrefix.matches(MY_STATS)
                || messageWithOutPrefix.matches(HELP)
                || messageWithOutPrefix.matches(HG)
                || messageWithOutPrefix.matches(HG_STOP)) {
            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
            buttons.add(Button.link("https://discord.com/oauth2/authorize?client_id=845974873682608129&permissions=2147544128&scope=applications.commands%20bot", "Add Slash Commands"));

            event.getChannel()
                    .sendMessage("Deprecated. Use `/Slash Commands`")
                    .setActionRow(buttons)
                    .queue();
        }

    }
}
