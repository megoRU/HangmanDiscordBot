package main.controller;

import lombok.Getter;
import main.core.ChecksClass;
import main.core.CoreBot;
import main.core.events.*;
import main.enums.Buttons;
import main.hangman.Hangman;
import main.hangman.HangmanRegistry;
import main.model.repository.*;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@Getter
@Component
public class UpdateController {

    //REPO
    private final LanguageRepository languageRepository;
    private final GameLanguageRepository gameLanguageRepository;
    private final HangmanGameRepository hangmanGameRepository;
    private final PlayerRepository playerRepository;
    private final GamesRepository gamesRepository;
    private final CategoryRepository categoryRepository;
    //LOGGER
    private final static Logger LOGGER = Logger.getLogger(UpdateController.class.getName());

    //CORE
    private CoreBot coreBot;

    @Autowired
    public UpdateController(LanguageRepository languageRepository, GameLanguageRepository gameLanguageRepository, HangmanGameRepository hangmanGameRepository, PlayerRepository playerRepository, GamesRepository gamesRepository, CategoryRepository categoryRepository) {
        this.languageRepository = languageRepository;
        this.gameLanguageRepository = gameLanguageRepository;
        this.hangmanGameRepository = hangmanGameRepository;
        this.playerRepository = playerRepository;
        this.gamesRepository = gamesRepository;
        this.categoryRepository = categoryRepository;
    }

    public void registerBot(CoreBot coreBot) {
        this.coreBot = coreBot;
    }

    public void processEvent(Object event) {
        distributeEventsByType(event);
    }

    private void distributeEventsByType(Object event) {
        if (event instanceof SlashCommandInteractionEvent) {
            slashEvent((SlashCommandInteractionEvent) event);
        } else if (event instanceof MessageReceivedEvent) {
            messageReceivedEvent((MessageReceivedEvent) event);
        } else if (event instanceof GuildJoinEvent) {
            joinEvent((GuildJoinEvent) event);
        } else if (event instanceof UserContextInteractionEvent) {
            contextEvent((UserContextInteractionEvent) event);
        } else if (event instanceof ButtonInteractionEvent) {
            buttonEvent((ButtonInteractionEvent) event);
        }
    }

    private void buttonEvent(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        if (buttonId == null) return;

        if (Objects.equals(buttonId, Buttons.BUTTON_RUS.name()) || Objects.equals(buttonId, Buttons.BUTTON_ENG.name())) {
            LanguageButton languageButton = new LanguageButton(gameLanguageRepository);
            languageButton.language(event);
            return;
        }

        if (Objects.equals(buttonId, Buttons.BUTTON_MY_STATS.name())) {
            event.editButton(event.getButton().asDisabled()).queue();
            StatsCommand statsCommand = new StatsCommand(gamesRepository);
            statsCommand.stats(event.getHook());
            return;
        }

        if (Objects.equals(buttonId, Buttons.BUTTON_HELP.name())) {
            event.editButton(event.getButton().asDisabled()).queue();
            HelpCommand helpCommand = new HelpCommand();
            helpCommand.help(event, this);
            return;
        }

        if (Objects.equals(buttonId, Buttons.BUTTON_STOP.name())) {
            event.editButton(event.getButton().asDisabled()).queue();
            StopCommand stopCommand = new StopCommand(hangmanGameRepository);
            stopCommand.stop(event, this);
            return;
        }

        if (Objects.equals(buttonId, Buttons.BUTTON_START_NEW_GAME.name()) || buttonId.matches("BUTTON_START_NEW_GAME_\\d+_\\d+")) {
            HangmanButton hangmanCommand = new HangmanButton();
            hangmanCommand.hangman(event, this);
        }
    }

    private void contextEvent(@NotNull UserContextInteractionEvent event) {
        HangmanCommand hangmanCommand = new HangmanCommand();
        hangmanCommand.hangman(event, this);
    }

    private void messageReceivedEvent(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        long userIdLong = event.getAuthor().getIdLong();
        String message = event.getMessage().getContentRaw();

        if (message.matches("!delete\\s[A-Za-z0-9]+$")) {
            DeleteMessage deleteMessage = new DeleteMessage(gamesRepository, languageRepository, gameLanguageRepository, categoryRepository);
            deleteMessage.delete(event);
            return;
        }

        if (message.toLowerCase().matches("[A-Za-zА-ЯЁа-яё]") || message.toLowerCase().matches("[-A-Za-zА-ЯЁа-яё\\s]{3,24}+")) {
            Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
            if (hangman != null) {
                boolean permission = ChecksClass.check(event);
                if (!permission) return;
                hangman.inputHandler(message.toLowerCase(), event.getMessage());
            }
        }
    }

    private void slashEvent(@NotNull SlashCommandInteractionEvent event) {
        if (event.getUser().isBot()) return;

        switch (event.getName()) {
            case "help" -> {
                HelpCommand helpCommand = new HelpCommand();
                helpCommand.help(event, this);
            }
            case "language" -> {
                LanguageCommand languageCommand = new LanguageCommand(languageRepository, gameLanguageRepository);
                languageCommand.language(event);
            }
            case "mystats", "allstats" -> {
                StatsCommand statsCommand = new StatsCommand(gamesRepository);
                statsCommand.stats(event);
            }
            case "stats" -> {
                StatsCommand statsCommand = new StatsCommand(gamesRepository);
                event.deferReply().queue();
                statsCommand.stats(event.getHook());
            }
            case "stop" -> {
                StopCommand stopCommand = new StopCommand(hangmanGameRepository);
                stopCommand.stop(event, this);
            }
            case "delete" -> {
                DeleteCommand deleteCommand = new DeleteCommand();
                deleteCommand.delete(event);
            }
            case "hg", "multi" -> {
                HangmanCommand hangmanCommand = new HangmanCommand();
                hangmanCommand.hangman(event, this);
            }
            case "category" -> {
                CategoryCommand categoryCommand = new CategoryCommand(categoryRepository);
                categoryCommand.category(event);
            }
        }
    }

    public void sendMessage(Event event, String text, Button button) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            if (slashEvent.isAcknowledged()) slashEvent.getHook().sendMessage(text).addActionRow(button).queue();
            else slashEvent.reply(text).addActionRow(button).queue();
        } else if (event instanceof ButtonInteractionEvent buttonEvent) {
            if (buttonEvent.isAcknowledged()) buttonEvent.getHook().sendMessage(text).addActionRow(button).queue();
            else buttonEvent.reply(text).addActionRow(button).queue();
        }
    }

    public void sendMessage(@NotNull Event event, MessageEmbed build, List<Button> buttonList) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            if (slashEvent.isAcknowledged()) slashEvent.getHook().sendMessageEmbeds(build).addActionRow(buttonList).queue();
            else slashEvent.replyEmbeds(build).addActionRow(buttonList).queue();
        } else if (event instanceof ButtonInteractionEvent buttonEvent) {
            if (buttonEvent.isAcknowledged()) buttonEvent.getHook().sendMessageEmbeds(build).addActionRow(buttonList).queue();
            else buttonEvent.replyEmbeds(build).addActionRow(buttonList).queue();
        }
    }

    private void joinEvent(@NotNull GuildJoinEvent event) {
        JoinEvent joinEvent = new JoinEvent();
        joinEvent.join(event);
    }

    public long getUserId(@NotNull Event event) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            return slashEvent.getUser().getIdLong();
        } else if (event instanceof UserContextInteractionEvent contextEvent) {
            return contextEvent.getUser().getIdLong();
        } else {
            ButtonInteractionEvent buttonInteractionEvent = (ButtonInteractionEvent) event;
            return buttonInteractionEvent.getUser().getIdLong();
        }
    }
}