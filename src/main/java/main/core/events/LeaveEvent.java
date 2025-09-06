package main.core.events;

import lombok.AllArgsConstructor;
import main.service.UpdateStatisticsService;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
@AllArgsConstructor
public class LeaveEvent {

    private final static Logger LOGGER = Logger.getLogger(LeaveEvent.class.getName());
    private final UpdateStatisticsService updateStatisticsService;

    public void leave(@NotNull GuildLeaveEvent event) {
        LOGGER.info("Guild leave: " + event.getGuild().getId());

        updateStatisticsService.update(event.getJDA());
    }
}