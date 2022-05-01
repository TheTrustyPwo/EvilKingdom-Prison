package net.minecraft.gametest.framework;

import javax.annotation.Nullable;

class GameTestEvent {
    @Nullable
    public final Long expectedDelay;
    public final Runnable assertion;

    private GameTestEvent(@Nullable Long duration, Runnable task) {
        this.expectedDelay = duration;
        this.assertion = task;
    }

    static GameTestEvent create(Runnable task) {
        return new GameTestEvent((Long)null, task);
    }

    static GameTestEvent create(long duration, Runnable task) {
        return new GameTestEvent(duration, task);
    }
}
