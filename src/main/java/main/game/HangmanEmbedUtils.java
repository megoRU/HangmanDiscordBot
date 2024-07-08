package main.game;

import main.config.BotStartConfig;
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

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class HangmanEmbedUtils {

    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);
    private static final Logger LOGGER = Logger.getLogger(HangmanEmbedUtils.class.getName());

    public static EmbedBuilder hangmanLayout(long userId, String status) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userId);

        if (hangman != null) {
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

            Map<Long, UserSettings.GameLanguage> mapGameLanguages = BotStartConfig.getMapGameLanguages();

            String gameLanguage = jsonGameParsers.getLocale("Game_Language", userId);
            String language = mapGameLanguages.get(userId)
                    .name()
                    .equals("RU") ? "Кириллица\nКатег.: " + HangmanUtils.category(userId) : "Latin\nCateg.:" + HangmanUtils.category(userId);

            embedBuilder.setColor(Color.GREEN);
            if (!hangman.isCompetitive()) {
                //Gamers
                embedBuilder.addField(gamePlayer, userIdWithDiscord, true);
            } else {
                String against = jsonGameParsers.getLocale("against", userId);
                String againstPlayerWithDiscord = hangman.getAgainstPlayerWithDiscord();

                embedBuilder.addField(against, againstPlayerWithDiscord, true);
            }
            //Game Language
            embedBuilder.addField(gameLanguage, language, true);
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

    //Синхронизация чтобы не было ошибок в верстке и пропаданию части слова
    public static synchronized void editMessage(EmbedBuilder embedBuilder, Long userIdLong, HangmanGameRepository hangmanGameRepository) {
        JDA jda = BotStartConfig.jda;
        if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
            Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
            if (hangman == null) return;
            HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
            HangmanPlayer hangmanPlayer = hangmanPlayers[0];

            Long guildId = hangmanPlayer.getGuildId();
            long channelId = hangmanPlayer.getChannelId();
            long messageId = hangman.getMessageId();

            if (hangmanPlayer.isFromGuild() && guildId != null) {
                Guild guildById = jda.getGuildById(guildId);
                if (guildById != null) {

                    GuildMessageChannel textChannelById = guildById.getTextChannelById(channelId);
                    if (textChannelById == null) textChannelById = guildById.getNewsChannelById(channelId);
                    if (textChannelById == null) textChannelById = guildById.getThreadChannelById(channelId);
                    if (textChannelById != null) {
                        try {
                            textChannelById.editMessageEmbedsById(messageId, embedBuilder.build()).queue();
                        } catch (Exception e) {
                            if (e.getMessage().contains("UNKNOWN_MESSAGE")
                                    || e.getMessage().contains("MISSING_ACCESS")
                                    || e.getMessage().contains("UNKNOWN_CHANNEL")
                                    || e.getMessage().contains("INVALID_AUTHOR_EDIT")) {
                                HangmanRegistry.getInstance().removeHangman(userIdLong);
                                hangmanGameRepository.deleteActiveGame(userIdLong);
                                LOGGER.info("editMessage(): " + e.getMessage());
                            }
                        }
                    }
                }
            } else {
                try {
                    PrivateChannel privateChannelById = jda.getPrivateChannelById(channelId);
                    if (privateChannelById == null) {
                        jda
                                .retrieveUserById(userIdLong)
                                .complete()
                                .openPrivateChannel()
                                .flatMap(channel -> channel.editMessageEmbedsById(messageId, embedBuilder.build()))
                                .queue();
                    } else {
                        privateChannelById.editMessageEmbedsById(messageId, embedBuilder.build()).queue();
                    }
                } catch (Exception e) {
                    if (e.getMessage().contains("Unknown Message")
                            || e.getMessage().contains("Unknown Channel")
                            || e.getMessage().contains("Missing Access")
                            || e.getMessage().contains("Cannot edit a message authored by another user")) {
                        hangmanGameRepository.deleteActiveGame(userIdLong);
                        HangmanRegistry.getInstance().removeHangman(userIdLong);
                        LOGGER.info("editMessage(): " + e.getMessage());
                    }
                }
            }
        }
    }

    public static void editMessageWithButtons(EmbedBuilder embedBuilder, long userId, HangmanGameRepository hangmanGameRepository) {
        JDA jda = BotStartConfig.jda;
        if (HangmanRegistry.getInstance().hasHangman(userId)) {
            Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userId);
            if (hangman == null) return;
            boolean isCompetitive = hangman.isCompetitive();
            int playersCount = hangman.getPlayersCount();
            List<Button> listButtons;

            HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
            HangmanPlayer hangmanPlayer = hangmanPlayers[0];

            if (playersCount > 1) {
                listButtons = HangmanUtils.getListButtons(userId);
            } else if (playersCount == 1 && !isCompetitive) {
                listButtons = HangmanUtils.getListButtons(userId);
            } else {
                listButtons = HangmanUtils.getListCompetitiveButtons(userId);
            }

            Long guildId = hangmanPlayer.getGuildId();
            long channelId = hangmanPlayer.getChannelId();
            long messageId = hangman.getMessageId();

            if (hangmanPlayer.isFromGuild() && guildId != null) {
                Guild guildById = jda.getGuildById(guildId);
                if (guildById != null) {
                    GuildMessageChannel textChannelById = guildById.getTextChannelById(channelId);
                    if (textChannelById == null) textChannelById = guildById.getNewsChannelById(channelId);
                    if (textChannelById == null) textChannelById = guildById.getThreadChannelById(channelId);
                    if (textChannelById != null) {
                        try {
                            textChannelById
                                    .editMessageEmbedsById(messageId, embedBuilder.build())
                                    .setActionRow(listButtons)
                                    .queue();
                        } catch (Exception e) {
                            if (e.getMessage().contains("Unknown Message")
                                    || e.getMessage().contains("Unknown Channel")
                                    || e.getMessage().contains("Missing Access")
                                    || e.getMessage().contains("Cannot edit a message authored by another user")) {
                                HangmanRegistry.getInstance().removeHangman(userId);
                                hangmanGameRepository.deleteActiveGame(userId);
                                LOGGER.info("editMessageWithButtons(): " + e.getMessage());
                            }
                        }
                    }
                }
            } else {
                try {
                    PrivateChannel privateChannelById = jda.getPrivateChannelById(channelId);
                    if (privateChannelById == null) {
                        jda.retrieveUserById(userId).complete()
                                .openPrivateChannel()
                                .flatMap(channel -> channel.editMessageEmbedsById(messageId, embedBuilder.build())
                                        .setActionRow(listButtons))
                                .queue();
                    } else {
                        privateChannelById
                                .editMessageEmbedsById(messageId, embedBuilder.build())
                                .setActionRow(listButtons)
                                .queue();
                    }
                } catch (Exception e) {
                    if (e.getMessage().contains("Unknown Message")
                            || e.getMessage().contains("Unknown Channel")
                            || e.getMessage().contains("Missing Access")
                            || e.getMessage().contains("Cannot edit a message authored by another user")) {
                        HangmanRegistry.getInstance().removeHangman(userId);
                        hangmanGameRepository.deleteActiveGame(userId);
                        LOGGER.info("editMessageWithButtons(): " + e.getMessage());
                    }
                }
            }
        }
    }
}