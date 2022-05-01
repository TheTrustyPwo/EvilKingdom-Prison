package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LongJumpToRandomPos<E extends Mob> extends Behavior<E> {
    private static final int FIND_JUMP_TRIES = 20;
    private static final int PREPARE_JUMP_DURATION = 40;
    private static final int MIN_PATHFIND_DISTANCE_TO_VALID_JUMP = 8;
    public static final int TIME_OUT_DURATION = 200;
    private final UniformInt timeBetweenLongJumps;
    private final int maxLongJumpHeight;
    private final int maxLongJumpWidth;
    private final float maxJumpVelocity;
    private final List<LongJumpToRandomPos.PossibleJump> jumpCandidates = new ArrayList<>();
    private Optional<Vec3> initialPosition = Optional.empty();
    private Optional<LongJumpToRandomPos.PossibleJump> chosenJump = Optional.empty();
    private int findJumpTries;
    private long prepareJumpStart;
    private Function<E, SoundEvent> getJumpSound;

    public LongJumpToRandomPos(UniformInt cooldownRange, int verticalRange, int horizontalRange, float maxRange, Function<E, SoundEvent> entityToSound) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_ABSENT), 200);
        this.timeBetweenLongJumps = cooldownRange;
        this.maxLongJumpHeight = verticalRange;
        this.maxLongJumpWidth = horizontalRange;
        this.maxJumpVelocity = maxRange;
        this.getJumpSound = entityToSound;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Mob entity) {
        return entity.isOnGround() && !world.getBlockState(entity.blockPosition()).is(Blocks.HONEY_BLOCK);
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Mob mob, long l) {
        boolean bl = this.initialPosition.isPresent() && this.initialPosition.get().equals(mob.position()) && this.findJumpTries > 0 && (this.chosenJump.isPresent() || !this.jumpCandidates.isEmpty());
        if (!bl && !mob.getBrain().getMemory(MemoryModuleType.LONG_JUMP_MID_JUMP).isPresent()) {
            mob.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(serverLevel.random) / 2);
        }

        return bl;
    }

    @Override
    protected void start(ServerLevel serverLevel, Mob mob, long l) {
        this.chosenJump = Optional.empty();
        this.findJumpTries = 20;
        this.jumpCandidates.clear();
        this.initialPosition = Optional.of(mob.position());
        BlockPos blockPos = mob.blockPosition();
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        Iterable<BlockPos> iterable = BlockPos.betweenClosed(i - this.maxLongJumpWidth, j - this.maxLongJumpHeight, k - this.maxLongJumpWidth, i + this.maxLongJumpWidth, j + this.maxLongJumpHeight, k + this.maxLongJumpWidth);
        PathNavigation pathNavigation = mob.getNavigation();

        for(BlockPos blockPos2 : iterable) {
            double d = blockPos2.distSqr(blockPos);
            if ((i != blockPos2.getX() || k != blockPos2.getZ()) && pathNavigation.isStableDestination(blockPos2) && mob.getPathfindingMalus(WalkNodeEvaluator.getBlockPathTypeStatic(mob.level, blockPos2.mutable())) == 0.0F) {
                Optional<Vec3> optional = this.calculateOptimalJumpVector(mob, Vec3.atCenterOf(blockPos2));
                optional.ifPresent((vel) -> {
                    this.jumpCandidates.add(new LongJumpToRandomPos.PossibleJump(new BlockPos(blockPos2), vel, Mth.ceil(d)));
                });
            }
        }

    }

    @Override
    protected void tick(ServerLevel serverLevel, E mob, long l) {
        if (this.chosenJump.isPresent()) {
            if (l - this.prepareJumpStart >= 40L) {
                mob.setYRot(mob.yBodyRot);
                mob.setDiscardFriction(true);
                Vec3 vec3 = this.chosenJump.get().getJumpVector();
                double d = vec3.length();
                double e = d + mob.getJumpBoostPower();
                mob.setDeltaMovement(vec3.scale(e / d));
                mob.getBrain().setMemory(MemoryModuleType.LONG_JUMP_MID_JUMP, true);
                serverLevel.playSound((Player)null, mob, this.getJumpSound.apply(mob), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        } else {
            --this.findJumpTries;
            Optional<LongJumpToRandomPos.PossibleJump> optional = WeightedRandom.getRandomItem(serverLevel.random, this.jumpCandidates);
            if (optional.isPresent()) {
                this.jumpCandidates.remove(optional.get());
                mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(optional.get().getJumpTarget()));
                PathNavigation pathNavigation = mob.getNavigation();
                Path path = pathNavigation.createPath(optional.get().getJumpTarget(), 0, 8);
                if (path == null || !path.canReach()) {
                    this.chosenJump = optional;
                    this.prepareJumpStart = l;
                }
            }
        }

    }

    private Optional<Vec3> calculateOptimalJumpVector(Mob entity, Vec3 pos) {
        Optional<Vec3> optional = Optional.empty();

        for(int i = 65; i < 85; i += 5) {
            Optional<Vec3> optional2 = this.calculateJumpVectorForAngle(entity, pos, i);
            if (!optional.isPresent() || optional2.isPresent() && optional2.get().lengthSqr() < optional.get().lengthSqr()) {
                optional = optional2;
            }
        }

        return optional;
    }

    private Optional<Vec3> calculateJumpVectorForAngle(Mob entity, Vec3 pos, int range) {
        Vec3 vec3 = entity.position();
        Vec3 vec32 = (new Vec3(pos.x - vec3.x, 0.0D, pos.z - vec3.z)).normalize().scale(0.5D);
        pos = pos.subtract(vec32);
        Vec3 vec33 = pos.subtract(vec3);
        float f = (float)range * (float)Math.PI / 180.0F;
        double d = Math.atan2(vec33.z, vec33.x);
        double e = vec33.subtract(0.0D, vec33.y, 0.0D).lengthSqr();
        double g = Math.sqrt(e);
        double h = vec33.y;
        double i = Math.sin((double)(2.0F * f));
        double j = 0.08D;
        double k = Math.pow(Math.cos((double)f), 2.0D);
        double l = Math.sin((double)f);
        double m = Math.cos((double)f);
        double n = Math.sin(d);
        double o = Math.cos(d);
        double p = e * 0.08D / (g * i - 2.0D * h * k);
        if (p < 0.0D) {
            return Optional.empty();
        } else {
            double q = Math.sqrt(p);
            if (q > (double)this.maxJumpVelocity) {
                return Optional.empty();
            } else {
                double r = q * m;
                double s = q * l;
                int t = Mth.ceil(g / r) * 2;
                double u = 0.0D;
                Vec3 vec34 = null;

                for(int v = 0; v < t - 1; ++v) {
                    u += g / (double)t;
                    double w = l / m * u - Math.pow(u, 2.0D) * 0.08D / (2.0D * p * Math.pow(m, 2.0D));
                    double x = u * o;
                    double y = u * n;
                    Vec3 vec35 = new Vec3(vec3.x + x, vec3.y + w, vec3.z + y);
                    if (vec34 != null && !this.isClearTransition(entity, vec34, vec35)) {
                        return Optional.empty();
                    }

                    vec34 = vec35;
                }

                return Optional.of((new Vec3(r * o, s, r * n)).scale((double)0.95F));
            }
        }
    }

    private boolean isClearTransition(Mob entity, Vec3 startPos, Vec3 endPos) {
        EntityDimensions entityDimensions = entity.getDimensions(Pose.LONG_JUMPING);
        Vec3 vec3 = endPos.subtract(startPos);
        double d = (double)Math.min(entityDimensions.width, entityDimensions.height);
        int i = Mth.ceil(vec3.length() / d);
        Vec3 vec32 = vec3.normalize();
        Vec3 vec33 = startPos;

        for(int j = 0; j < i; ++j) {
            vec33 = j == i - 1 ? endPos : vec33.add(vec32.scale(d * (double)0.9F));
            AABB aABB = entityDimensions.makeBoundingBox(vec33);
            if (!entity.level.noCollision(entity, aABB)) {
                return false;
            }
        }

        return true;
    }

    public static class PossibleJump extends WeightedEntry.IntrusiveBase {
        private final BlockPos jumpTarget;
        private final Vec3 jumpVector;

        public PossibleJump(BlockPos pos, Vec3 ramVelocity, int weight) {
            super(weight);
            this.jumpTarget = pos;
            this.jumpVector = ramVelocity;
        }

        public BlockPos getJumpTarget() {
            return this.jumpTarget;
        }

        public Vec3 getJumpVector() {
            return this.jumpVector;
        }
    }
}
