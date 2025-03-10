package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.enums.Buttons;
import main.game.Hangman;
import main.game.HangmanBuilder;
import main.game.HangmanDataSaving;
import main.game.HangmanPlayer;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class HangmanButton {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final HangmanDataSaving hangmanDataSaving;

    public void hangman(@NotNull ButtonInteractionEvent event) {
        if (event.getButton().getId() == null) return;

        Guild guild = event.getGuild();
        var userIdLong = event.getUser().getIdLong();
        var channelIdLong = event.getChannel().getIdLong();

        String gameLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getIdLong());
        Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
        UserSettings userSettings = userSettingsMap.get(userIdLong);

        HangmanRegistry instance = HangmanRegistry.getInstance();

        if (userSettings == null) {
            event.reply(gameLanguage)
                    .addActionRow(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH)
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!instance.hasHangman(userIdLong)) {
            event.getChannel().sendTyping().queue();

            HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, guild != null ? guild.getIdLong() : null, channelIdLong);
            HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder();
            hangmanBuilder.addHangmanPlayer(hangmanPlayer);

            Message message = event.getMessage();
            String guildId = event.getGuild() != null ? event.getGuild().getId() : "null";

            List<Long> usersList = new ArrayList<>();
            List<MessageEmbed> embeds = message.getEmbeds();

            for (MessageEmbed embed : embeds) {
                List<MessageEmbed.Field> fields = embed.getFields();
                for (MessageEmbed.Field field : fields) {
                    if (field.getValue() != null) {
                        String input = field.getValue();
                        String regex = "<@\\d+>";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(input);
                        while (matcher.find()) {
                            String userIdString = matcher.group()
                                    .replaceAll("<@", "")
                                    .replaceAll(">", "");
                            usersList.add(Long.parseLong(userIdString));
                        }
                    }
                }
            }

            if (!usersList.contains(userIdLong)) {
                String youCannotPressPlayAgain = jsonParsers.getLocale("you_cannot_press_play_again", userIdLong);
                event.reply(youCannotPressPlayAgain).setEphemeral(true).queue();
                return;
            } else {
                event.editButton(event.getButton().asDisabled()).queue();
            }

            usersList.stream()
                    .filter(user -> !instance.hasHangman(user))
                    .filter(user -> !user.equals(userIdLong))
                    .forEach(user -> {
                        Long guildIdLong = !guildId.equals("null") ? Long.parseLong(guildId) : null;
                        HangmanPlayer hangmanPlayerSecond = new HangmanPlayer(user, guildIdLong, channelIdLong);
                        hangmanBuilder.addHangmanPlayer(hangmanPlayerSecond);
                    });

            Hangman hangman = hangmanBuilder.build();

            HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
            for (HangmanPlayer player : hangmanPlayers) {
                instance.setHangman(player.getUserId(), hangman);
            }

            hangman.startGame(event.getChannel(), hangmanDataSaving);
        } else {
            String hangmanListenerYouPlay = jsonParsers.getLocale("Hangman_Listener_You_Play", event.getUser().getIdLong());

            event.reply(hangmanListenerYouPlay)
                    .setActionRow(Button.danger(Buttons.BUTTON_STOP.name(), "Stop game"))
                    .setEphemeral(true)
                    .queue();
        }
    }
}