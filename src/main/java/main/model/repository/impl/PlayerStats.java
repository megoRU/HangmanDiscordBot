package main.model.repository.impl;

public interface PlayerStats {
    Long getId();

    Long getWins();

    Long getLosses();

    Long getTotalGames();

    Integer getWinrate(); // округлённый int
}