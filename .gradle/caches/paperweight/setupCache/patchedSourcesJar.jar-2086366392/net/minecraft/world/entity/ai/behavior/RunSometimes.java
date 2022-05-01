package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;

public class RunSometimes<E extends LivingEntity> extends Behavior<E> {
    private boolean resetTicks;
    private boolean wasRunning;
    private final UniformInt interval;
    private final Behavior<? super E> wrappedBehavior;
    private int ticksUntilNextStart;

    public RunSometimes(Behavior<? super E> delegate, UniformInt timeRange) {
        this(delegate, false, timeRange);
    }

    public RunSometimes(Behavior<? super E> delegate, boolean skipFirstRun, UniformInt timeRange) {
        super(delegate.entryCondition);
        this.wrappedBehavior = delegate;
        this.resetTicks = !skipFirstRun;
        this.interval = timeRange;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        if (!this.wrappedBehavior.checkExtraStartConditions(world, entity)) {
            return false;
        } else {
            if (this.resetTicks) {
                this.resetTicksUntilNextStart(world);
                this.resetTicks = false;
            }

            if (this.ticksUntilNextStart > 0) {
                --this.ticksUntilNextStart;
            }

            return !this.wasRunning && this.ticksUntilNextStart == 0;
        }
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        this.wrappedBehavior.start(world, entity, time);
    }

    @Override
    protected boolean canStillUse(ServerLevel world, E entity, long time) {
        return this.wrappedBehavior.canStillUse(world, entity, time);
    }

    @Override
    protected void tick(ServerLevel world, E entity, long time) {
        this.wrappedBehavior.tick(world, entity, time);
        this.wasRunning = this.wrappedBehavior.getStatus() == Behavior.Status.RUNNING;
    }

    @Override
    protected void stop(ServerLevel world, E entity, long time) {
        this.resetTicksUntilNextStart(world);
        this.wrappedBehavior.stop(world, entity, time);
    }

    private void resetTicksUntilNextStart(ServerLevel world) {
        this.ticksUntilNextStart = this.interval.sample(world.random);
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    public String toString() {
        return "RunSometimes: " + this.wrappedBehavior;
    }
}
