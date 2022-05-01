package net.minecraft.world.entity.ai.sensing;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class AxolotlAttackablesSensor extends NearestVisibleLivingEntitySensor {
    public static final float TARGET_DETECTION_DISTANCE = 8.0F;

    @Override
    protected boolean isMatchingEntity(LivingEntity entity, LivingEntity target) {
        return this.isClose(entity, target) && target.isInWaterOrBubble() && (this.isHostileTarget(target) || this.isHuntTarget(entity, target)) && Sensor.isEntityAttackable(entity, target);
    }

    private boolean isHuntTarget(LivingEntity axolotl, LivingEntity target) {
        return !axolotl.getBrain().hasMemoryValue(MemoryModuleType.HAS_HUNTING_COOLDOWN) && target.getType().is(EntityTypeTags.AXOLOTL_HUNT_TARGETS);
    }

    private boolean isHostileTarget(LivingEntity axolotl) {
        return axolotl.getType().is(EntityTypeTags.AXOLOTL_ALWAYS_HOSTILES);
    }

    private boolean isClose(LivingEntity axolotl, LivingEntity target) {
        return target.distanceToSqr(axolotl) <= 64.0D;
    }

    @Override
    protected MemoryModuleType<LivingEntity> getMemory() {
        return MemoryModuleType.NEAREST_ATTACKABLE;
    }
}
