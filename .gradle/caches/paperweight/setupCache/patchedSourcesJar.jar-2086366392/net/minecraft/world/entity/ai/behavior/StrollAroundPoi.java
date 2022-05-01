package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class StrollAroundPoi extends Behavior<PathfinderMob> {
    private static final int MIN_TIME_BETWEEN_STROLLS = 180;
    private static final int STROLL_MAX_XZ_DIST = 8;
    private static final int STROLL_MAX_Y_DIST = 6;
    private final MemoryModuleType<GlobalPos> memoryType;
    private long nextOkStartTime;
    private final int maxDistanceFromPoi;
    private final float speedModifier;

    public StrollAroundPoi(MemoryModuleType<GlobalPos> target, float walkSpeed, int maxDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, target, MemoryStatus.VALUE_PRESENT));
        this.memoryType = target;
        this.speedModifier = walkSpeed;
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
            Optional<Vec3> optional = Optional.ofNullable(LandRandomPos.getPos(entity, 8, 6));
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, optional.map((vec3) -> {
                return new WalkTarget(vec3, this.speedModifier, 1);
            }));
            this.nextOkStartTime = time + 180L;
        }

    }
}
