package main.hangman.impl;

public class UnsuccessfulHttpException extends Exception {

    public UnsuccessfulHttpException(int code, String message) {
        super("The server responded with code: " + code + ", message: " + message);
    }

}
