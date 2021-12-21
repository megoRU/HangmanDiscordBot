package main.threads;

import lombok.AllArgsConstructor;
import main.hangman.HangmanRegistry;
import main.model.repository.HangmanGameRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@AllArgsConstructor
public class EngGameByTime {

    private final HangmanGameRepository hangmanGameRepository;

    public void runTask() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() throws NullPointerException {
                try {
                    Map<Long, LocalDateTime> timeCreatedGame = new HashMap<>(HangmanRegistry.getInstance().getTimeCreatedGame());

                    for (Map.Entry<Long, LocalDateTime> entry : timeCreatedGame.entrySet()) {
                        Instant specificTime = Instant.ofEpochMilli(Instant.now().toEpochMilli());

                        if (entry.getValue().isBefore(ChronoLocalDateTime.from(OffsetDateTime.parse(String.valueOf(specificTime)).minusMinutes(10L)))) {
                            synchronized (this) {
                                if (HangmanRegistry.getInstance().hasHangman(entry.getKey())) {
                                    HangmanRegistry.getInstance().getActiveHangman().get(entry.getKey()).stopGameByTime();
                                    HangmanRegistry.getInstance().getTimeCreatedGame().remove(entry.getKey());
                                    hangmanGameRepository.deleteActiveGame(entry.getKey());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }, 1, 15000L);
    }
}
