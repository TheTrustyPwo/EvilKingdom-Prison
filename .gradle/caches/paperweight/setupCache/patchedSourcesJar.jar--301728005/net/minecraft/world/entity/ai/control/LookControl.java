package net.minecraft.world.entity.ai.control;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class LookControl implements Control {
    protected final Mob mob;
    protected float yMaxRotSpeed;
    protected float xMaxRotAngle;
    protected int lookAtCooldown;
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;

    public LookControl(Mob entity) {
        this.mob = entity;
    }

    public void setLookAt(Vec3 direction) {
        this.setLookAt(direction.x, direction.y, direction.z);
    }

    public void setLookAt(Entity entity) {
        this.setLookAt(entity.getX(), getWantedY(entity), entity.getZ());
    }

    public void setLookAt(Entity entity, float maxYawChange, float maxPitchChange) {
        this.setLookAt(entity.getX(), getWantedY(entity), entity.getZ(), maxYawChange, maxPitchChange);
    }

    public void setLookAt(double x, double y, double z) {
        this.setLookAt(x, y, z, (float)this.mob.getHeadRotSpeed(), (float)this.mob.getMaxHeadXRot());
    }

    public void setLookAt(double x, double y, double z, float maxYawChange, float maxPitchChange) {
        this.wantedX = x;
        this.wantedY = y;
        this.wantedZ = z;
        this.yMaxRotSpeed = maxYawChange;
        this.xMaxRotAngle = maxPitchChange;
        this.lookAtCooldown = 2;
    }

    public void tick() {
        if (this.resetXRotOnTick()) {
            this.mob.setXRot(0.0F);
        }

        if (this.lookAtCooldown > 0) {
            --this.lookAtCooldown;
            this.getYRotD().ifPresent((yaw) -> {
                this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, yaw, this.yMaxRotSpeed);
            });
            this.getXRotD().ifPresent((pitch) -> {
                this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), pitch, this.xMaxRotAngle));
            });
        } else {
            this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, this.mob.yBodyRot, 10.0F);
        }

        this.clampHeadRotationToBody();
    }

    protected void clampHeadRotationToBody() {
        if (!this.mob.getNavigation().isDone()) {
            this.mob.yHeadRot = Mth.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, (float)this.mob.getMaxHeadYRot());
        }

    }

    protected boolean resetXRotOnTick() {
        return true;
    }

    public boolean isLookingAtTarget() {
        return this.lookAtCooldown > 0;
    }

    public double getWantedX() {
        return this.wantedX;
    }

    public double getWantedY() {
        return this.wantedY;
    }

    public double getWantedZ() {
        return this.wantedZ;
    }

    protected Optional<Float> getXRotD() {
        double d = this.wantedX - this.mob.getX();
        double e = this.wantedY - this.mob.getEyeY();
        double f = this.wantedZ - this.mob.getZ();
        double g = Math.sqrt(d * d + f * f);
        return !(Math.abs(e) > (double)1.0E-5F) && !(Math.abs(g) > (double)1.0E-5F) ? Optional.empty() : Optional.of((float)(-(Mth.atan2(e, g) * (double)(180F / (float)Math.PI))));
    }

    protected Optional<Float> getYRotD() {
        double d = this.wantedX - this.mob.getX();
        double e = this.wantedZ - this.mob.getZ();
        return !(Math.abs(e) > (double)1.0E-5F) && !(Math.abs(d) > (double)1.0E-5F) ? Optional.empty() : Optional.of((float)(Mth.atan2(e, d) * (double)(180F / (float)Math.PI)) - 90.0F);
    }

    protected float rotateTowards(float from, float to, float max) {
        float f = Mth.degreesDifference(from, to);
        float g = Mth.clamp(f, -max, max);
        return from + g;
    }

    private static double getWantedY(Entity entity) {
        return entity instanceof LivingEntity ? entity.getEyeY() : (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0D;
    }
}
