package main.controller;

import lombok.Getter;
import main.core.ChecksClass;
import main.core.CoreBot;
import main.core.events.*;
import main.enums.Buttons;
import main.game.Hangman;
import main.game.HangmanDataSaving;
import main.game.HangmanInputs;
import main.game.HangmanResult;
import main.game.api.HangmanAPI;
import main.game.core.HangmanRegistry;
import main.model.repository.CompetitiveQueueRepository;
import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.UserSettingsRepository;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static main.config.BotStartConfig.jda;

@Getter
@Component
public class UpdateController {

    //REPO
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final CompetitiveQueueRepository competitiveQueueRepository;
    private final HangmanDataSaving hangmanDataSaving;
    private final HangmanResult hangmanResult;
    private final HangmanInputs hangmanInputs;
    private final HangmanAPI hangmanAPI;

    //LOGGER
    private final static Logger LOGGER = LoggerFactory.getLogger(UpdateController.class.getName());

    //CORE
    private CoreBot coreBot;

    @Autowired
    public UpdateController(HangmanGameRepository hangmanGameRepository,
                            GamesRepository gamesRepository,
                            UserSettingsRepository userSettingsRepository,
                            CompetitiveQueueRepository competitiveQueueRepository,
                            HangmanDataSaving hangmanDataSaving,
                            HangmanResult hangmanResult,
                            HangmanInputs hangmanInputs,
                            HangmanAPI hangmanAPI) {
        this.hangmanGameRepository = hangmanGameRepository;
        this.gamesRepository = gamesRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.competitiveQueueRepository = competitiveQueueRepository;
        this.hangmanDataSaving = hangmanDataSaving;
        this.hangmanResult = hangmanResult;
        this.hangmanInputs = hangmanInputs;
        this.hangmanAPI = hangmanAPI;
    }

    public void registerBot(CoreBot coreBot) {
        this.coreBot = coreBot;
    }

    public void processEvent(Object event) {
        distributeEventsByType(event);
    }

    private void distributeEventsByType(Object event) {
        if (event instanceof SlashCommandInteractionEvent slashCommandInteractionEvent) {
            LOGGER.info(slashCommandInteractionEvent.getName());
            slashEvent(slashCommandInteractionEvent);
        } else if (event instanceof MessageReceivedEvent messageReceivedEvent) {
            messageReceivedEvent(messageReceivedEvent);
        } else if (event instanceof GuildJoinEvent guildJoinEvent) {
            joinEvent(guildJoinEvent);
        } else if (event instanceof UserContextInteractionEvent userContextInteractionEvent) {
            LOGGER.info(userContextInteractionEvent.getName());
            contextEvent(userContextInteractionEvent);
        } else if (event instanceof ButtonInteractionEvent buttonInteractionEvent) {
            LOGGER.info(buttonInteractionEvent.getInteraction().getButton().getLabel());
            buttonEvent(buttonInteractionEvent);
        } else if (event instanceof MessageDeleteEvent messageDeleteEvent) {
            messageDeleteEvent(messageDeleteEvent);
        }
    }

    private void messageDeleteEvent(MessageDeleteEvent messageDeleteEvent) {
        DeleteEvent deleteEvent = new DeleteEvent(hangmanGameRepository);
        deleteEvent.handle(messageDeleteEvent);
    }

    private void buttonEvent(@NotNull ButtonInteractionEvent event) {
        boolean permission = ChecksClass.check(event);
        if (!permission) return;

        String buttonId = event.getButton().getId();
        if (buttonId == null) return;

        if (Objects.equals(buttonId, Buttons.BUTTON_RUS.name()) || Objects.equals(buttonId, Buttons.BUTTON_ENG.name())) {
            LanguageButton languageButton = new LanguageButton(userSettingsRepository);
            languageButton.language(event);
            return;
        }

        if (Objects.equals(buttonId, Buttons.BUTTON_MY_STATS.name())) {
            StatsCommand statsCommand = new StatsCommand(gamesRepository);
            statsCommand.stats(event);
            return;
        }

        if (Objects.equals(buttonId, Buttons.BUTTON_HELP.name())) {
            event.editButton(event.getButton().asDisabled()).queue();
            HelpCommand helpCommand = new HelpCommand();
            helpCommand.help(event, this);
            return;
        }

        if (Objects.equals(buttonId, Buttons.BUTTON_START_GAME_GPT.name())) {
            event.editButton(event.getButton().asDisabled()).queue();
            CompetitivePlayGPT competitivePlayGPT = new CompetitivePlayGPT(hangmanAPI, hangmanDataSaving);
            competitivePlayGPT.competitive(event);
            return;
        }

        if (Objects.equals(buttonId, Buttons.BUTTON_STOP.name())) {
            event.editButton(event.getButton().asDisabled()).queue();
            StopCommand stopCommand = new StopCommand(hangmanGameRepository);
            stopCommand.stop(event, this);
            return;
        }

        if (Objects.equals(buttonId, Buttons.BUTTON_COMPETITIVE_STOP.name())) {
            event.editButton(event.getButton().asDisabled()).queue();
            CompetitiveStopButton competitiveStopButton = new CompetitiveStopButton(competitiveQueueRepository);
            competitiveStopButton.stop(event);
            return;
        }

        if (Objects.equals(buttonId, Buttons.BUTTON_START_NEW_GAME.name())) {
            HangmanButton hangmanCommand = new HangmanButton(hangmanDataSaving);
            hangmanCommand.hangman(event);
            return;
        }
        if (Objects.equals(buttonId, Buttons.BUTTON_COMPETITIVE_AGAIN.name())) {
            CompetitivePlayButton competitivePlayButton = new CompetitivePlayButton(competitiveQueueRepository);
            competitivePlayButton.competitive(event);
        }
    }

    private void contextEvent(@NotNull UserContextInteractionEvent event) {
        boolean permission = ChecksClass.check(event);
        if (!permission) return;

        HangmanCommand hangmanCommand = new HangmanCommand(hangmanDataSaving, hangmanAPI);
        hangmanCommand.hangman(event);
    }

    private void messageReceivedEvent(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        long userIdLong = event.getAuthor().getIdLong();
        String message = event.getMessage().getContentRaw();

        if (message.equals("ping-test")) {
            jda.getRestPing().queue((time) ->
                    event.getChannel().sendMessageFormat("Ping: %d ms", time).queue()
            );
            return;
        }

        if (message.matches("!delete\\s[A-Za-z0-9]+$")) {
            DeleteMessage deleteMessage = new DeleteMessage(gamesRepository, userSettingsRepository);
            deleteMessage.delete(event);
            return;
        }

        if (message.toLowerCase().matches("[A-Za-zА-ЯЁа-яё]") || message.toLowerCase().matches("[-A-Za-zА-ЯЁа-яё\\s]{3,60}+")) {
            Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
            if (hangman != null) {
                boolean permission = ChecksClass.check(event);
                if (!permission) return;
                hangmanInputs.handler(message.toLowerCase(), event.getMessage(), hangman);
            }
        }
    }

    private void slashEvent(@NotNull SlashCommandInteractionEvent event) {
        boolean permission = ChecksClass.check(event);
        if (!permission) return;

        if (event.getUser().isBot()) return;

        switch (event.getName()) {
            case "help" -> {
                HelpCommand helpCommand = new HelpCommand();
                helpCommand.help(event, this);
            }
            case "language" -> {
                LanguageCommand languageCommand = new LanguageCommand(userSettingsRepository);
                languageCommand.language(event);
            }
            case "bot-statistics", "statistics" -> {
                StatsCommand statsCommand = new StatsCommand(gamesRepository);
                statsCommand.stats(event);
            }
            case "leadboard" -> {
                LeadboardCommand leadboardCommand = new LeadboardCommand(gamesRepository);
                leadboardCommand.board(event);
            }
            case "quit" -> {
                StopCommand stopCommand = new StopCommand(hangmanGameRepository);
                stopCommand.stop(event, this);
            }
            case "delete" -> {
                DeleteCommand deleteCommand = new DeleteCommand();
                deleteCommand.delete(event);
            }
            case "multi", "play", "multiple", "chatgpt" -> {
                HangmanCommand hangmanCommand = new HangmanCommand(hangmanDataSaving, hangmanAPI);
                hangmanCommand.hangman(event);
            }
            case "category" -> {
                CategoryCommand categoryCommand = new CategoryCommand(userSettingsRepository);
                categoryCommand.category(event);
            }
            case "competitive" -> {
                CompetitiveCommand competitiveCommand = new CompetitiveCommand(competitiveQueueRepository);
                competitiveCommand.competitive(event, this);
            }
        }
    }

    public void sendMessage(Event event, String text) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            if (slashEvent.isAcknowledged()) slashEvent.getHook().sendMessage(text).queue();
            else slashEvent.reply(text).queue();
        } else if (event instanceof ButtonInteractionEvent buttonEvent) {
            if (buttonEvent.isAcknowledged()) buttonEvent.getHook().sendMessage(text).queue();
            else buttonEvent.reply(text).queue();
        }
    }

    public void sendMessage(@NotNull Event event, MessageEmbed build, List<Button> buttonList) {
        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            if (slashEvent.isAcknowledged())
                slashEvent.getHook().sendMessageEmbeds(build).addActionRow(buttonList).queue();
            else slashEvent.replyEmbeds(build).addActionRow(buttonList).queue();
        } else if (event instanceof ButtonInteractionEvent buttonEvent) {
            if (buttonEvent.isAcknowledged())
                buttonEvent.getHook().sendMessageEmbeds(build).addActionRow(buttonList).queue();
            else buttonEvent.replyEmbeds(build).addActionRow(buttonList).queue();
        }
    }

    public void sendMessage(String userId, String text) {
        RestAction<User> action = jda.retrieveUserById(userId);
        action.submit()
                .thenCompose((user) -> user.openPrivateChannel().submit())
                .thenCompose((channel) -> channel.sendMessage(text).submit())
                .whenComplete((v, throwable) -> {
                    if (throwable != null) {
                        if (throwable.getMessage().contains("50007: Cannot send messages to this user")) {
                            LOGGER.error("50007: Cannot send messages to this user", throwable);
                        }
                    }
                });
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