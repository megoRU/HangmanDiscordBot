package main.hangman;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.enums.Buttons;
import main.eventlisteners.CheckPermissions;
import main.jsonparser.JSONParsers;
import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.PlayerRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class GameHangmanListener extends ListenerAdapter {

    private static final String HG = "!hg";
    private static final String HG_STOP = "!hg stop";
    private static final String HG_ONE_LETTER = "[А-ЯЁа-яё]";
    private static final String HG_ONE_LETTER_ENG = "[A-Za-z]";
    private static final JSONParsers jsonParsers = new JSONParsers();
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (event.isFromGuild() && CheckPermissions.isHasPermissionsWriteAndEmbedLinks(event.getTextChannel())) return;

        String message = event.getMessage().getContentRaw().trim().toLowerCase();

        String prefix = HG;
        String prefix2 = HG_STOP;
        long userIdLong = event.getAuthor().getIdLong();

        if ((message.matches(HG_ONE_LETTER) || message.matches(HG_ONE_LETTER_ENG)) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
            HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).logic(message, event.getMessage());
            return;
        }

        if (event.isFromGuild() && BotStartConfig.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "hg";
            prefix2 = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "hg stop";
        }

        if (message.equals(prefix) || message.equals(prefix2)) {
            if (BotStartConfig.getMapGameLanguages().get(event.getAuthor().getId()) == null) {
                EmbedBuilder needSetLanguage = new EmbedBuilder();

                needSetLanguage.setAuthor(event.getAuthor().getName(), null, event.getAuthor().getAvatarUrl());
                needSetLanguage.setColor(0x00FF00);
                needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getAuthor().getId()));

                event.getChannel().sendMessageEmbeds(needSetLanguage.build())
                        .setActionRow(
                                Button.secondary(Buttons.BUTTON_RUS.name(), "Кириллица")
                                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")),

                                Button.secondary(Buttons.BUTTON_ENG.name(), "Latin")
                                        .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")),
                                Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play"))
                        .queue();

                return;
            }

            if (message.equals(prefix) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.getChannel().sendTyping().queue();
                EmbedBuilder youPlay = new EmbedBuilder();

                youPlay.setAuthor(event.getAuthor().getName(), null, event.getAuthor().getAvatarUrl());
                youPlay.setColor(0x00FF00);
                youPlay.setDescription(jsonParsers.getLocale("Hangman_Listener_You_Play",
                        event.getAuthor().getId()).replaceAll("\\{0}", prefix));

                event.getChannel().sendMessageEmbeds(youPlay.build())
                        .setActionRow(Button.danger(Buttons.BUTTON_STOP.name(), "Stop game"))
                        .queue();
                return;
            }

            if (message.equals(prefix2) && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                HangmanRegistry.getInstance().removeHangman(userIdLong);

                event.getChannel().sendMessage(jsonParsers.getLocale("Hangman_Eng_game",
                                event.getAuthor().getId()).replaceAll("\\{0}", prefix))
                        .setActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                        .queue();
                hangmanGameRepository.deleteActiveGame(userIdLong);
                return;
            }

            if (message.equals(prefix2) && !HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.getChannel().sendMessage(jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getAuthor().getId()))
                        .setActionRow(Button.success(Buttons.BUTTON_START_NEW_GAME.name(), "Play again"))
                        .queue();
                return;
            }

            if (!HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                event.getChannel().sendTyping().queue();
                HangmanRegistry.getInstance().setHangman(userIdLong, new Hangman(event.getAuthor().getId(), event.getGuild().getId(), event.getChannel().getIdLong(), hangmanGameRepository, gamesRepository, playerRepository));
                HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).startGame(event.getTextChannel(), event.getAuthor().getAvatarUrl(), event.getAuthor().getName());
            }
        }
    }
}