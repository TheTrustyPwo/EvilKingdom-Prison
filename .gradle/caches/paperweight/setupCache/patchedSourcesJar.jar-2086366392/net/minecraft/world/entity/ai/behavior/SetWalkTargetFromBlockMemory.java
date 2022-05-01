package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetFromBlockMemory extends Behavior<Villager> {
    private final MemoryModuleType<GlobalPos> memoryType;
    private final float speedModifier;
    private final int closeEnoughDist;
    private final int tooFarDistance;
    private final int tooLongUnreachableDuration;

    public SetWalkTargetFromBlockMemory(MemoryModuleType<GlobalPos> destination, float speed, int completionRange, int maxRange, int maxRunTime) {
        super(ImmutableMap.of(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, destination, MemoryStatus.VALUE_PRESENT));
        this.memoryType = destination;
        this.speedModifier = speed;
        this.closeEnoughDist = completionRange;
        this.tooFarDistance = maxRange;
        this.tooLongUnreachableDuration = maxRunTime;
    }

    private void dropPOI(Villager villager, long time) {
        Brain<?> brain = villager.getBrain();
        villager.releasePoi(this.memoryType);
        brain.eraseMemory(this.memoryType);
        brain.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, time);
    }

    @Override
    protected void start(ServerLevel world, Villager entity, long time) {
        Brain<?> brain = entity.getBrain();
        brain.getMemory(this.memoryType).ifPresent((pos) -> {
            if (!this.wrongDimension(world, pos) && !this.tiredOfTryingToFindTarget(world, entity)) {
                if (this.tooFar(entity, pos)) {
                    Vec3 vec3 = null;
                    int i = 0;

                    for(int j = 1000; i < 1000 && (vec3 == null || this.tooFar(entity, GlobalPos.of(world.dimension(), new BlockPos(vec3)))); ++i) {
                        vec3 = DefaultRandomPos.getPosTowards(entity, 15, 7, Vec3.atBottomCenterOf(pos.pos()), (double)((float)Math.PI / 2F));
                    }

                    if (i == 1000) {
                        this.dropPOI(entity, time);
                        return;
                    }

                    brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(vec3, this.speedModifier, this.closeEnoughDist));
                } else if (!this.closeEnough(world, entity, pos)) {
                    brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos.pos(), this.speedModifier, this.closeEnoughDist));
                }
            } else {
                this.dropPOI(entity, time);
            }

        });
    }

    private boolean tiredOfTryingToFindTarget(ServerLevel world, Villager villager) {
        Optional<Long> optional = villager.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        if (optional.isPresent()) {
            return world.getGameTime() - optional.get() > (long)this.tooLongUnreachableDuration;
        } else {
            return false;
        }
    }

    private boolean tooFar(Villager villager, GlobalPos pos) {
        return pos.pos().distManhattan(villager.blockPosition()) > this.tooFarDistance;
    }

    private boolean wrongDimension(ServerLevel world, GlobalPos pos) {
        return pos.dimension() != world.dimension();
    }

    private boolean closeEnough(ServerLevel world, Villager villager, GlobalPos pos) {
        return pos.dimension() == world.dimension() && pos.pos().distManhattan(villager.blockPosition()) <= this.closeEnoughDist;
    }
}
