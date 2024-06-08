package main.core.events;

import main.config.BotStartConfig;
import main.enums.Buttons;
import main.game.*;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HangmanButton {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final HangmanGameRepository hangmanGameRepository;
    private final HangmanDataSaving hangmanDataSaving;
    private final HangmanResult hangmanResult;

    @Autowired
    public HangmanButton(HangmanGameRepository hangmanGameRepository,
                         HangmanDataSaving hangmanDataSaving,
                         HangmanResult hangmanResult) {
        this.hangmanGameRepository = hangmanGameRepository;
        this.hangmanDataSaving = hangmanDataSaving;
        this.hangmanResult = hangmanResult;
    }

    public void hangman(@NotNull ButtonInteractionEvent event) {
//        event.editButton(event.getButton().asDisabled()).queue();
        if (event.getButton().getId() == null) return;

        var userIdLong = event.getUser().getIdLong();
        var channelIdLong = event.getChannel().getIdLong();

        String gameLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getIdLong());
        UserSettings.GameLanguage userGameLanguage = BotStartConfig.getMapGameLanguages().get(userIdLong);
        HangmanRegistry instance = HangmanRegistry.getInstance();

        if (userGameLanguage == null) {
            event.getHook().sendMessage(gameLanguage)
                    .addActionRow(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH)
                    .addActionRow(HangmanUtils.getButtonPlayAgain(userIdLong))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!instance.hasHangman(userIdLong)) {
            event.getChannel().sendTyping().queue();

            HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder();
            hangmanBuilder.setHangmanDataSaving(hangmanDataSaving);
            hangmanBuilder.setHangmanGameRepository(hangmanGameRepository);
            hangmanBuilder.setHangmanResult(hangmanResult);

            Hangman hangman;
            Message message = event.getMessage();
            Mentions mentions = message.getMentions();
            String guildId = event.getGuild() != null ? event.getGuild().getId() : "null";

            List<User> users = mentions.getUsers();
            users.stream()
                    .filter(user -> !instance.hasHangman(Long.parseLong(user.getId())))
                    .forEach(user -> {
                        Long guildIdLong = !guildId.equals("null") ? Long.parseLong(guildId) : null;
                        HangmanPlayer hangmanPlayerSecond = new HangmanPlayer(Long.parseLong(user.getId()), guildIdLong, channelIdLong);
                        hangmanBuilder.addHangmanPlayer(hangmanPlayerSecond);
                    });

            hangman = hangmanBuilder.build();

            HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
            for (HangmanPlayer player : hangmanPlayers) {
                instance.setHangman(player.getUserId(), hangman);
            }

            hangman.startGame(event.getChannel());
        } else {
            String hangmanListenerYouPlay = jsonParsers.getLocale("Hangman_Listener_You_Play", event.getUser().getIdLong());

            EmbedBuilder youPlay = new EmbedBuilder();
            youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
            youPlay.setColor(0x00FF00);
            youPlay.setDescription(hangmanListenerYouPlay);

            event.getHook()
                    .sendMessageEmbeds(youPlay.build())
                    .setActionRow(Button.danger(Buttons.BUTTON_STOP.name(), "Stop game"))
                    .queue();
        }
    }
}