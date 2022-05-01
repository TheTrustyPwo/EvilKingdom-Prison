package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class SetEntityLookTarget extends Behavior<LivingEntity> {
    private final Predicate<LivingEntity> predicate;
    private final float maxDistSqr;
    private Optional<LivingEntity> nearestEntityMatchingTest = Optional.empty();

    public SetEntityLookTarget(TagKey<EntityType<?>> entityType, float maxDistance) {
        this((entity) -> {
            return entity.getType().is(entityType);
        }, maxDistance);
    }

    public SetEntityLookTarget(MobCategory group, float maxDistance) {
        this((entity) -> {
            return group.equals(entity.getType().getCategory());
        }, maxDistance);
    }

    public SetEntityLookTarget(EntityType<?> entityType, float maxDistance) {
        this((entity) -> {
            return entityType.equals(entity.getType());
        }, maxDistance);
    }

    public SetEntityLookTarget(float maxDistance) {
        this((entity) -> {
            return true;
        }, maxDistance);
    }

    public SetEntityLookTarget(Predicate<LivingEntity> predicate, float maxDistance) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
        this.predicate = predicate;
        this.maxDistSqr = maxDistance * maxDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        NearestVisibleLivingEntities nearestVisibleLivingEntities = entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get();
        this.nearestEntityMatchingTest = nearestVisibleLivingEntities.findClosest(this.predicate.and((livingEntity2) -> {
            return livingEntity2.distanceToSqr(entity) <= (double)this.maxDistSqr;
        }));
        return this.nearestEntityMatchingTest.isPresent();
    }

    @Override
    protected void start(ServerLevel world, LivingEntity entity, long time) {
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.nearestEntityMatchingTest.get(), true));
        this.nearestEntityMatchingTest = Optional.empty();
    }
}
