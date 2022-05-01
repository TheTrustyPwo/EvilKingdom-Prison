package net.minecraft.gametest.framework;

public class GlobalTestReporter {
    private static TestReporter DELEGATE = new LogTestReporter();

    public static void replaceWith(TestReporter listener) {
        DELEGATE = listener;
    }

    public static void onTestFailed(GameTestInfo test) {
        DELEGATE.onTestFailed(test);
    }

    public static void onTestSuccess(GameTestInfo test) {
        DELEGATE.onTestSuccess(test);
    }

    public static void finish() {
        DELEGATE.finish();
    }
}
