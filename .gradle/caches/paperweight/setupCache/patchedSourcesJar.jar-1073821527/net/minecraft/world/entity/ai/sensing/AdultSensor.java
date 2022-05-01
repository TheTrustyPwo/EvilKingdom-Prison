package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class AdultSensor extends Sensor<AgeableMob> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }

    @Override
    protected void doTick(ServerLevel world, AgeableMob entity) {
        entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).ifPresent((nearestVisibleLivingEntities) -> {
            this.setNearestVisibleAdult(entity, nearestVisibleLivingEntities);
        });
    }

    private void setNearestVisibleAdult(AgeableMob entity, NearestVisibleLivingEntities nearestVisibleLivingEntities) {
        Optional<AgeableMob> optional = nearestVisibleLivingEntities.findClosest((livingEntity) -> {
            return livingEntity.getType() == entity.getType() && !livingEntity.isBaby();
        }).map(AgeableMob.class::cast);
        entity.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT, optional);
    }
}
