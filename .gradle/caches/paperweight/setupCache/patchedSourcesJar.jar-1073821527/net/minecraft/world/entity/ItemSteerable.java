package net.minecraft.world.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public interface ItemSteerable {
    boolean boost();

    void travelWithInput(Vec3 movementInput);

    float getSteeringSpeed();

    default boolean travel(Mob entity, ItemBasedSteering saddledEntity, Vec3 movementInput) {
        if (!entity.isAlive()) {
            return false;
        } else {
            Entity entity2 = entity.getFirstPassenger();
            if (entity.isVehicle() && entity.canBeControlledByRider() && entity2 instanceof Player) {
                entity.setYRot(entity2.getYRot());
                entity.yRotO = entity.getYRot();
                entity.setXRot(entity2.getXRot() * 0.5F);
                entity.setRot(entity.getYRot(), entity.getXRot());
                entity.yBodyRot = entity.getYRot();
                entity.yHeadRot = entity.getYRot();
                entity.maxUpStep = 1.0F;
                entity.flyingSpeed = entity.getSpeed() * 0.1F;
                if (saddledEntity.boosting && saddledEntity.boostTime++ > saddledEntity.boostTimeTotal) {
                    saddledEntity.boosting = false;
                }

                if (entity.isControlledByLocalInstance()) {
                    float f = this.getSteeringSpeed();
                    if (saddledEntity.boosting) {
                        f += f * 1.15F * Mth.sin((float)saddledEntity.boostTime / (float)saddledEntity.boostTimeTotal * (float)Math.PI);
                    }

                    entity.setSpeed(f);
                    this.travelWithInput(new Vec3(0.0D, 0.0D, 1.0D));
                    entity.lerpSteps = 0;
                } else {
                    entity.calculateEntityAnimation(entity, false);
                    entity.setDeltaMovement(Vec3.ZERO);
                }

                entity.tryCheckInsideBlocks();
                return true;
            } else {
                entity.maxUpStep = 0.5F;
                entity.flyingSpeed = 0.02F;
                this.travelWithInput(movementInput);
                return false;
            }
        }
    }
}
