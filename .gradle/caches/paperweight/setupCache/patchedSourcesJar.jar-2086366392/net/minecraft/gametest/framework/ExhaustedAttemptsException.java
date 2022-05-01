package net.minecraft.gametest.framework;

class ExhaustedAttemptsException extends Throwable {
    public ExhaustedAttemptsException(int attempts, int successes, GameTestInfo test) {
        super("Not enough successes: " + successes + " out of " + attempts + " attempts. Required successes: " + test.requiredSuccesses() + ". max attempts: " + test.maxAttempts() + ".", test.getError());
    }
}
