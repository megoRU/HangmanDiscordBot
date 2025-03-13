package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.game.Hangman;
import main.game.HangmanBuilder;
import main.game.HangmanDataSaving;
import main.game.HangmanPlayer;
import main.game.api.HangmanAPI;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class HangmanCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(HangmanCommand.class.getName());
    private final static JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private final HangmanDataSaving hangmanDataSaving;
    private final static HangmanRegistry instance = HangmanRegistry.getInstance();
    private final HangmanAPI hangmanAPI;

    public void hangman(@NotNull GenericCommandInteractionEvent event) {
        long userIdLong = event.getUser().getIdLong();
        long channelIdLong = event.getMessageChannel().getIdLong();

        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            slashEvent.getChannel().sendTyping().queue();
        }

        //Проверяем установлен ли язык. Если нет - то возвращаем в чат ошибку
        Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
        UserSettings userSettings = userSettingsMap.get(userIdLong);

        if (userSettings == null) {
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

            UserSettings.GameLanguage userGameLanguage = userSettings.getGameLanguage();

            HangmanPlayer hangmanPlayer = new HangmanPlayer(userIdLong, guild != null ? guild.getIdLong() : null, channelIdLong, userGameLanguage);
            HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder();
            hangmanBuilder.addHangmanPlayer(hangmanPlayer);

            if (event.getName().equals("multi") && event instanceof SlashCommandInteractionEvent slashCommandInteractionEvent) {
                multi(slashCommandInteractionEvent, hangmanBuilder);
            } else if (event.getName().equals("multiple") && event instanceof SlashCommandInteractionEvent slashCommandInteractionEvent) {
                multiple(slashCommandInteractionEvent, hangmanBuilder);
            } else if (event.getName().equals("multi") && event instanceof UserContextInteractionEvent userContextInteractionEvent) {
                userContext(userContextInteractionEvent, hangmanBuilder);
            } else if (event.getName().equals("chatgpt") && event instanceof SlashCommandInteractionEvent slashCommandInteractionEvent) {
                chatgpt(slashCommandInteractionEvent, hangmanBuilder, userGameLanguage);
            } else if (event.getName().equals("play")) {
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

        hangman.startGame(event.getMessageChannel(), hangmanDataSaving);
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

    private void chatgpt(SlashCommandInteractionEvent slashCommandInteractionEvent,
                         HangmanBuilder.Builder hangmanBuilder,
                         UserSettings.GameLanguage gameLanguage) {
        try {
            long userIdLong = slashCommandInteractionEvent.getUser().getIdLong();
            String word = hangmanAPI.getWord(userIdLong);

            HangmanBuilder.Builder hangmanBuilderGPT = new HangmanBuilder.Builder();
            hangmanBuilderGPT.setCompetitive(true);

            HangmanPlayer hangmanPlayerGPT = new HangmanPlayer(-userIdLong, null, null, gameLanguage);

            hangmanBuilder.setAgainstPlayerId(-userIdLong);
            hangmanBuilder.setCompetitive(true);

            //
            hangmanBuilderGPT.setAgainstPlayerId(userIdLong);
            hangmanBuilderGPT.addHangmanPlayer(hangmanPlayerGPT);

            //Build
            Hangman build = hangmanBuilder.build();
            Hangman buildGPT = hangmanBuilderGPT.build();

            instance.setHangman(userIdLong, build);
            instance.setHangman(-userIdLong, buildGPT);

            String createGame = jsonParsers.getLocale("create_game", userIdLong);
            slashCommandInteractionEvent.reply(createGame).setEphemeral(true).queue();

            build.startGame(slashCommandInteractionEvent.getMessageChannel(), word, hangmanDataSaving);
            buildGPT.startGame(word, hangmanDataSaving);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
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