package net.minecraft.gametest.framework;

public class GameTestTimeoutException extends RuntimeException {
    public GameTestTimeoutException(String message) {
        super(message);
    }
}
