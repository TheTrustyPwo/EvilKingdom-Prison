package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;

public class LocateHidingPlace extends Behavior<LivingEntity> {
    private final float speedModifier;
    private final int radius;
    private final int closeEnoughDist;
    private Optional<BlockPos> currentPos = Optional.empty();

    public LocateHidingPlace(int maxDistance, float walkSpeed, int preferredDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.HOME, MemoryStatus.REGISTERED, MemoryModuleType.HIDING_PLACE, MemoryStatus.REGISTERED));
        this.radius = maxDistance;
        this.speedModifier = walkSpeed;
        this.closeEnoughDist = preferredDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        Optional<BlockPos> optional = world.getPoiManager().find((poiType) -> {
            return poiType == PoiType.HOME;
        }, (blockPos) -> {
            return true;
        }, entity.blockPosition(), this.closeEnoughDist + 1, PoiManager.Occupancy.ANY);
        if (optional.isPresent() && optional.get().closerToCenterThan(entity.position(), (double)this.closeEnoughDist)) {
            this.currentPos = optional;
        } else {
            this.currentPos = Optional.empty();
        }

        return true;
    }

    @Override
    protected void start(ServerLevel world, LivingEntity entity, long time) {
        Brain<?> brain = entity.getBrain();
        Optional<BlockPos> optional = this.currentPos;
        if (!optional.isPresent()) {
            optional = world.getPoiManager().getRandom((poiType) -> {
                return poiType == PoiType.HOME;
            }, (blockPos) -> {
                return true;
            }, PoiManager.Occupancy.ANY, entity.blockPosition(), this.radius, entity.getRandom());
            if (!optional.isPresent()) {
                Optional<GlobalPos> optional2 = brain.getMemory(MemoryModuleType.HOME);
                if (optional2.isPresent()) {
                    optional = Optional.of(optional2.get().pos());
                }
            }
        }

        if (optional.isPresent()) {
            brain.eraseMemory(MemoryModuleType.PATH);
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            brain.eraseMemory(MemoryModuleType.BREED_TARGET);
            brain.eraseMemory(MemoryModuleType.INTERACTION_TARGET);
            brain.setMemory(MemoryModuleType.HIDING_PLACE, GlobalPos.of(world.dimension(), optional.get()));
            if (!optional.get().closerToCenterThan(entity.position(), (double)this.closeEnoughDist)) {
                brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(optional.get(), this.speedModifier, this.closeEnoughDist));
            }
        }

    }
}
