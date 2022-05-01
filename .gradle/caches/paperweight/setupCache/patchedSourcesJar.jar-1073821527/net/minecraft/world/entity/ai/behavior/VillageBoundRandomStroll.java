package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class VillageBoundRandomStroll extends Behavior<PathfinderMob> {
    private static final int MAX_XZ_DIST = 10;
    private static final int MAX_Y_DIST = 7;
    private final float speedModifier;
    private final int maxXyDist;
    private final int maxYDist;

    public VillageBoundRandomStroll(float walkSpeed) {
        this(walkSpeed, 10, 7);
    }

    public VillageBoundRandomStroll(float walkSpeed, int maxHorizontalDistance, int maxVerticalDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = walkSpeed;
        this.maxXyDist = maxHorizontalDistance;
        this.maxYDist = maxVerticalDistance;
    }

    @Override
    protected void start(ServerLevel world, PathfinderMob entity, long time) {
        BlockPos blockPos = entity.blockPosition();
        if (world.isVillage(blockPos)) {
            this.setRandomPos(entity);
        } else {
            SectionPos sectionPos = SectionPos.of(blockPos);
            SectionPos sectionPos2 = BehaviorUtils.findSectionClosestToVillage(world, sectionPos, 2);
            if (sectionPos2 != sectionPos) {
                this.setTargetedPos(entity, sectionPos2);
            } else {
                this.setRandomPos(entity);
            }
        }

    }

    private void setTargetedPos(PathfinderMob entity, SectionPos pos) {
        Optional<Vec3> optional = Optional.ofNullable(DefaultRandomPos.getPosTowards(entity, this.maxXyDist, this.maxYDist, Vec3.atBottomCenterOf(pos.center()), (double)((float)Math.PI / 2F)));
        entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, optional.map((vec3) -> {
            return new WalkTarget(vec3, this.speedModifier, 0);
        }));
    }

    private void setRandomPos(PathfinderMob entity) {
        Optional<Vec3> optional = Optional.ofNullable(LandRandomPos.getPos(entity, this.maxXyDist, this.maxYDist));
        entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, optional.map((vec3) -> {
            return new WalkTarget(vec3, this.speedModifier, 0);
        }));
    }
}
