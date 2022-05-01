package net.minecraft.gametest.framework;

public interface GameTestListener {
    void testStructureLoaded(GameTestInfo test);

    void testPassed(GameTestInfo test);

    void testFailed(GameTestInfo test);
}
