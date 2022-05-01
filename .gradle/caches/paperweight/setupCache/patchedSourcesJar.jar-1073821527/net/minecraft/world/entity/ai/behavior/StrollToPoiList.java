package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;

public class StrollToPoiList extends Behavior<Villager> {
    private final MemoryModuleType<List<GlobalPos>> strollToMemoryType;
    private final MemoryModuleType<GlobalPos> mustBeCloseToMemoryType;
    private final float speedModifier;
    private final int closeEnoughDist;
    private final int maxDistanceFromPoi;
    private long nextOkStartTime;
    @Nullable
    private GlobalPos targetPos;

    public StrollToPoiList(MemoryModuleType<List<GlobalPos>> secondaryPositions, float speed, int completionRange, int primaryPositionActivationDistance, MemoryModuleType<GlobalPos> primaryPosition) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, secondaryPositions, MemoryStatus.VALUE_PRESENT, primaryPosition, MemoryStatus.VALUE_PRESENT));
        this.strollToMemoryType = secondaryPositions;
        this.speedModifier = speed;
        this.closeEnoughDist = completionRange;
        this.maxDistanceFromPoi = primaryPositionActivationDistance;
        this.mustBeCloseToMemoryType = primaryPosition;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Villager entity) {
        Optional<List<GlobalPos>> optional = entity.getBrain().getMemory(this.strollToMemoryType);
        Optional<GlobalPos> optional2 = entity.getBrain().getMemory(this.mustBeCloseToMemoryType);
        if (optional.isPresent() && optional2.isPresent()) {
            List<GlobalPos> list = optional.get();
            if (!list.isEmpty()) {
                this.targetPos = list.get(world.getRandom().nextInt(list.size()));
                return this.targetPos != null && world.dimension() == this.targetPos.dimension() && optional2.get().pos().closerToCenterThan(entity.position(), (double)this.maxDistanceFromPoi);
            }
        }

        return false;
    }

    @Override
    protected void start(ServerLevel world, Villager entity, long time) {
        if (time > this.nextOkStartTime && this.targetPos != null) {
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetPos.pos(), this.speedModifier, this.closeEnoughDist));
            this.nextOkStartTime = time + 100L;
        }

    }
}
