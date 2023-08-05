package main.hangman;

import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.model.repository.HangmanGameRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class HangmanEmbedUtils {

    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);
    private static final Logger LOGGER = Logger.getLogger(HangmanEmbedUtils.class.getName());

    public static EmbedBuilder hangmanPattern(long userId, String status) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userId);

        if (hangman != null) {
            HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
            String gamePlayer;
            if (hangmanPlayers.length > 1) {
                gamePlayer = jsonGameParsers.getLocale("Game_Players", userId);
            } else {
                gamePlayer = jsonGameParsers.getLocale("Game_Player", userId);
            }

            String userIdWithDiscord = hangman.getUserIdWithDiscord();
            int hangmanErrors = hangman.getHangmanErrors();
            String wordHidden = hangman.getWORD_HIDDEN();
            String guesses = hangman.getGuesses();
            String word = hangman.getWORD().toUpperCase().replaceAll("", " ").trim();
            Hangman.Status hangmanSTATUS = hangman.getSTATUS();

            Map<Long, String> mapGameLanguages = BotStartConfig.getMapGameLanguages();
            String gameLanguage = jsonGameParsers.getLocale("Game_Language", userId);
            String language = mapGameLanguages.get(userId).equals("rus") ? "Кириллица\nКатег.: " + category(userId) : "Latin\nCateg.:" + category(userId);

            embedBuilder.setColor(Color.GREEN);
            //Gamers
            embedBuilder.addField(gamePlayer, userIdWithDiscord, true);
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
            if (hangmanSTATUS == Hangman.Status.LOSE_GAME) {
                String gameWordThatWas = jsonGameParsers.getLocale("Game_Word_That_Was", userId);
                embedBuilder.addField(gameCurrentWord, currentWorldUpper, false);
                embedBuilder.addField(gameWordThatWas, worldUpper, false);
            } else if (hangmanSTATUS == Hangman.Status.WIN_GAME) {
                embedBuilder.addField(gameCurrentWord, worldUpper, false);
            } else {
                embedBuilder.addField(gameCurrentWord, currentWorldUpper, false);
            }

            //Status
            String gameInfo = jsonGameParsers.getLocale("Game_Info", userId);
            embedBuilder.addField(gameInfo, status, false);
        }

        return embedBuilder;
    }

    public static void editMessage(EmbedBuilder embedBuilder, Long userIdLong, HangmanGameRepository hangmanGameRepository) {
        if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
            Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
            if (hangman == null) return;
            HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
            HangmanPlayer hangmanPlayer = hangmanPlayers[0];

            Long guildId = hangmanPlayer.getGuildId();
            long channelId = hangmanPlayer.getChannelId();
            long messageId = hangman.getMessageId();

            if (hangmanPlayer.isFromGuild()) {
                Guild guildById = BotStartConfig.jda.getGuildById(guildId);
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
                    PrivateChannel privateChannelById = BotStartConfig.jda.getPrivateChannelById(channelId);
                    if (privateChannelById == null) {
                        BotStartConfig
                                .jda.retrieveUserById(userIdLong).complete()
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

    public static void editMessageWithButtons(EmbedBuilder embedBuilder, Long userIdLong, List<Button> buttons, HangmanGameRepository hangmanGameRepository) {
        if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
            Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
            if (hangman == null) return;
            HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
            HangmanPlayer hangmanPlayer = hangmanPlayers[0];

            Long guildId = hangmanPlayer.getGuildId();
            long channelId = hangmanPlayer.getChannelId();
            long messageId = hangman.getMessageId();

            if (hangmanPlayer.isFromGuild()) {
                Guild guildById = BotStartConfig.jda.getGuildById(guildId);
                if (guildById != null) {
                    GuildMessageChannel textChannelById = guildById.getTextChannelById(channelId);
                    if (textChannelById == null) textChannelById = guildById.getNewsChannelById(channelId);
                    if (textChannelById == null) textChannelById = guildById.getThreadChannelById(channelId);
                    if (textChannelById != null) {
                        try {
                            textChannelById
                                    .editMessageEmbedsById(messageId, embedBuilder.build())
                                    .setActionRow(buttons)
                                    .queue();
                        } catch (Exception e) {
                            if (e.getMessage().contains("Unknown Message")
                                    || e.getMessage().contains("Unknown Channel")
                                    || e.getMessage().contains("Missing Access")
                                    || e.getMessage().contains("Cannot edit a message authored by another user")) {
                                HangmanRegistry.getInstance().removeHangman(userIdLong);
                                hangmanGameRepository.deleteActiveGame(userIdLong);
                                LOGGER.info("editMessageWithButtons(): " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } else {
            try {
                Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
                if (hangman == null) return;
                HangmanPlayer[] hangmanPlayers = hangman.getHangmanPlayers();
                HangmanPlayer hangmanPlayer = hangmanPlayers[0];

                long channelId = hangmanPlayer.getChannelId();
                long messageId = hangman.getMessageId();

                PrivateChannel privateChannelById = BotStartConfig.jda.getPrivateChannelById(channelId);
                if (privateChannelById == null) {
                    BotStartConfig
                            .jda.retrieveUserById(userIdLong).complete()
                            .openPrivateChannel()
                            .flatMap(channel -> channel.editMessageEmbedsById(messageId, embedBuilder.build())
                                    .setActionRow(buttons))
                            .queue();
                } else {
                    privateChannelById.editMessageEmbedsById(messageId, embedBuilder.build())
                            .setActionRow(buttons)
                            .queue();
                }
            } catch (Exception e) {
                if (e.getMessage().contains("Unknown Message")
                        || e.getMessage().contains("Unknown Channel")
                        || e.getMessage().contains("Missing Access")
                        || e.getMessage().contains("Cannot edit a message authored by another user")) {
                    HangmanRegistry.getInstance().removeHangman(userIdLong);
                    hangmanGameRepository.deleteActiveGame(userIdLong);
                    LOGGER.info("editMessageWithButtons(): " + e.getMessage());
                }
            }
        }
    }

    private static String category(Long userId) {
        String category = BotStartConfig.getMapGameCategory().get(userId);
        String language = BotStartConfig.getMapLanguages().get(userId);
        if (category == null) return Objects.equals(language, "eng") ? "`Any`" : "`Любая`";
        return switch (category) {
            case "colors" -> Objects.equals(language, "eng") ? "`Colors`" : "`Цвета`";
            case "flowers" -> Objects.equals(language, "eng") ? "`Flowers`" : "`Цветы`";
            case "fruits" -> Objects.equals(language, "eng") ? "`Fruits`" : "`Фрукты`";
            default -> Objects.equals(language, "eng") ? "`Any`" : "`Любая`";
        };
    }
}