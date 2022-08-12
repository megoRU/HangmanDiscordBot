package main.eventlisteners.selections;

import main.hangman.HangmanRegistry;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class SelectMenuEvent extends ListenerAdapter {

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        long userIdLong = event.getUser().getIdLong();

        if (!HangmanRegistry.getInstance().hasHangman(userIdLong)) return;
        event.deferEdit().queue();
        String letter = event.getInteraction().getSelectedOptions().get(0).getValue();

        if (letter.length() == 1)
            HangmanRegistry.getInstance().getActiveHangman().get(userIdLong).logic(letter, event.getMessage());
    }
}
