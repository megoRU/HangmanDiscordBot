package main.statistic;

import lombok.Getter;

@Getter
public class GameStatsByUser {

    private final double percentage;
    private final String ID_LONG;
    private final String COUNT_GAMES;

    public GameStatsByUser(double percentage, String id_long, String COUNT_GAMES) {
        this.percentage = percentage;
        this.ID_LONG = id_long;
        this.COUNT_GAMES = COUNT_GAMES;
    }

}
