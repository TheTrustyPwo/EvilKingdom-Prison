package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class JumpOnBed extends Behavior<Mob> {
    private static final int MAX_TIME_TO_REACH_BED = 100;
    private static final int MIN_JUMPS = 3;
    private static final int MAX_JUMPS = 6;
    private static final int COOLDOWN_BETWEEN_JUMPS = 5;
    private final float speedModifier;
    @Nullable
    private BlockPos targetBed;
    private int remainingTimeToReachBed;
    private int remainingJumps;
    private int remainingCooldownUntilNextJump;

    public JumpOnBed(float walkSpeed) {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_BED, MemoryStatus.VALUE_PRESENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = walkSpeed;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Mob entity) {
        return entity.isBaby() && this.nearBed(world, entity);
    }

    @Override
    protected void start(ServerLevel world, Mob entity, long time) {
        super.start(world, entity, time);
        this.getNearestBed(entity).ifPresent((pos) -> {
            this.targetBed = pos;
            this.remainingTimeToReachBed = 100;
            this.remainingJumps = 3 + world.random.nextInt(4);
            this.remainingCooldownUntilNextJump = 0;
            this.startWalkingTowardsBed(entity, pos);
        });
    }

    @Override
    protected void stop(ServerLevel serverLevel, Mob mob, long l) {
        super.stop(serverLevel, mob, l);
        this.targetBed = null;
        this.remainingTimeToReachBed = 0;
        this.remainingJumps = 0;
        this.remainingCooldownUntilNextJump = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Mob mob, long l) {
        return mob.isBaby() && this.targetBed != null && this.isBed(serverLevel, this.targetBed) && !this.tiredOfWalking(serverLevel, mob) && !this.tiredOfJumping(serverLevel, mob);
    }

    @Override
    protected boolean timedOut(long time) {
        return false;
    }

    @Override
    protected void tick(ServerLevel world, Mob entity, long time) {
        if (!this.onOrOverBed(world, entity)) {
            --this.remainingTimeToReachBed;
        } else if (this.remainingCooldownUntilNextJump > 0) {
            --this.remainingCooldownUntilNextJump;
        } else {
            if (this.onBedSurface(world, entity)) {
                entity.getJumpControl().jump();
                --this.remainingJumps;
                this.remainingCooldownUntilNextJump = 5;
            }

        }
    }

    private void startWalkingTowardsBed(Mob mob, BlockPos pos) {
        mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, this.speedModifier, 0));
    }

    private boolean nearBed(ServerLevel world, Mob mob) {
        return this.onOrOverBed(world, mob) || this.getNearestBed(mob).isPresent();
    }

    private boolean onOrOverBed(ServerLevel world, Mob mob) {
        BlockPos blockPos = mob.blockPosition();
        BlockPos blockPos2 = blockPos.below();
        return this.isBed(world, blockPos) || this.isBed(world, blockPos2);
    }

    private boolean onBedSurface(ServerLevel world, Mob mob) {
        return this.isBed(world, mob.blockPosition());
    }

    private boolean isBed(ServerLevel world, BlockPos pos) {
        return world.getBlockState(pos).is(BlockTags.BEDS);
    }

    private Optional<BlockPos> getNearestBed(Mob mob) {
        return mob.getBrain().getMemory(MemoryModuleType.NEAREST_BED);
    }

    private boolean tiredOfWalking(ServerLevel world, Mob mob) {
        return !this.onOrOverBed(world, mob) && this.remainingTimeToReachBed <= 0;
    }

    private boolean tiredOfJumping(ServerLevel world, Mob mob) {
        return this.onOrOverBed(world, mob) && this.remainingJumps <= 0;
    }
}
