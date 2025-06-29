package main.game;

import main.config.BotStartConfig;
import main.core.ChecksClass;
import main.enums.GameStatus;
import main.game.core.HangmanRegistry;
import main.game.utils.HangmanUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.UserSettings;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HangmanEmbedUtils {

    private static final ConcurrentHashMap<Long, Object> locks = new ConcurrentHashMap<>();
    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);
    private final static Logger LOGGER = LoggerFactory.getLogger(HangmanEmbedUtils.class.getName());

    public static EmbedBuilder hangmanLayout(long userId, String status) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userId);

        if (hangman != null && !hangman.isChatGPT()) {
            HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
            userId = HangmanUtils.getHangmanFirstPlayer(hangmanPlayers);
            String gamePlayer;
            if (hangmanPlayers.length > 1) {
                gamePlayer = jsonGameParsers.getLocale("Game_Players", userId);
            } else {
                gamePlayer = jsonGameParsers.getLocale("Game_Player", userId);
            }

            String userIdWithDiscord = hangman.getUserIdWithDiscord();
            int hangmanErrors = hangman.getHangmanErrors();
            String wordHidden = hangman.getWORD_HIDDEN();
            String guesses = HangmanUtils.getGuesses(hangman.getGuesses());
            String word = hangman.getWORD().toUpperCase().replaceAll("", " ").trim();
            GameStatus gameStatus = hangman.getGameStatus();

            Map<Long, UserSettings> userSettingsMap = BotStartConfig.userSettingsMap;
            UserSettings userSettings = userSettingsMap.get(userId);

            UserSettings.GameLanguage mapGameLanguages = userSettings.getGameLanguage();

            String gameSettings = jsonGameParsers.getLocale("Game_Settings", userId);
            String language = mapGameLanguages
                    .name()
                    .equals("RU") ?
                    String.format("""
                            Язык: `Кириллица`
                            Катег.: %s
                            """, HangmanUtils.category(userId)) :
                    String.format("""
                            Lang.: `Latin`
                            Categ.: %s
                            """, HangmanUtils.category(userId));

            embedBuilder.setColor(Color.GREEN);
            if (!hangman.isCompetitive()) {
                //Gamers
                embedBuilder.addField(gamePlayer, userIdWithDiscord, true);
            } else {
                String against = jsonGameParsers.getLocale("against", userId);
                String againstPlayerWithDiscord = hangman.getAgainstPlayerWithDiscord();

                embedBuilder.addField(against, againstPlayerWithDiscord, true);
            }
            //Game Settings
            embedBuilder.addField(gameSettings, language, true);
            //Image
            embedBuilder.setThumbnail(HangmanUtils.getImage(hangmanErrors));

            //Guesses
            if (!guesses.isEmpty()) {
                String gameGuesses = jsonGameParsers.getLocale("Game_Guesses", userId);
                String guessesFormat = String.format("`%s`", guesses.toUpperCase());
                embedBuilder.addField(gameGuesses, guessesFormat, false);
            }

            //Current Hidden Word
            String gameCurrentWord = jsonGameParsers.getLocale("Game_Current_Word", userId);
            String currentWorldUpper = String.format("`%s`", wordHidden.toUpperCase());
            String worldUpper = String.format("`%s`", word);

            //Game Word That Was
            if (gameStatus == GameStatus.LOSE_GAME) {
                String gameWordThatWas = jsonGameParsers.getLocale("Game_Word_That_Was", userId);
                embedBuilder.addField(gameCurrentWord, currentWorldUpper, false);
                embedBuilder.addField(gameWordThatWas, worldUpper, false);
            } else if (gameStatus == GameStatus.WIN_GAME) {
                embedBuilder.addField(gameCurrentWord, worldUpper, false);
            } else {
                embedBuilder.addField(gameCurrentWord, currentWorldUpper, false);
            }

            //Status
            String gameInfo = jsonGameParsers.getLocale("Game_Info", userId);
            embedBuilder.addField(gameInfo, status, false);

            if (hangman.isCompetitive()) {
                String competitiveGame = jsonGameParsers.getLocale("competitive_game", userId);
                embedBuilder.setFooter(competitiveGame);
            }

            if (hangman.isOpponentLose()) {
                String gameIsNotOver = jsonGameParsers.getLocale("Game_Is_Not_Over", userId);
                String opponentLost = jsonGameParsers.getLocale("Game_Opponent_Lost", userId);
                embedBuilder.addField(opponentLost, gameIsNotOver, false);
            }
        }

        return embedBuilder;
    }

    public static void editMessage(EmbedBuilder embedBuilder, long userId, boolean withButtons, HangmanGameRepository hangmanGameRepository) {
        locks.putIfAbsent(userId, new Object());
        JDA jda = BotStartConfig.jda;

        synchronized (locks.get(userId)) {
            Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userId);
            if (hangman == null || hangman.isChatGPT()) return;

            HangmanPlayer hangmanPlayer = hangman.getHangmanPlayers()[0];
            Long guildId = hangmanPlayer.getGuildId();
            Long channelId = hangmanPlayer.getChannelId();
            Long messageId = hangman.getMessageId();

            List<Button> buttons = withButtons ? getButtons(hangman, userId) : null;

            if (hangmanPlayer.isFromGuild() && guildId != null) {
                editGuildMessage(jda, embedBuilder, guildId, channelId, messageId, buttons, userId, hangmanGameRepository);
            } else {
                editPrivateMessage(jda, embedBuilder, userId, channelId, messageId, buttons, hangmanGameRepository);
            }

            locks.remove(userId);
        }
    }

    private static List<Button> getButtons(Hangman hangman, long userId) {
        int playersCount = hangman.getPlayersCount();
        boolean competitive = hangman.isCompetitive();

        if (playersCount > 1) {
            return HangmanUtils.getListButtons(userId);
        } else if (playersCount == 1 && !competitive) {
            return HangmanUtils.getListButtons(userId);
        } else if (HangmanUtils.isChatGPT(hangman.getAgainstPlayerEmbedded())) {
            return List.of(HangmanUtils.getButtonGPT(userId));
        } else {
            return HangmanUtils.getListCompetitiveButtons(userId);
        }
    }

    private static void editGuildMessage(JDA jda, EmbedBuilder embedBuilder, Long guildId, Long channelId, Long messageId, List<Button> buttons, long userId, HangmanGameRepository hangmanGameRepository) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        GuildMessageChannel channel = getGuildChannel(guild, channelId);

        if (channel != null) {
            if (ChecksClass.check(channel)) {
                MessageEditAction action = channel.editMessageEmbedsById(messageId, embedBuilder.build());
                if (buttons != null) action.setActionRow(buttons);
                action.queue(null, throwable -> handleEditException(throwable, userId, hangmanGameRepository));
            }
        }
    }

    private static GuildMessageChannel getGuildChannel(Guild guild, Long channelId) {
        GuildMessageChannel channel = guild.getTextChannelById(channelId);
        if (channel == null) channel = guild.getNewsChannelById(channelId);
        if (channel == null) channel = guild.getThreadChannelById(channelId);
        return channel;
    }

    private static void editPrivateMessage(JDA jda, EmbedBuilder embedBuilder, long userId, Long channelId, Long messageId, List<Button> buttons, HangmanGameRepository hangmanGameRepository) {
        PrivateChannel channel = jda.getPrivateChannelById(channelId);

        if (channel == null) {
            jda.retrieveUserById(userId)
                    .queue(user ->
                            user.openPrivateChannel().queue(
                                    userChannel -> {
                                        MessageEditAction action = userChannel.editMessageEmbedsById(messageId, embedBuilder.build());
                                        if (buttons != null) action.setActionRow(buttons);
                                        action.queue(null, throwable -> handleEditException(throwable, userId, hangmanGameRepository));
                                    }, throwable -> handleEditException(throwable, userId, hangmanGameRepository)
                            )
                    );
        } else {
            MessageEditAction action = channel.editMessageEmbedsById(messageId, embedBuilder.build());
            if (buttons != null) action.setActionRow(buttons);
            action.queue(null, throwable -> handleEditException(throwable, userId, hangmanGameRepository));
        }
    }

    private static void handleEditException(Throwable throwable, long userId, HangmanGameRepository hangmanGameRepository) {
        String message = throwable.getMessage();
        if (message.contains("Unknown") || message.contains("Access") || message.contains("Cannot edit")) {
            HangmanRegistry.getInstance().removeHangman(userId);
            hangmanGameRepository.deleteActiveGame(userId);
            LOGGER.info("editMessage: {}", message);
        }
    }
}