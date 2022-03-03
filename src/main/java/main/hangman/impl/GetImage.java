package main.hangman.impl;

public interface GetImage {

    String HANGMAN_URL = "https://megoru.ru/hangman3/";

    static String get(int count) {
        return HANGMAN_URL + count + ".png";
    }
}
