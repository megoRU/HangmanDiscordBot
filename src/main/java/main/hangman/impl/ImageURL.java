package main.hangman.impl;

public interface ImageURL {

    static String get(int count) {
        return String.format("https://megoru.ru/hangman/%s.png", count);
    }
}
