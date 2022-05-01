package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class GolemSensor extends Sensor<LivingEntity> {
    private static final int GOLEM_SCAN_RATE = 200;
    private static final int MEMORY_TIME_TO_LIVE = 600;

    public GolemSensor() {
        this(200);
    }

    public GolemSensor(int senseInterval) {
        super(senseInterval);
    }

    @Override
    protected void doTick(ServerLevel world, LivingEntity entity) {
        checkForNearbyGolem(entity);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES);
    }

    public static void checkForNearbyGolem(LivingEntity entity) {
        Optional<List<LivingEntity>> optional = entity.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
        if (optional.isPresent()) {
            boolean bl = optional.get().stream().anyMatch((livingEntity) -> {
                return livingEntity.getType().equals(EntityType.IRON_GOLEM);
            });
            if (bl) {
                golemDetected(entity);
            }

        }
    }

    public static void golemDetected(LivingEntity entity) {
        entity.getBrain().setMemoryWithExpiry(MemoryModuleType.GOLEM_DETECTED_RECENTLY, true, 600L);
    }
}
