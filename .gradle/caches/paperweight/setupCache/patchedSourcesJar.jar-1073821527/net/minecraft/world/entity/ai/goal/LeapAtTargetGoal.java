package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class LeapAtTargetGoal extends Goal {
    private final Mob mob;
    private LivingEntity target;
    private final float yd;

    public LeapAtTargetGoal(Mob mob, float velocity) {
        this.mob = mob;
        this.yd = velocity;
        this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isVehicle()) {
            return false;
        } else {
            this.target = this.mob.getTarget();
            if (this.target == null) {
                return false;
            } else {
                double d = this.mob.distanceToSqr(this.target);
                if (!(d < 4.0D) && !(d > 16.0D)) {
                    if (!this.mob.isOnGround()) {
                        return false;
                    } else {
                        return this.mob.getRandom().nextInt(reducedTickDelay(5)) == 0;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.isOnGround();
    }

    @Override
    public void start() {
        Vec3 vec3 = this.mob.getDeltaMovement();
        Vec3 vec32 = new Vec3(this.target.getX() - this.mob.getX(), 0.0D, this.target.getZ() - this.mob.getZ());
        if (vec32.lengthSqr() > 1.0E-7D) {
            vec32 = vec32.normalize().scale(0.4D).add(vec3.scale(0.2D));
        }

        this.mob.setDeltaMovement(vec32.x, (double)this.yd, vec32.z);
    }
}
