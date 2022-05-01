package net.minecraft.gametest.framework;

public interface TestReporter {
    void onTestFailed(GameTestInfo test);

    void onTestSuccess(GameTestInfo test);

    default void finish() {
    }
}
