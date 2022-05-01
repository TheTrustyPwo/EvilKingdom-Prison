package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class SetLookAndInteract extends Behavior<LivingEntity> {
    private final EntityType<?> type;
    private final int interactionRangeSqr;
    private final Predicate<LivingEntity> targetFilter;
    private final Predicate<LivingEntity> selfFilter;

    public SetLookAndInteract(EntityType<?> entityType, int maxDistance, Predicate<LivingEntity> shouldRunPredicate, Predicate<LivingEntity> predicate) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
        this.type = entityType;
        this.interactionRangeSqr = maxDistance * maxDistance;
        this.targetFilter = predicate;
        this.selfFilter = shouldRunPredicate;
    }

    public SetLookAndInteract(EntityType<?> entityType, int maxDistance) {
        this(entityType, maxDistance, (livingEntity) -> {
            return true;
        }, (livingEntity) -> {
            return true;
        });
    }

    @Override
    public boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        return this.selfFilter.test(entity) && this.getVisibleEntities(entity).contains(this::isMatchingTarget);
    }

    @Override
    public void start(ServerLevel world, LivingEntity entity, long time) {
        super.start(world, entity, time);
        Brain<?> brain = entity.getBrain();
        brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).flatMap((nearestVisibleLivingEntities) -> {
            return nearestVisibleLivingEntities.findClosest((livingEntity2) -> {
                return livingEntity2.distanceToSqr(entity) <= (double)this.interactionRangeSqr && this.isMatchingTarget(livingEntity2);
            });
        }).ifPresent((target) -> {
            brain.setMemory(MemoryModuleType.INTERACTION_TARGET, target);
            brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        });
    }

    private boolean isMatchingTarget(LivingEntity entity) {
        return this.type.equals(entity.getType()) && this.targetFilter.test(entity);
    }

    private NearestVisibleLivingEntities getVisibleEntities(LivingEntity entity) {
        return entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get();
    }
}
