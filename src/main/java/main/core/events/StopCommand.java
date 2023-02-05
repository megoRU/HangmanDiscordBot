package main.core.events;

import main.hangman.Hangman;
import main.hangman.HangmanEmbedUtils;
import main.hangman.HangmanRegistry;
import main.hangman.impl.ButtonIMpl;
import main.hangman.impl.HangmanHelper;
import main.jsonparser.JSONParsers;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StopCommand {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);

    private final HangmanGameRepository hangmanGameRepository;

    @Autowired
    public StopCommand(HangmanGameRepository hangmanGameRepository) {
        this.hangmanGameRepository = hangmanGameRepository;
    }

    public void stop(@NotNull Event event) {
        var userIdLong = 0L;
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            userIdLong = slashEvent.getUser().getIdLong();
        } else if (event instanceof ButtonInteractionEvent buttonEvent) {
            userIdLong = buttonEvent.getUser().getIdLong();
        }

        //Проверяем играет ли сейчас игрок. Если да удаляем игру.
        Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
        if (hangman != null) {
            long userId = hangman.getUserId(); //NPE? по идеи не должно
            long secondPlayer = hangman.getSecondPlayer(); //NPE? по идеи не должно

            String hangmanEngGame = jsonParsers.getLocale("Hangman_Eng_game", userId);
            String hangmanEngGame1 = jsonGameParsers.getLocale("Hangman_Eng_game", userId);

            if (secondPlayer != 0L) {
                Button buttonPlayAgainWithUsers = ButtonIMpl.getButtonPlayAgainWithUsers(userId, secondPlayer);
                sendMessage(event, hangmanEngGame, buttonPlayAgainWithUsers);
            } else {
                sendMessage(event, hangmanEngGame, ButtonIMpl.BUTTON_PLAY_AGAIN);
            }
            EmbedBuilder embedBuilder = HangmanEmbedUtils.hangmanPattern(userId, hangmanEngGame1);

            HangmanHelper.editMessage(embedBuilder, userId, hangmanGameRepository);
            HangmanRegistry.getInstance().removeHangman(userId);
            hangmanGameRepository.deleteActiveGame(userId);
            //Если игрок не играет, а хочет завершить игру, то нужно ему это прислать уведомление, что он сейчас не играет
        } else {
            String hangmanYouAreNotPlay = jsonParsers.getLocale("Hangman_You_Are_Not_Play", userIdLong);

            sendMessage(event, hangmanYouAreNotPlay, ButtonIMpl.BUTTON_PLAY_AGAIN);
        }
    }

    //TODO: Вынести в UpdateController -> CoreBot
    private void sendMessage(Event event, String text, Button button) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            if (slashEvent.isAcknowledged()) slashEvent.getHook().sendMessage(text).addActionRow(button).queue();
            else slashEvent.reply(text).addActionRow(button).queue();
        } else if (event instanceof ButtonInteractionEvent buttonEvent) {
            if (buttonEvent.isAcknowledged()) buttonEvent.getHook().sendMessage(text).addActionRow(button).queue();
            else buttonEvent.reply(text).addActionRow(button).queue();
        }
    }
}