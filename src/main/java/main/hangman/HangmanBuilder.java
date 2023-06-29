package main.hangman;

import main.controller.UpdateController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface HangmanBuilder {

    class Builder {

        //UpdateController
        private UpdateController updateController;

        //User|Guild|Channel data

        private final List<HangmanPlayer> hangmanPlayerList = new ArrayList<>();
        private long messageId;

        //For restoring
        private String guesses;
        private String word;
        private String currentHiddenWord;
        private int hangmanErrors;
        private LocalDateTime localDateTime;

        private HangmanPlayer[] toArray() {
            List<HangmanPlayer> list = hangmanPlayerList.stream().distinct().toList();
            int size = list.size();
            HangmanPlayer[] hangmanPlayers = new HangmanPlayer[size];
            for (int i = 0; i < list.size(); i++) {
                HangmanPlayer hangmanPlayer = list.get(i);
                hangmanPlayers[i] = hangmanPlayer;
            }
            return hangmanPlayers;
        }

        public Builder setMessageId(long messageId) {
            this.messageId = messageId;
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

        public Builder setLocalDateTime(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
            return this;
        }

        public Builder addHangmanPlayer(HangmanPlayer... hangmanPlayer) {
            hangmanPlayerList.addAll(List.of(hangmanPlayer));
            return this;
        }

        public Builder setUpdateController(UpdateController updateController) {
            this.updateController = updateController;
            return this;
        }

        /**
         * @throws IllegalArgumentException if hangmanGameRepository, playerRepository, gamesRepository, channelId == null
         */
        public Hangman build() {
            if (updateController == null)
                throw new IllegalArgumentException("The provided updateController cannot be null!");

            if (word != null && currentHiddenWord != null) {
                return new Hangman(
                        messageId,
                        guesses,
                        word,
                        currentHiddenWord,
                        hangmanErrors,
                        localDateTime,
                        updateController,
                        toArray());
            }
            return new Hangman(updateController, toArray());
        }

    }
}
