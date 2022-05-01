package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class GameTestSequence {
    final GameTestInfo parent;
    private final List<GameTestEvent> events = Lists.newArrayList();
    private long lastTick;

    GameTestSequence(GameTestInfo gameTest) {
        this.parent = gameTest;
        this.lastTick = gameTest.getTick();
    }

    public GameTestSequence thenWaitUntil(Runnable task) {
        this.events.add(GameTestEvent.create(task));
        return this;
    }

    public GameTestSequence thenWaitUntil(long duration, Runnable task) {
        this.events.add(GameTestEvent.create(duration, task));
        return this;
    }

    public GameTestSequence thenIdle(int minDuration) {
        return this.thenExecuteAfter(minDuration, () -> {
        });
    }

    public GameTestSequence thenExecute(Runnable task) {
        this.events.add(GameTestEvent.create(() -> {
            this.executeWithoutFail(task);
        }));
        return this;
    }

    public GameTestSequence thenExecuteAfter(int minDuration, Runnable task) {
        this.events.add(GameTestEvent.create(() -> {
            if (this.parent.getTick() < this.lastTick + (long)minDuration) {
                throw new GameTestAssertException("Waiting");
            } else {
                this.executeWithoutFail(task);
            }
        }));
        return this;
    }

    public GameTestSequence thenExecuteFor(int minDuration, Runnable task) {
        this.events.add(GameTestEvent.create(() -> {
            if (this.parent.getTick() < this.lastTick + (long)minDuration) {
                this.executeWithoutFail(task);
                throw new GameTestAssertException("Waiting");
            }
        }));
        return this;
    }

    public void thenSucceed() {
        this.events.add(GameTestEvent.create(this.parent::succeed));
    }

    public void thenFail(Supplier<Exception> exceptionSupplier) {
        this.events.add(GameTestEvent.create(() -> {
            this.parent.fail(exceptionSupplier.get());
        }));
    }

    public GameTestSequence.Condition thenTrigger() {
        GameTestSequence.Condition condition = new GameTestSequence.Condition();
        this.events.add(GameTestEvent.create(() -> {
            condition.trigger(this.parent.getTick());
        }));
        return condition;
    }

    public void tickAndContinue(long tick) {
        try {
            this.tick(tick);
        } catch (GameTestAssertException var4) {
        }

    }

    public void tickAndFailIfNotComplete(long tick) {
        try {
            this.tick(tick);
        } catch (GameTestAssertException var4) {
            this.parent.fail(var4);
        }

    }

    private void executeWithoutFail(Runnable task) {
        try {
            task.run();
        } catch (GameTestAssertException var3) {
            this.parent.fail(var3);
        }

    }

    private void tick(long tick) {
        Iterator<GameTestEvent> iterator = this.events.iterator();

        while(iterator.hasNext()) {
            GameTestEvent gameTestEvent = iterator.next();
            gameTestEvent.assertion.run();
            iterator.remove();
            long l = tick - this.lastTick;
            long m = this.lastTick;
            this.lastTick = tick;
            if (gameTestEvent.expectedDelay != null && gameTestEvent.expectedDelay != l) {
                this.parent.fail(new GameTestAssertException("Succeeded in invalid tick: expected " + (m + gameTestEvent.expectedDelay) + ", but current tick is " + tick));
                break;
            }
        }

    }

    public class Condition {
        private static final long NOT_TRIGGERED = -1L;
        private long triggerTime = -1L;

        void trigger(long tick) {
            if (this.triggerTime != -1L) {
                throw new IllegalStateException("Condition already triggered at " + this.triggerTime);
            } else {
                this.triggerTime = tick;
            }
        }

        public void assertTriggeredThisTick() {
            long l = GameTestSequence.this.parent.getTick();
            if (this.triggerTime != l) {
                if (this.triggerTime == -1L) {
                    throw new GameTestAssertException("Condition not triggered (t=" + l + ")");
                } else {
                    throw new GameTestAssertException("Condition triggered at " + this.triggerTime + ", (t=" + l + ")");
                }
            }
        }
    }
}
