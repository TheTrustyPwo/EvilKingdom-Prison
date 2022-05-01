package net.minecraft.server;

public class TickTask implements Runnable {
    private final int tick;
    private final Runnable runnable;

    public TickTask(int creationTicks, Runnable runnable) {
        this.tick = creationTicks;
        this.runnable = runnable;
    }

    public int getTick() {
        return this.tick;
    }

    @Override
    public void run() {
        this.runnable.run();
    }
}
