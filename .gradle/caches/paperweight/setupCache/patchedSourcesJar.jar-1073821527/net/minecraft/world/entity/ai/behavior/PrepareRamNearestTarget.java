package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class PrepareRamNearestTarget<E extends PathfinderMob> extends Behavior<E> {
    public static final int TIME_OUT_DURATION = 160;
    private final ToIntFunction<E> getCooldownOnFail;
    private final int minRamDistance;
    private final int maxRamDistance;
    private final float walkSpeed;
    private final TargetingConditions ramTargeting;
    private final int ramPrepareTime;
    private final Function<E, SoundEvent> getPrepareRamSound;
    private Optional<Long> reachedRamPositionTimestamp = Optional.empty();
    private Optional<PrepareRamNearestTarget.RamCandidate> ramCandidate = Optional.empty();

    public PrepareRamNearestTarget(ToIntFunction<E> cooldownFactory, int minDistance, int maxDistance, float speed, TargetingConditions targetPredicate, int prepareTime, Function<E, SoundEvent> soundFactory) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT, MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_ABSENT), 160);
        this.getCooldownOnFail = cooldownFactory;
        this.minRamDistance = minDistance;
        this.maxRamDistance = maxDistance;
        this.walkSpeed = speed;
        this.ramTargeting = targetPredicate;
        this.ramPrepareTime = prepareTime;
        this.getPrepareRamSound = soundFactory;
    }

    @Override
    protected void start(ServerLevel world, PathfinderMob entity, long time) {
        Brain<?> brain = entity.getBrain();
        brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).flatMap((nearestVisibleLivingEntities) -> {
            return nearestVisibleLivingEntities.findClosest((mob) -> {
                return this.ramTargeting.test(entity, mob);
            });
        }).ifPresent((mob) -> {
            this.chooseRamPosition(entity, mob);
        });
    }

    @Override
    protected void stop(ServerLevel serverLevel, E pathfinderMob, long l) {
        Brain<?> brain = pathfinderMob.getBrain();
        if (!brain.hasMemoryValue(MemoryModuleType.RAM_TARGET)) {
            serverLevel.broadcastEntityEvent(pathfinderMob, (byte)59);
            brain.setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getCooldownOnFail.applyAsInt(pathfinderMob));
        }

    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, PathfinderMob pathfinderMob, long l) {
        return this.ramCandidate.isPresent() && this.ramCandidate.get().getTarget().isAlive();
    }

    @Override
    protected void tick(ServerLevel world, E entity, long time) {
        if (this.ramCandidate.isPresent()) {
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.ramCandidate.get().getStartPosition(), this.walkSpeed, 0));
            entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.ramCandidate.get().getTarget(), true));
            boolean bl = !this.ramCandidate.get().getTarget().blockPosition().equals(this.ramCandidate.get().getTargetPosition());
            if (bl) {
                world.broadcastEntityEvent(entity, (byte)59);
                entity.getNavigation().stop();
                this.chooseRamPosition(entity, (this.ramCandidate.get()).target);
            } else {
                BlockPos blockPos = entity.blockPosition();
                if (blockPos.equals(this.ramCandidate.get().getStartPosition())) {
                    world.broadcastEntityEvent(entity, (byte)58);
                    if (!this.reachedRamPositionTimestamp.isPresent()) {
                        this.reachedRamPositionTimestamp = Optional.of(time);
                    }

                    if (time - this.reachedRamPositionTimestamp.get() >= (long)this.ramPrepareTime) {
                        entity.getBrain().setMemory(MemoryModuleType.RAM_TARGET, this.getEdgeOfBlock(blockPos, this.ramCandidate.get().getTargetPosition()));
                        world.playSound((Player)null, entity, this.getPrepareRamSound.apply(entity), SoundSource.HOSTILE, 1.0F, entity.getVoicePitch());
                        this.ramCandidate = Optional.empty();
                    }
                }
            }

        }
    }

    private Vec3 getEdgeOfBlock(BlockPos start, BlockPos end) {
        double d = 0.5D;
        double e = 0.5D * (double)Mth.sign((double)(end.getX() - start.getX()));
        double f = 0.5D * (double)Mth.sign((double)(end.getZ() - start.getZ()));
        return Vec3.atBottomCenterOf(end).add(e, 0.0D, f);
    }

    private Optional<BlockPos> calculateRammingStartPosition(PathfinderMob entity, LivingEntity target) {
        BlockPos blockPos = target.blockPosition();
        if (!this.isWalkableBlock(entity, blockPos)) {
            return Optional.empty();
        } else {
            List<BlockPos> list = Lists.newArrayList();
            BlockPos.MutableBlockPos mutableBlockPos = blockPos.mutable();

            for(Direction direction : Direction.Plane.HORIZONTAL) {
                mutableBlockPos.set(blockPos);

                for(int i = 0; i < this.maxRamDistance; ++i) {
                    if (!this.isWalkableBlock(entity, mutableBlockPos.move(direction))) {
                        mutableBlockPos.move(direction.getOpposite());
                        break;
                    }
                }

                if (mutableBlockPos.distManhattan(blockPos) >= this.minRamDistance) {
                    list.add(mutableBlockPos.immutable());
                }
            }

            PathNavigation pathNavigation = entity.getNavigation();
            return list.stream().sorted(Comparator.comparingDouble(entity.blockPosition()::distSqr)).filter((start) -> {
                Path path = pathNavigation.createPath(start, 0);
                return path != null && path.canReach();
            }).findFirst();
        }
    }

    private boolean isWalkableBlock(PathfinderMob entity, BlockPos target) {
        return entity.getNavigation().isStableDestination(target) && entity.getPathfindingMalus(WalkNodeEvaluator.getBlockPathTypeStatic(entity.level, target.mutable())) == 0.0F;
    }

    private void chooseRamPosition(PathfinderMob entity, LivingEntity target) {
        this.reachedRamPositionTimestamp = Optional.empty();
        this.ramCandidate = this.calculateRammingStartPosition(entity, target).map((start) -> {
            return new PrepareRamNearestTarget.RamCandidate(start, target.blockPosition(), target);
        });
    }

    public static class RamCandidate {
        private final BlockPos startPosition;
        private final BlockPos targetPosition;
        final LivingEntity target;

        public RamCandidate(BlockPos start, BlockPos end, LivingEntity entity) {
            this.startPosition = start;
            this.targetPosition = end;
            this.target = entity;
        }

        public BlockPos getStartPosition() {
            return this.startPosition;
        }

        public BlockPos getTargetPosition() {
            return this.targetPosition;
        }

        public LivingEntity getTarget() {
            return this.target;
        }
    }
}
