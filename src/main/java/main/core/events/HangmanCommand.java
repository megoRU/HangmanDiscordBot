package main.core.events;

import main.config.BotStartConfig;
import main.game.*;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class HangmanCommand {

    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final HangmanGameRepository hangmanGameRepository;
    private final HangmanDataSaving hangmanDataSaving;
    private final HangmanResult hangmanResult;
    private final HangmanRegistry instance = HangmanRegistry.getInstance();

    @Autowired
    public HangmanCommand(HangmanGameRepository hangmanGameRepository,
                          HangmanDataSaving hangmanDataSaving,
                          HangmanResult hangmanResult) {
        this.hangmanGameRepository = hangmanGameRepository;
        this.hangmanDataSaving = hangmanDataSaving;
        this.hangmanResult = hangmanResult;
    }

    public void hangman(@NotNull GenericCommandInteractionEvent event) {
        long userIdLong = event.getUser().getIdLong();
        long channelIdLong = event.getMessageChannel().getIdLong();

        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            slashEvent.getChannel().sendTyping().queue();
        }

        //Проверяем установлен ли язык. Если нет - то возвращаем в чат ошибку
        Map<Long, UserSettings.GameLanguage> mapGameLanguages = BotStartConfig.getMapGameLanguages();
        if (!mapGameLanguages.containsKey(userIdLong)) {
            String hangmanListenerNeedSetLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userIdLong);
            event.reply(hangmanListenerNeedSetLanguage)
                    .addActionRow(HangmanUtils.BUTTON_RUSSIAN, HangmanUtils.BUTTON_ENGLISH)
                    .setEphemeral(true)
                    .queue();
            //Проверяем если игрок уже играет. То присылаем в чат уведомление
        } else if (instance.hasCompetitive(userIdLong)) {
            String youArePlayNow = jsonParsers.getLocale("you_are_search_now", userIdLong);
            event.reply(youArePlayNow)
                    .setActionRow(HangmanUtils.getButtonLeaveSearch(userIdLong))
                    .setEphemeral(true)
                    .queue();
        } else if (instance.hasHangman(userIdLong)) {
            String hangmanListenerYouPlay = jsonParsers.getLocale("Hangman_Listener_You_Play", userIdLong);
            event.reply(hangmanListenerYouPlay)
                    .setActionRow(HangmanUtils.getButtonStop(userIdLong))
                    .setEphemeral(true)
                    .queue();
            //Если всё хорошо, создаем игру
        } else {
            Guild guild = event.getGuild();

            HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, guild != null ? guild.getIdLong() : null, channelIdLong);
            HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder();
            hangmanBuilder.addHangmanPlayer(hangmanPlayer);
            hangmanBuilder.setHangmanDataSaving(hangmanDataSaving);
            hangmanBuilder.setHangmanGameRepository(hangmanGameRepository);
            hangmanBuilder.setHangmanResult(hangmanResult);

            if (event.getName().equals("multi") && event instanceof SlashCommandInteractionEvent s) {
                multi((SlashCommandInteractionEvent) event, hangmanBuilder);
            } else if (event.getName().equals("multiple") && event instanceof SlashCommandInteractionEvent) {
                multiple((SlashCommandInteractionEvent) event, hangmanBuilder);
            } else if (event.getName().equals("multi") && event instanceof UserContextInteractionEvent) {
                userContext((UserContextInteractionEvent) event, hangmanBuilder);
            } else if (event.getName().equals("hg") || event.getName().equals("play")) {
                String createGame = jsonParsers.getLocale("create_game", userIdLong);
                event.reply(createGame)
                        .setEphemeral(true)
                        .queue();

                startGame(event, hangmanBuilder);
            }
        }
    }

    private void startGame(GenericCommandInteractionEvent event, HangmanBuilder.Builder hangmanBuilder) {
        Hangman hangman = hangmanBuilder.build();

        //Заполнение коллекции
        HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
        for (HangmanPlayer player : hangmanPlayers) {
            instance.setHangman(player.getUserId(), hangman);
        }

        hangman.startGame(event.getMessageChannel());
    }

    private void userContext(UserContextInteractionEvent userContextEvent, HangmanBuilder.Builder hangmanBuilder) {
        Guild guild = userContextEvent.getGuild();
        long channelIdLong = userContextEvent.getChannelIdLong();

        Member targetMember = userContextEvent.getTargetMember();
        if (targetMember == null) {
            userContextEvent.reply("User is `null`").setEphemeral(true).queue();
            return;
        }
        User targetUser = targetMember.getUser();
        if (isUserError(targetUser, userContextEvent)) return;

        HangmanPlayer hangmanPlayerSecond = new HangmanPlayer(targetUser.getIdLong(), guild != null ? guild.getIdLong() : null, channelIdLong);
        hangmanBuilder.addHangmanPlayer(hangmanPlayerSecond);

        long userIdLong = userContextEvent.getUser().getIdLong();
        String createGame = jsonParsers.getLocale("create_game", userIdLong);
        userContextEvent.reply(createGame).setEphemeral(true).queue();

        startGame(userContextEvent, hangmanBuilder);
    }

    private void multi(SlashCommandInteractionEvent slashCommandInteractionEvent, HangmanBuilder.Builder hangmanBuilder) {
        User user = slashCommandInteractionEvent.getOption("user", OptionMapping::getAsUser);
        long userIdLong = slashCommandInteractionEvent.getUser().getIdLong();
        Guild guild = slashCommandInteractionEvent.getGuild();
        long channelIdLong = slashCommandInteractionEvent.getChannelIdLong();

        if (user == null) {
            slashCommandInteractionEvent.reply("User is `null`").setEphemeral(true).queue();
            return;
        }
        if (isUserError(user, slashCommandInteractionEvent)) return;

        HangmanPlayer hangmanPlayerSecond = new HangmanPlayer(user.getIdLong(), guild != null ? guild.getIdLong() : null, channelIdLong);
        hangmanBuilder.addHangmanPlayer(hangmanPlayerSecond);

        String createGame = jsonParsers.getLocale("create_game", userIdLong);
        slashCommandInteractionEvent.reply(createGame).setEphemeral(true).queue();

        startGame(slashCommandInteractionEvent, hangmanBuilder);
    }

    private void multiple(SlashCommandInteractionEvent slashCommandInteractionEvent, HangmanBuilder.Builder hangmanBuilder) {
        long userIdLong = slashCommandInteractionEvent.getUser().getIdLong();
        Guild guild = slashCommandInteractionEvent.getGuild();
        long channelIdLong = slashCommandInteractionEvent.getChannelIdLong();

        Mentions users = slashCommandInteractionEvent.getOption("users", OptionMapping::getMentions);
        if (users == null) {
            String usersMentionsNull = jsonParsers.getLocale("users_mentions_null", userIdLong);
            slashCommandInteractionEvent.reply(usersMentionsNull).setEphemeral(true).queue();
            return;
        }

        List<User> usersList = users.getUsers()
                .stream()
                .filter(user -> !user.isBot())
                .filter(user -> !user.equals(slashCommandInteractionEvent.getUser()))
                .filter(user -> !instance.hasHangman(user.getIdLong()))
                .distinct()
                .toList();

        for (User user : usersList) {
            long userId = user.getIdLong();
            HangmanPlayer hgPlayer = new HangmanPlayer(userId, guild != null ? guild.getIdLong() : null, channelIdLong);
            hangmanBuilder.addHangmanPlayer(hgPlayer);
        }

        String createGame = jsonParsers.getLocale("create_game", userIdLong);
        slashCommandInteractionEvent.reply(createGame).setEphemeral(true).queue();

        startGame(slashCommandInteractionEvent, hangmanBuilder);
    }

    private boolean isUserError(@NotNull User user, GenericCommandInteractionEvent event) {
        long targetUser = user.getIdLong(); //всё верно
        long userEvent = event.getUser().getIdLong(); //всё верно

        if (user.isBot()) {
            String playWithBot = jsonParsers.getLocale("play_with_bot", userEvent);
            event.reply(playWithBot).setEphemeral(true).queue();
            return true;
        } else if (userEvent == targetUser) {
            String playWithYourself = jsonParsers.getLocale("play_with_yourself", userEvent);
            event.reply(playWithYourself).setEphemeral(true).queue();
            return true;
        } else if (instance.hasHangman(targetUser)) {
            String secondPlayerAlreadyPlaying = jsonParsers.getLocale("second_player_already_playing", userEvent);
            event.reply(secondPlayerAlreadyPlaying).setEphemeral(true).queue();
            return true;
        } else {
            return false;
        }
    }
}