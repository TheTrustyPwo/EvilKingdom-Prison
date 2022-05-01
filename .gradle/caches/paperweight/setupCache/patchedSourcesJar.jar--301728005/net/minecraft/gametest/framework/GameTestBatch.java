package net.minecraft.gametest.framework;

import java.util.Collection;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;

public class GameTestBatch {
    public static final String DEFAULT_BATCH_NAME = "defaultBatch";
    private final String name;
    private final Collection<TestFunction> testFunctions;
    @Nullable
    private final Consumer<ServerLevel> beforeBatchFunction;
    @Nullable
    private final Consumer<ServerLevel> afterBatchFunction;

    public GameTestBatch(String id, Collection<TestFunction> testFunctions, @Nullable Consumer<ServerLevel> beforeBatchConsumer, @Nullable Consumer<ServerLevel> afterBatchConsumer) {
        if (testFunctions.isEmpty()) {
            throw new IllegalArgumentException("A GameTestBatch must include at least one TestFunction!");
        } else {
            this.name = id;
            this.testFunctions = testFunctions;
            this.beforeBatchFunction = beforeBatchConsumer;
            this.afterBatchFunction = afterBatchConsumer;
        }
    }

    public String getName() {
        return this.name;
    }

    public Collection<TestFunction> getTestFunctions() {
        return this.testFunctions;
    }

    public void runBeforeBatchFunction(ServerLevel world) {
        if (this.beforeBatchFunction != null) {
            this.beforeBatchFunction.accept(world);
        }

    }

    public void runAfterBatchFunction(ServerLevel world) {
        if (this.afterBatchFunction != null) {
            this.afterBatchFunction.accept(world);
        }

    }
}
