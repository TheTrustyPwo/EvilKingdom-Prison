package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SmoothSwimmingMoveControl extends MoveControl {
    private final int maxTurnX;
    private final int maxTurnY;
    private final float inWaterSpeedModifier;
    private final float outsideWaterSpeedModifier;
    private final boolean applyGravity;

    public SmoothSwimmingMoveControl(Mob entity, int pitchChange, int yawChange, float speedInWater, float speedInAir, boolean buoyant) {
        super(entity);
        this.maxTurnX = pitchChange;
        this.maxTurnY = yawChange;
        this.inWaterSpeedModifier = speedInWater;
        this.outsideWaterSpeedModifier = speedInAir;
        this.applyGravity = buoyant;
    }

    @Override
    public void tick() {
        if (this.applyGravity && this.mob.isInWater()) {
            this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0D, 0.005D, 0.0D));
        }

        if (this.operation == MoveControl.Operation.MOVE_TO && !this.mob.getNavigation().isDone()) {
            double d = this.wantedX - this.mob.getX();
            double e = this.wantedY - this.mob.getY();
            double f = this.wantedZ - this.mob.getZ();
            double g = d * d + e * e + f * f;
            if (g < (double)2.5000003E-7F) {
                this.mob.setZza(0.0F);
            } else {
                float h = (float)(Mth.atan2(f, d) * (double)(180F / (float)Math.PI)) - 90.0F;
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), h, (float)this.maxTurnY));
                this.mob.yBodyRot = this.mob.getYRot();
                this.mob.yHeadRot = this.mob.getYRot();
                float i = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
                if (this.mob.isInWater()) {
                    this.mob.setSpeed(i * this.inWaterSpeedModifier);
                    double j = Math.sqrt(d * d + f * f);
                    if (Math.abs(e) > (double)1.0E-5F || Math.abs(j) > (double)1.0E-5F) {
                        float k = -((float)(Mth.atan2(e, j) * (double)(180F / (float)Math.PI)));
                        k = Mth.clamp(Mth.wrapDegrees(k), (float)(-this.maxTurnX), (float)this.maxTurnX);
                        this.mob.setXRot(this.rotlerp(this.mob.getXRot(), k, 5.0F));
                    }

                    float l = Mth.cos(this.mob.getXRot() * ((float)Math.PI / 180F));
                    float m = Mth.sin(this.mob.getXRot() * ((float)Math.PI / 180F));
                    this.mob.zza = l * i;
                    this.mob.yya = -m * i;
                } else {
                    this.mob.setSpeed(i * this.outsideWaterSpeedModifier);
                }

            }
        } else {
            this.mob.setSpeed(0.0F);
            this.mob.setXxa(0.0F);
            this.mob.setYya(0.0F);
            this.mob.setZza(0.0F);
        }
    }
}
