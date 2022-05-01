package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class GoToCelebrateLocation<E extends Mob> extends Behavior<E> {
    private final int closeEnoughDist;
    private final float speedModifier;

    public GoToCelebrateLocation(int completionRange, float speed) {
        super(ImmutableMap.of(MemoryModuleType.CELEBRATE_LOCATION, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
        this.closeEnoughDist = completionRange;
        this.speedModifier = speed;
    }

    @Override
    protected void start(ServerLevel world, Mob entity, long time) {
        BlockPos blockPos = getCelebrateLocation(entity);
        boolean bl = blockPos.closerThan(entity.blockPosition(), (double)this.closeEnoughDist);
        if (!bl) {
            BehaviorUtils.setWalkAndLookTargetMemories(entity, getNearbyPos(entity, blockPos), this.speedModifier, this.closeEnoughDist);
        }

    }

    private static BlockPos getNearbyPos(Mob mob, BlockPos pos) {
        Random random = mob.level.random;
        return pos.offset(getRandomOffset(random), 0, getRandomOffset(random));
    }

    private static int getRandomOffset(Random random) {
        return random.nextInt(3) - 1;
    }

    private static BlockPos getCelebrateLocation(Mob entity) {
        return entity.getBrain().getMemory(MemoryModuleType.CELEBRATE_LOCATION).get();
    }
}
