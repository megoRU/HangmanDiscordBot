package main.hangman;

import lombok.RequiredArgsConstructor;
import main.config.BotStartConfig;
import main.eventlisteners.CheckPermissions;
import main.jsonparser.JSONParsers;
import main.model.entity.GameLanguage;
import main.model.entity.Language;
import main.model.repository.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SlashCommand extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();
    private final HangmanGameRepository hangmanGameRepository;
    private final GamesRepository gamesRepository;
    private final PlayerRepository playerRepository;
    private final GameLanguageRepository gameLanguageRepository;
    private final LanguageRepository languageRepository;

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (event.getUser().isBot()) return;
        if (event.getGuild() == null) return;

        if (CheckPermissions.isHasPermissionsWriteAndEmbedLinks(event.getTextChannel())) {
            return;
        }

        try {
            if (event.getName().equals("hg-start")) {
                event.getChannel().sendTyping().queue();
                //Проверяем установлен ли язык. Если нет - то возвращаем в чат ошибку
                if (BotStartConfig.getMapGameLanguages().get(event.getUser().getId()) == null) {
                    EmbedBuilder needSetLanguage = new EmbedBuilder();

                    needSetLanguage.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    needSetLanguage.setColor(0x00FF00);
                    needSetLanguage.setDescription(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getId()));

                    event.replyEmbeds(needSetLanguage.build())
                            .addActionRow(
                                    Button.secondary(ReactionsButton.BUTTON_RUS, "Кириллица")
                                            .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")),

                                    Button.secondary(ReactionsButton.BUTTON_ENG, "Latin")
                                            .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")),
                                    Button.success(ReactionsButton.BUTTON_START_NEW_GAME, "Play"))
                            .queue();
                    //Проверяем если игрок уже играет. То присылаем в чат уведомление
                } else if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {

                    EmbedBuilder youPlay = new EmbedBuilder();

                    youPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                    youPlay.setColor(0x00FF00);
                    youPlay.setDescription(jsonParsers.getLocale("Hangman_Listener_You_Play",
                            event.getUser().getId()).replaceAll("\\{0}", BotStartConfig.getMapPrefix().get(event.getGuild().getId()) == null
                            ? "!hg" : BotStartConfig.getMapPrefix().get(event.getGuild().getId())));

                    event.replyEmbeds(youPlay.build())
                            .addActionRow(Button.danger(ReactionsButton.BUTTON_STOP, "Stop game"))
                            .queue();
                    //Если всё хорошо, создаем игру
                } else {
                    HangmanRegistry.getInstance().setHangman(event.getUser().getIdLong(), new Hangman(event.getUser().getId(), event.getGuild().getId(), event.getChannel().getIdLong(), hangmanGameRepository, gamesRepository, playerRepository));
                    HangmanRegistry.getInstance().getActiveHangman().get(event.getUser().getIdLong()).startGame(event);
                }
                return;
            }

            if (event.getName().equals("hg-stop")) {
                //Проверяем играет ли сейчас игрок. Если да удаляем игру.
                if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
                    HangmanRegistry.getInstance().getActiveHangman().remove(event.getUser().getIdLong());

                    event.reply(jsonParsers.getLocale("Hangman_Eng_game",
                                    event.getUser().getId()).replaceAll("\\{0}", BotStartConfig.getMapPrefix().get(event.getGuild().getId()) == null ? "!hg" : BotStartConfig.getMapPrefix().get(event.getGuild().getId())))
                            .addActionRow(Button.success(ReactionsButton.BUTTON_START_NEW_GAME, "Play again"))
                            .queue();
                    hangmanGameRepository.deleteActiveGame(event.getUser().getIdLong());
                    //Если игрок не играет, а хочет завершить игру, то нужно ему это прислать уведомление, что он сейчас не играет
                } else {
                    event.reply(jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getUser().getId()))
                            .addActionRow(Button.success(ReactionsButton.BUTTON_START_NEW_GAME, "Play again"))
                            .queue();
                }
                return;
            }
            //Если игрок сейчас играет сменить язык не даст
            if (event.getName().equals("language") && HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
                EmbedBuilder whenPlay = new EmbedBuilder();

                whenPlay.setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl());
                whenPlay.setColor(0x00FF00);
                whenPlay.setDescription(jsonParsers.getLocale("ReactionsButton_When_Play", event.getUser().getId()));

                event.replyEmbeds(whenPlay.build())
                        .addActionRow(Button.success(ReactionsButton.BUTTON_START_NEW_GAME, "Play again"))
                        .queue();
                return;
            }

            //0 - game | 1 - bot
            if (event.getName().equals("language") && event.getOptions().size() == 2) {
                BotStartConfig.getMapGameLanguages().put(event.getUser().getId(), event.getOptions().get(0).getAsString());
                BotStartConfig.getMapLanguages().put(event.getUser().getId(), event.getOptions().get(1).getAsString());

                event.reply(jsonParsers.getLocale("slash_language", event.getUser().getId())
                                .replaceAll("\\{0}", event.getOptions().get(0).getAsString())
                                .replaceAll("\\{1}", event.getOptions().get(1).getAsString()))

                        .addActionRow(Button.success(ReactionsButton.BUTTON_START_NEW_GAME, "Play again"))
                        .queue();
                GameLanguage gameLanguage = new GameLanguage();
                gameLanguage.setUserIdLong(event.getUser().getId());
                gameLanguage.setLanguage(event.getOptions().get(0).getAsString());
                gameLanguageRepository.save(gameLanguage);

                Language language = new Language();
                language.setUserIdLong(event.getUser().getId());
                language.setLanguage(event.getOptions().get(1).getAsString());
                languageRepository.save(language);
            }
        } catch (Exception e) {
            System.out.println("Unknown interaction");
        }
    }
}