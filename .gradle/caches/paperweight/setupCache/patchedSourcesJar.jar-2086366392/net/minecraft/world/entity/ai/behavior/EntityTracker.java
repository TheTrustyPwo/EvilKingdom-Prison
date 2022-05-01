package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.phys.Vec3;

public class EntityTracker implements PositionTracker {
    private final Entity entity;
    private final boolean trackEyeHeight;

    public EntityTracker(Entity entity, boolean useEyeHeight) {
        this.entity = entity;
        this.trackEyeHeight = useEyeHeight;
    }

    @Override
    public Vec3 currentPosition() {
        return this.trackEyeHeight ? this.entity.position().add(0.0D, (double)this.entity.getEyeHeight(), 0.0D) : this.entity.position();
    }

    @Override
    public BlockPos currentBlockPosition() {
        return this.entity.blockPosition();
    }

    @Override
    public boolean isVisibleBy(LivingEntity entity) {
        Entity optional = this.entity;
        if (optional instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)optional;
            if (!livingEntity.isAlive()) {
                return false;
            } else {
                Optional<NearestVisibleLivingEntities> optional = entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
                return optional.isPresent() && optional.get().contains(livingEntity);
            }
        } else {
            return true;
        }
    }

    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public String toString() {
        return "EntityTracker for " + this.entity;
    }
}
