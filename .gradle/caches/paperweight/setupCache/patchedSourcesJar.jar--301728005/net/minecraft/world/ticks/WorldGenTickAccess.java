package net.minecraft.world.ticks;

import java.util.function.Function;
import net.minecraft.core.BlockPos;

public class WorldGenTickAccess<T> implements LevelTickAccess<T> {
    private final Function<BlockPos, TickContainerAccess<T>> containerGetter;

    public WorldGenTickAccess(Function<BlockPos, TickContainerAccess<T>> mapper) {
        this.containerGetter = mapper;
    }

    @Override
    public boolean hasScheduledTick(BlockPos pos, T type) {
        return this.containerGetter.apply(pos).hasScheduledTick(pos, type);
    }

    @Override
    public void schedule(ScheduledTick<T> orderedTick) {
        this.containerGetter.apply(orderedTick.pos()).schedule(orderedTick);
    }

    @Override
    public boolean willTickThisTick(BlockPos pos, T type) {
        return false;
    }

    @Override
    public int count() {
        return 0;
    }
}
