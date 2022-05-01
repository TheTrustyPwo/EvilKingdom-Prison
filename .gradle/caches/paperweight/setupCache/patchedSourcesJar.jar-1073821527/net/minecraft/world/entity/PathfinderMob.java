package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public abstract class PathfinderMob extends Mob {
    protected static final float DEFAULT_WALK_TARGET_VALUE = 0.0F;

    protected PathfinderMob(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
    }

    public float getWalkTargetValue(BlockPos pos) {
        return this.getWalkTargetValue(pos, this.level);
    }

    public float getWalkTargetValue(BlockPos pos, LevelReader world) {
        return 0.0F;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, MobSpawnType spawnReason) {
        return this.getWalkTargetValue(this.blockPosition(), world) >= 0.0F;
    }

    public boolean isPathFinding() {
        return !this.getNavigation().isDone();
    }

    @Override
    protected void tickLeash() {
        super.tickLeash();
        Entity entity = this.getLeashHolder();
        if (entity != null && entity.level == this.level) {
            this.restrictTo(entity.blockPosition(), 5);
            float f = this.distanceTo(entity);
            if (this instanceof TamableAnimal && ((TamableAnimal)this).isInSittingPose()) {
                if (f > 10.0F) {
                    this.dropLeash(true, true);
                }

                return;
            }

            this.onLeashDistance(f);
            if (f > 10.0F) {
                this.dropLeash(true, true);
                this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
            } else if (f > 6.0F) {
                double d = (entity.getX() - this.getX()) / (double)f;
                double e = (entity.getY() - this.getY()) / (double)f;
                double g = (entity.getZ() - this.getZ()) / (double)f;
                this.setDeltaMovement(this.getDeltaMovement().add(Math.copySign(d * d * 0.4D, d), Math.copySign(e * e * 0.4D, e), Math.copySign(g * g * 0.4D, g)));
            } else {
                this.goalSelector.enableControlFlag(Goal.Flag.MOVE);
                float h = 2.0F;
                Vec3 vec3 = (new Vec3(entity.getX() - this.getX(), entity.getY() - this.getY(), entity.getZ() - this.getZ())).normalize().scale((double)Math.max(f - 2.0F, 0.0F));
                this.getNavigation().moveTo(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z, this.followLeashSpeed());
            }
        }

    }

    protected double followLeashSpeed() {
        return 1.0D;
    }

    protected void onLeashDistance(float leashLength) {
    }
}
