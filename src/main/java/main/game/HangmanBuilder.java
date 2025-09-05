package main.game;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public interface HangmanBuilder {

    class Builder {
        //User|Guild|Channel data
        private final List<HangmanPlayer> hangmanPlayerList = new ArrayList<>();
        private Long messageId;

        //For restoring
        private String guesses;
        private String word;
        private String currentHiddenWord;
        private int hangmanErrors;
        private Instant localDateTime;
        private boolean isCompetitive;
        private Long againstPlayerId;
        private boolean isOpponentLose;

        private HangmanPlayer[] hangmanPlayersArrays() {
            List<HangmanPlayer> list = hangmanPlayerList.stream().distinct().toList();
            int size = list.size();
            HangmanPlayer[] hangmanPlayers = new HangmanPlayer[size];
            for (int i = 0; i < list.size(); i++) {
                HangmanPlayer hangmanPlayer = list.get(i);
                hangmanPlayers[i] = hangmanPlayer;
            }
            return hangmanPlayers;
        }

        public Builder setMessageId(Long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder setIsOpponentLose(@Nullable Boolean isOpponentLose) {
            this.isOpponentLose = Objects.requireNonNullElse(isOpponentLose, false);
            return this;
        }

        public Builder setCompetitive(boolean isCompetitive) {
            this.isCompetitive = isCompetitive;
            return this;
        }

        public Builder setAgainstPlayerId(Long againstPlayerId) {
            this.againstPlayerId = againstPlayerId;
            return this;
        }

        public Builder setGuesses(String guesses) {
            this.guesses = guesses;
            return this;
        }

        public Builder setWord(String word) {
            this.word = word;
            return this;
        }

        public Builder setCurrentHiddenWord(String currentHiddenWord) {
            this.currentHiddenWord = currentHiddenWord;
            return this;
        }

        public Builder setHangmanErrors(int hangmanErrors) {
            this.hangmanErrors = hangmanErrors;
            return this;
        }

        public Builder setLocalDateTime(Instant localDateTime) {
            this.localDateTime = localDateTime;
            return this;
        }

        public Builder addHangmanPlayer(HangmanPlayer... hangmanPlayer) {
            hangmanPlayerList.addAll(List.of(hangmanPlayer));
            return this;
        }

        /**
         * @throws IllegalArgumentException if hangmanGameRepository, playerRepository, gamesRepository, channelId == null
         */
        public Hangman build() {
            Hangman hangman = new Hangman();

            if (word != null && currentHiddenWord != null) {
                return hangman.update(
                        messageId,
                        guesses,
                        word,
                        currentHiddenWord,
                        hangmanErrors,
                        localDateTime,
                        isCompetitive,
                        againstPlayerId,
                        isOpponentLose,
                        hangmanPlayersArrays());
            } else {
                hangman.setHangmanPlayers(hangmanPlayersArrays());
                hangman.setCompetitive(isCompetitive);
                hangman.setAgainstPlayerId(againstPlayerId);
                return hangman;
            }
        }
    }
}
