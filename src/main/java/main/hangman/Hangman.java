package main.hangman;

import api.megoru.ru.MegoruAPI;
import api.megoru.ru.entity.GameWordLanguage;
import api.megoru.ru.impl.MegoruAPIImpl;
import lombok.Getter;
import lombok.Setter;
import main.config.BotStartConfig;
import main.hangman.impl.EndGameButtons;
import main.hangman.impl.GetImage;
import main.hangman.impl.HangmanHelper;
import main.hangman.impl.SetGameLanguageButtons;
import main.jsonparser.JSONGameParsers;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveHangman;
import main.model.entity.Game;
import main.model.entity.Player;
import main.model.repository.GamesRepository;
import main.model.repository.HangmanGameRepository;
import main.model.repository.PlayerRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;

@Setter
@Getter
public class Hangman implements HangmanHelper {

    //Localisation
    private static final JSONGameParsers jsonGameParsers = new JSONGameParsers();
    private static final JSONParsers jsonParsers = new JSONParsers();

    //Repository
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;

    private final Set<String> guesses = new LinkedHashSet<>();
    private final List<Integer> index = new ArrayList<>();

    //User|Guild|Channel data
    private final String userId;
    private final String guildId;
    private final Long channelId;

    private final List<Message> messageList = new LinkedList<>();
    private int countUsedLetters;
    private String WORD = null;
    private char[] wordToChar;
    private String WORD_HIDDEN = "";
    private String currentHiddenWord;

    private int hangmanErrors = 0;

    private Logger LOGGER = Logger.getLogger(Hangman.class.getName());

    public Hangman(String userId, String guildId, Long channelId,
                   HangmanGameRepository hangmanGameRepository,
                   GamesRepository gamesRepository,
                   PlayerRepository playerRepository) {
        this.hangmanGameRepository = hangmanGameRepository;
        this.gamesRepository = gamesRepository;
        this.playerRepository = playerRepository;
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
    }

    //TODO: Работает, но изменить время на Instant желательно.
    private EmbedBuilder updateEmbedBuilder() {
        autoInsert();

        Instant instant = Instant.now().plusSeconds(600L);
        HangmanRegistry.getInstance().getTimeCreatedGame().put(Long.valueOf(userId), LocalDateTime.from(OffsetDateTime.parse(String.valueOf(Instant.now()))));
        HangmanRegistry.getInstance().getEndAutoDelete().put(Long.valueOf(userId), String.valueOf(OffsetDateTime.parse(String.valueOf(instant))));

        return embedBuilder(
                Color.GREEN,
                "<@" + Long.parseLong(userId) + ">",
                jsonGameParsers.getLocale("Game_Start", userId)
                        .replaceAll("\\{0}", BotStartConfig.getMapPrefix().get(guildId)
                                == null
                                ? "!"
                                : BotStartConfig.getMapPrefix().get(guildId)),
                false,
                false,
                null
        );
    }

    public void startGame(@NotNull SlashCommandInteractionEvent event) {
        try {
            if (BotStartConfig.getMapGameLanguages().get(getUserId()) == null) {
                EmbedBuilder needSetLanguage = new EmbedBuilder();

                needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                needSetLanguage.setColor(Color.GREEN);
                needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getId()));

                event.replyEmbeds(needSetLanguage.build()).addActionRow(SetGameLanguageButtons.getList).queue();
                clearingCollections();
                return;
            }

            MegoruAPI megoruAPI = new MegoruAPIImpl("this bot don`t use token");
            GameWordLanguage gameWordLanguage = new GameWordLanguage();
            gameWordLanguage.setLanguage(BotStartConfig.getMapGameLanguages().get(userId));

            try {
                WORD = megoruAPI.getWord(gameWordLanguage).getWord();
                if (WORD != null) {
                    wordToChar = WORD.toCharArray(); // Преобразуем строку str в массив символов (char)
                    hideWord(WORD.length());
                }
            } catch (Exception e) {
                EmbedBuilder wordIsNull = new EmbedBuilder();
                wordIsNull.setTitle(jsonParsers.getLocale("errors_title", event.getUser().getId()));
                wordIsNull.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                wordIsNull.setColor(Color.RED);
                wordIsNull.setDescription(jsonParsers.getLocale("errors", event.getUser().getId()));

                event.replyEmbeds(wordIsNull.build()).queue();
                clearingCollections();
                return;
            }

            event.replyEmbeds(updateEmbedBuilder().build()).queue(m -> m.retrieveOriginal().queue(this::createEntityInDataBase));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startGame(MessageChannel textChannel, String avatarUrl, String userName) {
        try {
            if (BotStartConfig.getMapGameLanguages().get(getUserId()) == null) {
                EmbedBuilder needSetLanguage = new EmbedBuilder();

                needSetLanguage.setAuthor(userName, null, avatarUrl);
                needSetLanguage.setColor(0x00FF00);
                needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", this.userId));

                textChannel.sendMessageEmbeds(needSetLanguage.build()).setActionRow(SetGameLanguageButtons.getList).queue();
                clearingCollections();
                return;
            }

            MegoruAPI megoruAPI = new MegoruAPIImpl("this bot don`t use token");
            GameWordLanguage gameWordLanguage = new GameWordLanguage();
            gameWordLanguage.setLanguage(BotStartConfig.getMapGameLanguages().get(userId));

            try {
                WORD = megoruAPI.getWord(gameWordLanguage).getWord();
                if (WORD != null) {
                    wordToChar = WORD.toCharArray(); // Преобразуем строку str в массив символов (char)
                    hideWord(WORD.length());
                }
            } catch (Exception e) {
                EmbedBuilder wordIsNull = new EmbedBuilder();
                wordIsNull.setTitle(jsonParsers.getLocale("errors_title", userId));
                wordIsNull.setAuthor(userName, null, avatarUrl);
                wordIsNull.setColor(Color.RED);
                wordIsNull.setDescription(jsonParsers.getLocale("errors", userId));

                textChannel.sendMessageEmbeds(wordIsNull.build()).queue();
                clearingCollections();
                return;
            }

            textChannel.sendMessageEmbeds(updateEmbedBuilder().build()).queue(this::createEntityInDataBase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO: Возможно произойдет так что игру закончили. Удалили данные из БД и произойдет REPLACE и игра не завершится
    private void executeInsert() {
        try {
            if ((guesses.size() > countUsedLetters) && HangmanRegistry.getInstance().hasHangman(Long.parseLong(userId))) {
                countUsedLetters = guesses.size();
                hangmanGameRepository.updateGame(Long.valueOf(userId), currentHiddenWord, getGuesses(), hangmanErrors);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Автоматически отправляет в БД данные
    public void autoInsert() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() throws NullPointerException {
                try {
                    if (HangmanRegistry.getInstance().hasHangman(Long.parseLong(userId))) {
                        executeInsert();
                        deleteMessages();
                    } else {
                        deleteMessages();
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }, 7000, 5000);
    }

    private void deleteMessages() throws InterruptedException {
        try {
            if (BotStartConfig.jda
                    .getGuildById(guildId)
                    .getSelfMember()
                    .hasPermission(BotStartConfig.jda.getTextChannelById(channelId), Permission.MESSAGE_MANAGE) && !messageList.isEmpty()) {
                if (messageList.size() > 2) {
                    BotStartConfig.jda.getGuildById(guildId).getTextChannelById(channelId).deleteMessages(messageList).queue();
                    //Так как метод асинхронный иногда может возникать NPE
                    Thread.sleep(3500);
                    messageList.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fullWord(String inputs, Message messages) {
        try {
            messageList.add(messages);
            if (inputs.length() < WORD.length()) return;
            if (isLetterPresent(inputs.toUpperCase())) {
                EmbedBuilder info = embedBuilder(
                        Color.GREEN,
                        "<@" + Long.parseLong(userId) + ">",
                        jsonGameParsers.getLocale("Game_You_Use_This_Word", userId),
                        true,
                        false,
                        inputs
                );
                HangmanHelper.editMessage(info, Long.parseLong(userId));
                return;
            }

            if (inputs.equals(WORD)) {
                EmbedBuilder win = embedBuilder(
                        Color.GREEN,
                        "<@" + Long.parseLong(userId) + ">",
                        jsonGameParsers.getLocale("Game_Stop_Win", userId),
                        true,
                        false,
                        inputs
                );

                HangmanHelper.editMessageWithButtons(win, Long.parseLong(userId), EndGameButtons.getListButtons(userId));

                clearingCollections();
                resultGame(true);
            } else {
                hangmanErrors++;
                if (hangmanErrors >= 8) {
                    gameLose(inputs);
                } else {
                    EmbedBuilder wordNotFound = embedBuilder(
                            Color.GREEN,
                            "<@" + Long.parseLong(userId) + ">",
                            jsonGameParsers.getLocale("Game_No_Such_Word", userId),
                            true,
                            false,
                            inputs
                    );

                    HangmanHelper.editMessage(wordNotFound, Long.parseLong(userId));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void gameLose(String inputs) {
        try {
            EmbedBuilder info = embedBuilder(
                    Color.GREEN,
                    "<@" + Long.parseLong(userId) + ">",
                    jsonGameParsers.getLocale("Game_You_Lose", userId),
                    true,
                    true,
                    inputs
            );

            HangmanHelper.editMessageWithButtons(info, Long.parseLong(userId), EndGameButtons.getListButtons(userId));

            clearingCollections();
            resultGame(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logic(String inputs, Message messages) {
        try {
            messageList.add(messages);
            if (WORD == null) {
                messages.getTextChannel()
                        .sendMessage(jsonParsers.getLocale("word_is_null", userId))
                        .setActionRow(EndGameButtons.getListButtons(userId))
                        .queue();
                clearingCollections();
                return;
            }
        } catch (Exception e) {
            System.out.println("Word null");
        }
        try {
            if (WORD_HIDDEN.contains("_")) {
                if (isLetterPresent(inputs.toUpperCase())) {

                    EmbedBuilder info = embedBuilder(
                            Color.GREEN,
                            "<@" + Long.parseLong(userId) + ">",
                            jsonGameParsers.getLocale("Game_You_Use_This_Letter", userId),
                            true,
                            false,
                            inputs
                    );

                    HangmanHelper.editMessage(info, Long.parseLong(userId));
                    return;
                }

                if (WORD.contains(inputs)) {
                    char c = inputs.charAt(0);
                    checkLetterInWord(wordToChar, c);
                    String result = replacementLetters(WORD.indexOf(inputs));

                    //Игрок угадал все буквы
                    if (!result.contains("_")) {
                        EmbedBuilder win = embedBuilder(
                                Color.GREEN,
                                "<@" + Long.parseLong(userId) + ">",
                                jsonGameParsers.getLocale("Game_Stop_Win", userId),
                                true,
                                false,
                                result
                        );

                        HangmanHelper.editMessageWithButtons(win, Long.parseLong(userId), EndGameButtons.getListButtons(userId));

                        clearingCollections();
                        resultGame(true);
                        return;
                    }

                    //Вы угадали букву!
                    EmbedBuilder info = embedBuilder(
                            Color.GREEN,
                            "<@" + Long.parseLong(userId) + ">",
                            jsonGameParsers.getLocale("Game_You_Guess_Letter", userId),
                            true,
                            false,
                            inputs
                    );

                    HangmanHelper.editMessage(info, Long.parseLong(userId));
                } else {
                    hangmanErrors++;
                    if (hangmanErrors >= 8) {
                        gameLose(inputs);
                    } else {
                        EmbedBuilder wordNotFound = embedBuilder(
                                Color.GREEN,
                                "<@" + Long.parseLong(userId) + ">",
                                jsonGameParsers.getLocale("Game_No_Such_Letter", userId),
                                true,
                                false,
                                inputs
                        );

                        HangmanHelper.editMessage(wordNotFound, Long.parseLong(userId));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private EmbedBuilder embedBuilder(Color color, String gamePlayer, String gameInfo, boolean gameGuesses, boolean isDefeat, @Nullable String inputs) {
        EmbedBuilder embedBuilder = null;
        try {
            String language = BotStartConfig.getMapGameLanguages().get(userId).equals("rus") ? "Кириллица" : "Latin";
            embedBuilder = new EmbedBuilder();

            LOGGER.info("\ngamePlayer: " + gamePlayer
                    + "\ngameInfo: " + gameInfo
                    + "\ngameGuesses: " + gameGuesses
                    + "\nisDefeat: " + isDefeat
                    + "\ninputs: " + inputs
                    + "\nlanguage " + language);

            embedBuilder.setColor(color);
            embedBuilder.addField(jsonGameParsers.getLocale("Game_Player", userId), gamePlayer, true);
            embedBuilder.addField(jsonGameParsers.getLocale("Game_Language", userId), language, true);
            embedBuilder.setThumbnail(GetImage.get(hangmanErrors));

            if (gameGuesses) {
                embedBuilder.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + getGuesses() + "`", false);
            }

            if (inputs != null && inputs.length() == 1) {
                embedBuilder.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);
            }

            if (inputs == null) {
                embedBuilder.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + WORD_HIDDEN.toUpperCase() + "`", false);
            } else if (inputs.length() >= 3) {
                if (inputs.equals(WORD)) {
                    embedBuilder.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + WORD.toUpperCase().replaceAll("", " ").trim() + "`", false);
                } else {
                    embedBuilder.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + WORD_HIDDEN.toUpperCase() + "`", false);
                }
            }

            if (isDefeat) {
                embedBuilder.addField(jsonGameParsers.getLocale("Game_Word_That_Was", userId), "`" + WORD.toUpperCase().replaceAll("", " ").trim() + "`", false);
            }

            embedBuilder.addField(jsonGameParsers.getLocale("Game_Info", userId), gameInfo, false);

        } catch (Exception e) {
            e.printStackTrace();
        }
        //embedBuilder.setTimestamp(OffsetDateTime.parse(String.valueOf(HangmanRegistry.getInstance().getEndAutoDelete().get(Long.parseLong(userId)))));
        //embedBuilder.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));
        return embedBuilder;
    }

    private void resultGame(boolean resultBool) {
        try {
            final int idGame = HangmanRegistry.getInstance().getIdGame();

            Game game = new Game();
            game.setId(idGame);
            game.setResult(resultBool);
            game.setGameDate(new Timestamp(Instant.now().toEpochMilli()));

            Player player = new Player();
            player.setId(idGame);
            player.setUserIdLong(Long.valueOf(userId));
            player.setGames_id(game);

            gamesRepository.saveAndFlush(game);
            playerRepository.saveAndFlush(player);

            hangmanGameRepository.deleteActiveGame(Long.valueOf(userId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createEntityInDataBase(Message message) {
        try {
            HangmanRegistry.getInstance().getMessageId().put(Long.parseLong(userId), message.getId());

            ActiveHangman activeHangman = new ActiveHangman();
            activeHangman.setUserIdLong(Long.valueOf(userId));
            activeHangman.setMessageIdLong(Long.valueOf(message.getId()));
            activeHangman.setChannelIdLong(channelId);
            activeHangman.setGuildLongId(guildId != null ? Long.valueOf(guildId) : null);
            activeHangman.setWord(WORD);
            activeHangman.setCurrentHiddenWord(WORD_HIDDEN);
            activeHangman.setGuesses(getGuesses());
            activeHangman.setHangmanErrors(hangmanErrors);
            activeHangman.setGameCreatedTime(new Timestamp(Instant.now().toEpochMilli()));
            hangmanGameRepository.saveAndFlush(activeHangman);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopGameByTime() {
        try {
            EmbedBuilder info = new EmbedBuilder();
            info.setColor(0x00FF00);
            info.setTitle(jsonGameParsers.getLocale("gameOver", userId));
            info.setDescription(jsonGameParsers.getLocale("timeIsOver", userId));
            info.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);

            HangmanHelper.editMessageWithButtons(info, Long.parseLong(userId), EndGameButtons.getListButtons(userId));

            clearingCollections();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //Создает скрытую линию из длины слова
    private void hideWord(int length) {
        try {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < length) {
                sb.append(sb.length() == 0 ? "_" : " _");
                i++;
            }
            WORD_HIDDEN = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //заменяет "_" на букву которая есть в слове
    private String replacementLetters(int length) {
        try {
            if (currentHiddenWord == null) {
                currentHiddenWord = WORD_HIDDEN;
            }
            StringBuilder sb = new StringBuilder(currentHiddenWord);

            for (int i = 0; i < index.size(); i++) {
                sb.replace(index.get(i) == 0 ? index.get(i) : index.get(i) * 2,
                        index.get(i) == 0 ? index.get(i) + 1 : index.get(i) * 2 + 1,
                        String.valueOf(wordToChar[length]));
            }
            index.clear();
            currentHiddenWord = sb.toString();
            WORD_HIDDEN = currentHiddenWord;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currentHiddenWord;
    }


    //Ищет все одинаковые буквы и записывает в коллекцию
    private void checkLetterInWord(char[] checkArray, char letter) {
        for (int i = 0; i < checkArray.length; i++) {
            if (checkArray[i] == letter) {
                index.add(i);
            }
        }
    }

    //Для инъекции при восстановлении
    public void updateVariables(String guesses, String word, String currentHiddenWord, int hangmanErrors) {
        this.guesses.addAll(Arrays.asList(guesses.split(", ")));
        this.WORD = word;
        this.WORD_HIDDEN = currentHiddenWord;
        this.currentHiddenWord = currentHiddenWord;
        this.hangmanErrors = hangmanErrors;
        this.wordToChar = word.toCharArray();
    }

    private void clearingCollections() {
        try {
            guesses.clear();
            currentHiddenWord = null;
            WORD = null;
            HangmanRegistry.getInstance().removeHangman(Long.parseLong(userId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isLetterPresent(String inputs) {
        boolean contains = guesses.contains(inputs.toUpperCase());
        if (!contains) {
            addGuesses(inputs.toUpperCase());
        }
        return contains;
    }

    private void addGuesses(String letter) {
        guesses.add(letter);
    }

    public String getGuesses() {
        return guesses
                .toString()
                .replaceAll("\\[", "")
                .replaceAll("]", "");
    }
}