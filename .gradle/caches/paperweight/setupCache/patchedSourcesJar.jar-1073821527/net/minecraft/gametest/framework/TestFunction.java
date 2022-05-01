package net.minecraft.gametest.framework;

import java.util.function.Consumer;
import net.minecraft.world.level.block.Rotation;

public class TestFunction {
    private final String batchName;
    private final String testName;
    private final String structureName;
    private final boolean required;
    private final int maxAttempts;
    private final int requiredSuccesses;
    private final Consumer<GameTestHelper> function;
    private final int maxTicks;
    private final long setupTicks;
    private final Rotation rotation;

    public TestFunction(String batchId, String structurePath, String structureName, int tickLimit, long duration, boolean required, Consumer<GameTestHelper> starter) {
        this(batchId, structurePath, structureName, Rotation.NONE, tickLimit, duration, required, 1, 1, starter);
    }

    public TestFunction(String batchId, String structurePath, String structureName, Rotation rotation, int tickLimit, long duration, boolean required, Consumer<GameTestHelper> starter) {
        this(batchId, structurePath, structureName, rotation, tickLimit, duration, required, 1, 1, starter);
    }

    public TestFunction(String batchId, String structurePath, String structureName, Rotation rotation, int tickLimit, long duration, boolean required, int requiredSuccesses, int maxAttempts, Consumer<GameTestHelper> starter) {
        this.batchName = batchId;
        this.testName = structurePath;
        this.structureName = structureName;
        this.rotation = rotation;
        this.maxTicks = tickLimit;
        this.required = required;
        this.requiredSuccesses = requiredSuccesses;
        this.maxAttempts = maxAttempts;
        this.function = starter;
        this.setupTicks = duration;
    }

    public void run(GameTestHelper context) {
        this.function.accept(context);
    }

    public String getTestName() {
        return this.testName;
    }

    public String getStructureName() {
        return this.structureName;
    }

    @Override
    public String toString() {
        return this.testName;
    }

    public int getMaxTicks() {
        return this.maxTicks;
    }

    public boolean isRequired() {
        return this.required;
    }

    public String getBatchName() {
        return this.batchName;
    }

    public long getSetupTicks() {
        return this.setupTicks;
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public boolean isFlaky() {
        return this.maxAttempts > 1;
    }

    public int getMaxAttempts() {
        return this.maxAttempts;
    }

    public int getRequiredSuccesses() {
        return this.requiredSuccesses;
    }
}
