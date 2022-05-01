package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class StrollToPoi extends Behavior<PathfinderMob> {
    private final MemoryModuleType<GlobalPos> memoryType;
    private final int closeEnoughDist;
    private final int maxDistanceFromPoi;
    private final float speedModifier;
    private long nextOkStartTime;

    public StrollToPoi(MemoryModuleType<GlobalPos> memoryModuleType, float walkSpeed, int completionRange, int maxDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, memoryModuleType, MemoryStatus.VALUE_PRESENT));
        this.memoryType = memoryModuleType;
        this.speedModifier = walkSpeed;
        this.closeEnoughDist = completionRange;
        this.maxDistanceFromPoi = maxDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, PathfinderMob entity) {
        Optional<GlobalPos> optional = entity.getBrain().getMemory(this.memoryType);
        return optional.isPresent() && world.dimension() == optional.get().dimension() && optional.get().pos().closerToCenterThan(entity.position(), (double)this.maxDistanceFromPoi);
    }

    @Override
    protected void start(ServerLevel world, PathfinderMob entity, long time) {
        if (time > this.nextOkStartTime) {
            Brain<?> brain = entity.getBrain();
            Optional<GlobalPos> optional = brain.getMemory(this.memoryType);
            optional.ifPresent((globalPos) -> {
                brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(globalPos.pos(), this.speedModifier, this.closeEnoughDist));
            });
            this.nextOkStartTime = time + 80L;
        }

    }
}
