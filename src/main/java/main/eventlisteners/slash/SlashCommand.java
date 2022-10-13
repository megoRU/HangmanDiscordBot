package main.eventlisteners.slash;

import lombok.RequiredArgsConstructor;
import main.config.BotStartConfig;
import main.enums.Statistic;
import main.eventlisteners.ChecksClass;
import main.eventlisteners.DeleteAllMyData;
import main.eventlisteners.buildClass.Help;
import main.eventlisteners.buildClass.MessageStats;
import main.hangman.Hangman;
import main.hangman.HangmanBuilder;
import main.hangman.HangmanRegistry;
import main.hangman.impl.ButtonIMpl;
import main.hangman.impl.HangmanHelper;
import main.jsonparser.JSONParsers;
import main.model.entity.Category;
import main.model.entity.GameLanguage;
import main.model.entity.Language;
import main.model.repository.*;
import main.statistic.CreatorGraph;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.logging.Logger;

@RequiredArgsConstructor
@Service
public class SlashCommand extends ListenerAdapter {

    //Language
    private final JSONParsers jsonParsers = new JSONParsers(JSONParsers.Locale.BOT);
    private static final JSONParsers jsonGameParsers = new JSONParsers(JSONParsers.Locale.GAME);

    private static final String HG_ONE_WORD = "[-A-Za-zА-ЯЁа-яё\s]{3,24}+";
    //REPO
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;
    private final GameLanguageRepository gameLanguageRepository;
    private final LanguageRepository languageRepository;
    private final CategoryRepository categoryRepository;
    private final static Logger LOGGER = Logger.getLogger(SlashCommand.class.getName());

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        try {
            if (event.getUser().isBot()) return;

            boolean permission = ChecksClass.canSendHG(event.getChannel(), event);
            if (!permission) return;

            long userIdLong = event.getUser().getIdLong();

            LOGGER.info("\nSlash Command name: " + event.getName());

            if (event.getName().equals("hg")) {
                event.getChannel().sendTyping().queue();
                //Проверяем установлен ли язык. Если нет - то возвращаем в чат ошибку
                if (!BotStartConfig.getMapGameLanguages().containsKey(userIdLong)) {

                    String hangmanListenerNeedSetLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userIdLong);

                    EmbedBuilder needSetLanguage = new EmbedBuilder();

                    needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    needSetLanguage.setColor(0x00FF00);
                    needSetLanguage.setDescription(hangmanListenerNeedSetLanguage);

                    event.replyEmbeds(needSetLanguage.build())
                            .addActionRow(ButtonIMpl.BUTTON_RUSSIAN, ButtonIMpl.BUTTON_ENGLISH)
                            .addActionRow(ButtonIMpl.BUTTON_PLAY_AGAIN)
                            .queue();
                    return;
                    //Проверяем если игрок уже играет. То присылаем в чат уведомление
                } else if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    String hangmanListenerYouPlay = jsonParsers.getLocale("Hangman_Listener_You_Play", userIdLong);

                    EmbedBuilder youPlay = new EmbedBuilder();

                    youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    youPlay.setColor(0x00FF00);

                    youPlay.setDescription(hangmanListenerYouPlay);

                    event.replyEmbeds(youPlay.build())
                            .addActionRow(ButtonIMpl.BUTTON_STOP)
                            .queue();
                    //Если всё хорошо, создаем игру
                } else {
                    HangmanBuilder.Builder hangmanBuilder = new HangmanBuilder.Builder()
                            .setUserIdLong(userIdLong)
                            .setChannelId(event.getChannel().getIdLong())
                            .setHangmanGameRepository(hangmanGameRepository)
                            .setGamesRepository(gamesRepository)
                            .setPlayerRepository(playerRepository);

                    if (event.getGuild() != null) {
                        hangmanBuilder.setGuildIdLong(event.getGuild().getIdLong());

                    } else {
                        hangmanBuilder.setGuildIdLong(null);
                    }

                    String createGame = jsonParsers.getLocale("create_game", userIdLong);

                    event.reply(createGame).setEphemeral(true).queue();

                    Hangman hangman = hangmanBuilder.build();

                    HangmanRegistry.getInstance().setHangman(userIdLong, hangman);

                    hangman.startGame(event.getChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
                }
                return;
            }

            if (event.getName().equals("category")) {
                String categorySlash = event.getOption("set", OptionMapping::getAsString);
                String gameCategory = jsonParsers.getLocale("game_category", userIdLong);

                if (categorySlash != null && categorySlash.equals("any")) {
                    BotStartConfig.mapGameCategory.remove(userIdLong);
                    categoryRepository.deleteCategory(userIdLong);
                    event.reply(gameCategory).setEphemeral(true).queue();
                    return;
                }
                Category category = new Category();
                category.setUserIdLong(userIdLong);
                category.setCategory(categorySlash);
                categoryRepository.save(category);
                BotStartConfig.mapGameCategory.put(userIdLong, categorySlash);
                event.reply(gameCategory).setEphemeral(true).queue();
                return;
            }

            if (event.getName().equals("multi")) {
                boolean canSendHG = ChecksClass.canSendHG(event.getChannel(), event);
                if (!canSendHG) return;
                User user = event.getOption("user", OptionMapping::getAsUser);

                if (user == null || event.getGuild() == null) {
                    event.reply("User is `null`").setEphemeral(true).queue();
                    return;
                } else if (user.isBot()) {
                    String playWithBot = jsonParsers.getLocale("play_with_bot", userIdLong);
                    event.reply(playWithBot).setEphemeral(true).queue();
                    return;
                } else if (user.getIdLong() == userIdLong) {
                    String playWithYourself = jsonParsers.getLocale("play_with_yourself", userIdLong);
                    event.reply(playWithYourself).setEphemeral(true).queue();
                    return;
                }

                if (!BotStartConfig.getMapGameLanguages().containsKey(userIdLong)) {

                    String hangmanListenerNeedSetLanguage = jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", userIdLong);

                    EmbedBuilder needSetLanguage = new EmbedBuilder();

                    needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    needSetLanguage.setColor(0x00FF00);
                    needSetLanguage.setDescription(hangmanListenerNeedSetLanguage);

                    event.replyEmbeds(needSetLanguage.build())
                            .addActionRow(ButtonIMpl.BUTTON_RUSSIAN, ButtonIMpl.BUTTON_ENGLISH)
                            .addActionRow(ButtonIMpl.getButtonPlayAgainWithUsers(userIdLong, user.getIdLong()))
                            .queue();
                    return;
                    //Проверяем если игрок уже играет. То присылаем в чат уведомление
                } else if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    String hangmanListenerYouPlay = jsonParsers.getLocale("Hangman_Listener_You_Play", userIdLong);

                    EmbedBuilder youPlay = new EmbedBuilder();

                    youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    youPlay.setColor(0x00FF00);

                    youPlay.setDescription(hangmanListenerYouPlay);

                    event.replyEmbeds(youPlay.build())
                            .addActionRow(ButtonIMpl.BUTTON_STOP)
                            .queue();
                    return;
                } else {
                    Hangman hangman = new HangmanBuilder.Builder()
                            .setUserIdLong(userIdLong)
                            .setSecondUserIdLong(user.getIdLong())
                            .setGuildIdLong(event.getGuild().getIdLong())
                            .setChannelId(event.getChannel().getIdLong())
                            .setHangmanGameRepository(hangmanGameRepository)
                            .setGamesRepository(gamesRepository)
                            .setPlayerRepository(playerRepository)
                            .build();

                    HangmanRegistry.getInstance().setHangman(userIdLong, hangman);
                    HangmanRegistry.getInstance().setHangman(user.getIdLong(), hangman);

                    String createGame = jsonParsers.getLocale("create_game", userIdLong);

                    event.reply(createGame).setEphemeral(true).queue();
                    hangman.startGame(event.getChannel(), event.getUser().getAvatarUrl(), event.getUser().getName());
                }
                return;
            }

            if (event.getName().equals("stop")) {
                //Проверяем играет ли сейчас игрок. Если да удаляем игру.
                if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    Hangman activeHangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);
                    long userId = activeHangman.getUserId(); //NPE? по идеи не должно
                    long secondPlayer = activeHangman.getSecondPlayer(); //NPE? по идеи не должно

                    String hangmanEngGame = jsonParsers.getLocale("Hangman_Eng_game", userId);
                    String hangmanEngGame1 = jsonGameParsers.getLocale("Hangman_Eng_game", userId);

                    if (secondPlayer != 0L) {
                        event.reply(hangmanEngGame)
                                .addActionRow(ButtonIMpl.getButtonPlayAgainWithUsers(userId, secondPlayer))
                                .queue();
                    } else {
                        event.reply(hangmanEngGame)
                                .addActionRow(ButtonIMpl.BUTTON_PLAY_AGAIN)
                                .queue();
                    }

                    var embedBuilder = activeHangman
                            .embedBuilder(Color.GREEN,
                                    hangmanEngGame1,
                                    false,
                                    false,
                                    null
                            );

                    HangmanHelper.editMessage(embedBuilder, userId);
                    HangmanRegistry.getInstance().removeHangman(userId);
                    hangmanGameRepository.deleteActiveGame(userId);
                    //Если игрок не играет, а хочет завершить игру, то нужно ему это прислать уведомление, что он сейчас не играет
                } else {
                    String hangmanYouAreNotPlay = jsonParsers.getLocale("Hangman_You_Are_Not_Play", userIdLong);
                    event.reply(hangmanYouAreNotPlay).addActionRow(ButtonIMpl.BUTTON_PLAY_AGAIN).queue();
                }
                return;
            }

            //Если игрок сейчас играет сменить язык не даст
            if (event.getName().equals("language") && HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                String reactionsButtonWhenPlay = jsonParsers.getLocale("ReactionsButton_When_Play", userIdLong);

                EmbedBuilder whenPlay = new EmbedBuilder();

                whenPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                whenPlay.setColor(Color.GREEN);
                whenPlay.setDescription(reactionsButtonWhenPlay);

                event.replyEmbeds(whenPlay.build()).addActionRow(ButtonIMpl.BUTTON_STOP).queue();
                return;
            }

            //0 - game | 1 - bot
            if (event.getName().equals("language") && event.getOptions().size() == 2) {
                String opOne = event.getOptions().get(0).getAsString();
                String opTwo = event.getOptions().get(1).getAsString();

                BotStartConfig.getMapGameLanguages().put(userIdLong, opOne);
                BotStartConfig.getMapLanguages().put(userIdLong, opTwo);

                String slashLanguage = String.format(jsonParsers.getLocale("slash_language", userIdLong), opOne, opTwo);

                event.reply(slashLanguage).addActionRow(ButtonIMpl.BUTTON_PLAY_AGAIN).queue();

                GameLanguage gameLanguage = new GameLanguage();
                gameLanguage.setUserIdLong(userIdLong);
                gameLanguage.setLanguage(opOne);
                gameLanguageRepository.save(gameLanguage);

                Language language = new Language();
                language.setUserIdLong(userIdLong);
                language.setLanguage(opTwo);
                languageRepository.save(language);
                return;
            }

            if (event.getName().equals("full")) {
                if (HangmanRegistry.getInstance().hasHangman(userIdLong)) {
                    String gotYourWord = jsonParsers.getLocale("got_your_word", userIdLong);

                    String word = event.getOption("word", OptionMapping::getAsString);
                    int length = 0;

                    if (word != null) {
                        word.toLowerCase();
                        length = word.length();
                    }

                    event.reply(gotYourWord).setEphemeral(true).queue();

                    Hangman hangman = HangmanRegistry.getInstance().getActiveHangman(userIdLong);

                    if (word != null && length == hangman.getLengthWord() && word.matches(HG_ONE_WORD)) {
                        hangman.fullWord(word);
                    } else {
                        String wrongLengthJson = jsonGameParsers.getLocale("wrongLength", userIdLong);
                        EmbedBuilder wrongLength = hangman.embedBuilder(
                                Color.GREEN,
                                wrongLengthJson,
                                false,
                                false,
                                word);

                        HangmanHelper.editMessage(wrongLength, userIdLong);
                        return;
                    }
                } else {
                    String hangmanYouAreNotPlay = jsonParsers.getLocale("Hangman_You_Are_Not_Play", userIdLong);
                    event.reply(hangmanYouAreNotPlay).addActionRow(ButtonIMpl.BUTTON_PLAY_AGAIN).queue();
                }
                return;
            }

            if (event.getName().equals("delete")) {
                DeleteAllMyData deleteAllMyData = new DeleteAllMyData(gamesRepository, languageRepository, gameLanguageRepository);
                deleteAllMyData.buildMessage(event, event.getUser());
                return;
            }

            if (event.getName().equals("help")) {
                Help help = new Help();
                help.send(
                        null,
                        event,
                        event.getUser().getAvatarUrl(),
                        userIdLong,
                        event.getUser().getName());
                return;
            }

            if (event.getName().equals("stats")) {
                event.deferReply().queue();

                MessageStats messageStats = new MessageStats(
                        gamesRepository, event.getHook(),
                        event.getUser().getAvatarUrl(),
                        userIdLong,
                        event.getUser().getName());

                messageStats.sendStats();
                return;
            }

            if (event.getName().equals("allstats") || event.getName().equals("mystats")) {
                event.deferReply().queue();

                CreatorGraph creatorGraph = new CreatorGraph(
                        gamesRepository,
                        event.getUser().getId(),
                        event.getHook());

                switch (event.getName()) {
                    case "allstats" -> creatorGraph.createGraph(Statistic.GLOBAL);
                    case "mystats" -> creatorGraph.createGraph(Statistic.MY);
                }
            }

        } catch (Exception e) {
            System.out.println("Unknown interaction");
            e.printStackTrace();
        }
    }
}