package hangman;

import db.DataBase;
import jsonparser.JSONParsers;
import messagesevents.GameLanguageChange;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class SlashCommand extends ListenerAdapter {
    private final JSONParsers jsonParsers = new JSONParsers();

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (event.getUser().isBot()) return;

        if (event.getGuild().getId() == null || event.getUser() == null) return;

        if (event.getName().equals("hg-start")) {

            if (BotStart.getMapGameLanguages().get(event.getUser().getId()) == null) {
                event.reply(jsonParsers.getLocale("Hangman_Listener_Need_Set_Language", event.getUser().getId()))
                        .addActionRow(
                                Button.secondary(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_RUS, "Кириллица")
                                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")),

                                Button.secondary(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_ENG, "Latin")
                                        .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")),
                                Button.success(event.getGuild().getId() + ":" + ReactionsButton.START_NEW_GAME, "Play"))
                        .queue();

            } else if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
                event.reply(jsonParsers.getLocale("Hangman_Listener_You_Play",
                        event.getUser().getId()).replaceAll("\\{0}", BotStart.getMapPrefix().get(event.getGuild().getId()) == null ? "!hg" : BotStart.getMapPrefix().get(event.getGuild().getId()))).queue();
            } else {
                HangmanRegistry.getInstance().setHangman(event.getUser().getIdLong(), new Hangman(event.getUser().getId(), event.getGuild().getId(), event.getChannel().getIdLong()));
                HangmanRegistry.getInstance().getActiveHangman().get(event.getUser().getIdLong()).startGame(event);
            }
            return;
        }

        if (event.getName().equals("hg-stop")) {

            if (HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
                HangmanRegistry.getInstance().getActiveHangman().remove(event.getUser().getIdLong());

                event.reply(jsonParsers.getLocale("Hangman_Eng_game",
                                event.getUser().getId()).replaceAll("\\{0}", BotStart.getMapPrefix().get(event.getGuild().getId()) == null ? "!hg" : BotStart.getMapPrefix().get(event.getGuild().getId())))
                        .addActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.START_NEW_GAME, "Play again"))
                        .queue();
                DataBase.getInstance().deleteActiveGame(event.getUser().getId());
                return;
            }

            if (!HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
                event.reply(jsonParsers.getLocale("Hangman_You_Are_Not_Play", event.getUser().getId()))
                        .addActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.START_NEW_GAME, "Play again"))
                        .queue();
            }
            return;
        }

        if (event.getName().equals("language") && HangmanRegistry.getInstance().hasHangman(event.getUser().getIdLong())) {
            event.reply(jsonParsers.getLocale("ReactionsButton_When_Play", event.getUser().getId()))
                    .addActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.START_NEW_GAME, "Play again"))
                    .queue();
            return;
        }

        //0 - game | 1 - bot
        if (event.getName().equals("language")) {

            new GameLanguageChange().changeGameLanguage(event.getOptions().get(0).getAsString(), event.getUser().getId());

            BotStart.getMapLanguages().put(event.getUser().getId(), event.getOptions().get(1).getAsString());
            DataBase.getInstance().addLanguageToDB(event.getUser().getId(), event.getOptions().get(1).getAsString());

            event.reply(jsonParsers.getLocale("slash_language", event.getUser().getId())
                            .replaceAll("\\{0}", event.getOptions().get(0).getAsString())
                            .replaceAll("\\{1}", event.getOptions().get(1).getAsString()))

                    .addActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.START_NEW_GAME, "Play again"))
                    .queue();
        }
    }
}