package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class MoveToSkySeeingSpot extends Behavior<LivingEntity> {
    private final float speedModifier;

    public MoveToSkySeeingSpot(float speed) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = speed;
    }

    @Override
    protected void start(ServerLevel world, LivingEntity entity, long time) {
        Optional<Vec3> optional = Optional.ofNullable(this.getOutdoorPosition(world, entity));
        if (optional.isPresent()) {
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, optional.map((pos) -> {
                return new WalkTarget(pos, this.speedModifier, 0);
            }));
        }

    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        return !world.canSeeSky(entity.blockPosition());
    }

    @Nullable
    private Vec3 getOutdoorPosition(ServerLevel world, LivingEntity entity) {
        Random random = entity.getRandom();
        BlockPos blockPos = entity.blockPosition();

        for(int i = 0; i < 10; ++i) {
            BlockPos blockPos2 = blockPos.offset(random.nextInt(20) - 10, random.nextInt(6) - 3, random.nextInt(20) - 10);
            if (hasNoBlocksAbove(world, entity, blockPos2)) {
                return Vec3.atBottomCenterOf(blockPos2);
            }
        }

        return null;
    }

    public static boolean hasNoBlocksAbove(ServerLevel world, LivingEntity entity, BlockPos pos) {
        return world.canSeeSky(pos) && (double)world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() <= entity.getY();
    }
}
