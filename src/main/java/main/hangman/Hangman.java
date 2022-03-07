package main.hangman;

import lombok.Getter;
import lombok.Setter;
import main.config.BotStartConfig;
import main.hangman.impl.*;
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

import java.awt.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Queue;
import java.util.*;

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

    private final Queue<Message> messageList = new ArrayDeque<>();
    private int countUsedLetters;
    private String WORD = null;
    private char[] wordToChar;
    private String WORD_HIDDEN = "";
    private String currentHiddenWord;

    private int hangmanErrors = 0;

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
    private void updateEmbedBuilder(EmbedBuilder start) {
        autoInsert();

        Instant instant = Instant.now().plusSeconds(600L);

        HangmanRegistry.getInstance().getTimeCreatedGame().put(Long.valueOf(userId), LocalDateTime.from(OffsetDateTime.parse(String.valueOf(Instant.now()))));

        HangmanRegistry.getInstance().getEndAutoDelete().put(Long.valueOf(userId), String.valueOf(OffsetDateTime.parse(String.valueOf(instant))));

        start.setColor(0x00FF00);
        start.setTitle(jsonGameParsers.getLocale("Game_Title", userId));

        start.addField(jsonGameParsers.getLocale("Game_Start_How_Play", userId), jsonGameParsers.getLocale("Game_Start", userId).replaceAll("\\{0}",
                BotStartConfig.getMapPrefix().get(guildId) == null ? "!" : BotStartConfig.getMapPrefix().get(guildId)), false);

        start.setThumbnail(GetImage.get(hangmanErrors));
        start.addField("Attention for Admins", "[Add slash commands](https://discord.com/api/oauth2/authorize?client_id=845974873682608129&scope=applications.commands%20bot)", false);
        start.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
        start.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "", false);
        start.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + WORD_HIDDEN.toUpperCase() + "`", false);
        start.setTimestamp(instant);
        start.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));
    }

    public void startGame(@NotNull SlashCommandInteractionEvent event) {
        try {
            if (BotStartConfig.getMapGameLanguages().get(getUserId()) == null) {
                EmbedBuilder needSetLanguage = new EmbedBuilder();

                needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                needSetLanguage.setColor(0x00FF00);
                needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getId()));

                event.replyEmbeds(needSetLanguage.build()).addActionRow(SetGameLanguageButtons.getList).queue();
                clearingCollections();
                return;
            }

            WORD = GetWord.get(userId);
            if (WORD != null) {
                wordToChar = WORD.toCharArray(); // Преобразуем строку str в массив символов (char)
                hideWord(WORD.length());
            } else {
                EmbedBuilder wordIsNull = new EmbedBuilder();
                wordIsNull.setTitle(jsonParsers.getLocale("errors_title", event.getUser().getId()));
                wordIsNull.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                wordIsNull.setColor(Color.RED);
                wordIsNull.setDescription(jsonParsers.getLocale("errors", event.getUser().getId()));

                event.replyEmbeds(wordIsNull.build()).queue();
                clearingCollections();
                return;
            }

            EmbedBuilder start = new EmbedBuilder();

            //Заполняем EmbedBuilder start
            updateEmbedBuilder(start);

            event.replyEmbeds(start.build()).queue(m -> m.retrieveOriginal().queue(this::createEntityInDataBase));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createEntityInDataBase(Message message) {
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
        hangmanGameRepository.save(activeHangman);
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

            WORD = GetWord.get(userId);
            if (WORD != null) {
                wordToChar = WORD.toCharArray(); // Преобразуем строку str в массив символов (char)
                hideWord(WORD.length());
            } else {
                EmbedBuilder wordIsNull = new EmbedBuilder();

                wordIsNull.setTitle(jsonParsers.getLocale("errors_title", userId));
                wordIsNull.setAuthor(userName, null, avatarUrl);
                wordIsNull.setColor(Color.RED);
                wordIsNull.setDescription(jsonParsers.getLocale("errors", userId));

                textChannel.sendMessageEmbeds(wordIsNull.build()).queue();
                clearingCollections();
                return;
            }

            EmbedBuilder start = new EmbedBuilder();

            //Заполняем EmbedBuilder start
            updateEmbedBuilder(start);

            textChannel.sendMessageEmbeds(start.build()).queue(this::createEntityInDataBase);
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
            System.out.println("По каким то причинам игры уже нет!");
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
                    deleteMessages();
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }, 7000, 5000);
    }

    private void deleteMessages() {
        if (guildId != null) {
            if (BotStartConfig.jda
                    .getGuildById(guildId)
                    .getSelfMember()
                    .hasPermission(BotStartConfig.jda.getTextChannelById(channelId), Permission.MESSAGE_MANAGE) && !messageList.isEmpty()) {
                if (messageList.size() == 1) {
                    BotStartConfig.jda.getGuildById(guildId).getTextChannelById(channelId).deleteMessageById(messageList.poll().getId()).queue();
                } else {
                    List<Message> temp = new ArrayList<>(messageList);
                    BotStartConfig.jda.getGuildById(guildId).getTextChannelById(channelId).deleteMessages(messageList).queue();
                    messageList.removeAll(temp);
                }
            }
        }
    }

    public void fullWord(String inputs, Message messages) {
        messageList.add(messages);
        try {

            if (isLetterPresent(inputs.toUpperCase())) {
                    EmbedBuilder info = new EmbedBuilder();
                    info.setColor(0x00FF00);
                    info.setTitle(jsonGameParsers.getLocale("Game_Title", userId));
                    info.appendDescription(jsonGameParsers.getLocale("Game_You_Use_This_Word", userId));

                    info.setThumbnail(GetImage.get(hangmanErrors));
                    info.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                    info.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + getGuesses() + "`", false);
                    info.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);

                    info.setTimestamp(OffsetDateTime.parse(String.valueOf(HangmanRegistry.getInstance().getEndAutoDelete().get(Long.parseLong(userId)))));
                    info.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));

                    HangmanHelper.editMessage(info, Long.parseLong(userId));
                return;
            }

            if (inputs.equals(WORD)) {
                EmbedBuilder win = new EmbedBuilder();
                extractedGameWin(win);
                win.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + WORD.toUpperCase() + "`", false);

                HangmanHelper.editMessageWithButtons(win, Long.parseLong(userId), EndGameButtons.getListButtons(userId));

                clearingCollections();
                resultGame(true);
            } else {
                hangmanErrors++;
                if (hangmanErrors >= 8) {
                    extractedGameLose(inputs);
                } else {
                    EmbedBuilder wordNotFound = new EmbedBuilder();
                    wordNotFound.setColor(0x00FF00);
                    wordNotFound.setTitle(jsonGameParsers.getLocale("Game_Title", userId));
                    wordNotFound.setDescription(jsonGameParsers.getLocale("Game_No_Such_Word", userId));

                    wordNotFound.setThumbnail(GetImage.get(hangmanErrors));
                    wordNotFound.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                    wordNotFound.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + getGuesses() + "`", false);
                    wordNotFound.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);

                    wordNotFound.setTimestamp(OffsetDateTime.parse(String.valueOf(HangmanRegistry.getInstance().getEndAutoDelete().get(Long.parseLong(userId)))));
                    wordNotFound.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));

                    HangmanHelper.editMessage(wordNotFound, Long.parseLong(userId));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void extractedGameWin(EmbedBuilder win) {
        win.setColor(0x00FF00);
        win.setTitle(jsonGameParsers.getLocale("Game_Title", userId));
        win.setDescription(jsonGameParsers.getLocale("Game_Stop_Win", userId));
        win.setThumbnail(GetImage.get(hangmanErrors));
        win.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
        win.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + getGuesses() + "`", false);
    }

    private void extractedGameLose(String inputs) {
        EmbedBuilder info = new EmbedBuilder();
        info.setColor(0x00FF00);
        info.setTitle(jsonGameParsers.getLocale("Game_Title", userId));
        info.setDescription(jsonGameParsers.getLocale("Game_You_Lose", userId));

        info.setThumbnail(GetImage.get(hangmanErrors));
        info.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
        info.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + getGuesses() + "`", false);
        info.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);
        info.addField(jsonGameParsers.getLocale("Game_Word_That_Was", userId), "`" + WORD.toUpperCase() + "`", false);

        HangmanHelper.editMessageWithButtons(info, Long.parseLong(userId), EndGameButtons.getListButtons(userId));

        clearingCollections();
        resultGame(false);
    }

    public void logic(String inputs, Message messages) {
        messageList.add(messages);
        try {
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

        if (WORD_HIDDEN.contains("_")) {

            if (isLetterPresent(inputs.toUpperCase())) {
                try {
                    EmbedBuilder info = new EmbedBuilder();
                    info.setColor(0x00FF00);
                    info.setTitle(jsonGameParsers.getLocale("Game_Title", userId));
                    info.appendDescription(jsonGameParsers.getLocale("Game_You_Use_This_Letter", userId));

                    info.setThumbnail(GetImage.get(hangmanErrors));
                    info.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                    info.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + getGuesses() + "`", false);
                    info.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);

                    info.setTimestamp(OffsetDateTime.parse(String.valueOf(HangmanRegistry.getInstance().getEndAutoDelete().get(Long.parseLong(userId)))));
                    info.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));

                    HangmanHelper.editMessage(info, Long.parseLong(userId));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            if (WORD.contains(inputs)) {
                char c = inputs.charAt(0);
                checkLetterInWord(wordToChar, c);
                String result = replacementLetters(WORD.indexOf(inputs));

                if (!result.contains("_")) {
                    try {
                        EmbedBuilder win = new EmbedBuilder();
                        extractedGameWin(win);
                        win.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + result.toUpperCase() + "`", false);

                        HangmanHelper.editMessageWithButtons(win, Long.parseLong(userId), EndGameButtons.getListButtons(userId));

                        clearingCollections();
                        resultGame(true);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    EmbedBuilder info = new EmbedBuilder();
                    info.setColor(0x00FF00);
                    info.setTitle(jsonGameParsers.getLocale("Game_Title", userId));
                    info.setDescription(jsonGameParsers.getLocale("Game_You_Guess_Letter", userId));

                    info.setThumbnail(GetImage.get(hangmanErrors));
                    info.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                    info.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + getGuesses() + "`", false);
                    info.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + result.toUpperCase() + "`", false);

                    info.setTimestamp(OffsetDateTime.parse(String.valueOf(HangmanRegistry.getInstance().getEndAutoDelete().get(Long.parseLong(userId)))));
                    info.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));

                    HangmanHelper.editMessage(info, Long.parseLong(userId));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                hangmanErrors++;
                if (hangmanErrors >= 8) {
                    try {
                        extractedGameLose(inputs);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        EmbedBuilder wordNotFound = new EmbedBuilder();
                        wordNotFound.setColor(0x00FF00);
                        wordNotFound.setTitle(jsonGameParsers.getLocale("Game_Title", userId));
                        wordNotFound.setDescription(jsonGameParsers.getLocale("Game_No_Such_Letter", userId));

                        wordNotFound.setThumbnail(GetImage.get(hangmanErrors));
                        wordNotFound.addField(jsonGameParsers.getLocale("Game_Player", userId), "<@" + Long.parseLong(userId) + ">", false);
                        wordNotFound.addField(jsonGameParsers.getLocale("Game_Guesses", userId), "`" + getGuesses() + "`", false);
                        wordNotFound.addField(jsonGameParsers.getLocale("Game_Current_Word", userId), "`" + replacementLetters(WORD.indexOf(inputs)).toUpperCase() + "`", false);

                        wordNotFound.setTimestamp(OffsetDateTime.parse(String.valueOf(HangmanRegistry.getInstance().getEndAutoDelete().get(Long.parseLong(userId)))));
                        wordNotFound.setFooter(jsonGameParsers.getLocale("gameOverTime", userId));

                        HangmanHelper.editMessage(wordNotFound, Long.parseLong(userId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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

            gamesRepository.save(game);
            playerRepository.save(player);

            hangmanGameRepository.deleteActiveGame(Long.valueOf(userId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Создает скрытую линию из длины слова
    private void hideWord(int length) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < length) {
            sb.append(sb.length() == 0 ? "_" : " _");
            i++;
        }
        WORD_HIDDEN = sb.toString();
    }

    //заменяет "_" на букву которая есть в слове
    private String replacementLetters(int length) {
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