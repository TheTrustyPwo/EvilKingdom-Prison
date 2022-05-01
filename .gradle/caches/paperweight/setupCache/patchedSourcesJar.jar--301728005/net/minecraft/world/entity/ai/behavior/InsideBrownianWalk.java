package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InsideBrownianWalk extends Behavior<PathfinderMob> {
    private final float speedModifier;

    public InsideBrownianWalk(float speed) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, PathfinderMob entity) {
        return !world.canSeeSky(entity.blockPosition());
    }

    @Override
    protected void start(ServerLevel world, PathfinderMob entity, long time) {
        BlockPos blockPos = entity.blockPosition();
        List<BlockPos> list = BlockPos.betweenClosedStream(blockPos.offset(-1, -1, -1), blockPos.offset(1, 1, 1)).map(BlockPos::immutable).collect(Collectors.toList());
        Collections.shuffle(list);
        Optional<BlockPos> optional = list.stream().filter((pos) -> {
            return !world.canSeeSky(pos);
        }).filter((pos) -> {
            return world.loadedAndEntityCanStandOn(pos, entity);
        }).filter((blockPosx) -> {
            return world.noCollision(entity);
        }).findFirst();
        optional.ifPresent((pos) -> {
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, this.speedModifier, 0));
        });
    }
}
