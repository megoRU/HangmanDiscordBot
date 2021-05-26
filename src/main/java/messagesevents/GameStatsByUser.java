package messagesevents;

public class GameStatsByUser {

    private final double percentage;
    private final String ID_LONG;
    private final String COUNT_GAMES;

    public GameStatsByUser(double percentage, String id_long, String COUNT_GAMES) {
        this.percentage = percentage;
        this.ID_LONG = id_long;
        this.COUNT_GAMES = COUNT_GAMES;
    }

    public double getPercentage() {
        return percentage;
    }

    public String getID_LONG() {
        return ID_LONG;
    }

    public String getCOUNT_GAMES() {
        return COUNT_GAMES;
    }

}
