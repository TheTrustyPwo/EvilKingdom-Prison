package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.ItemEntity;

public class GoToWantedItem<E extends LivingEntity> extends Behavior<E> {
    private final Predicate<E> predicate;
    private final int maxDistToWalk;
    private final float speedModifier;

    public GoToWantedItem(float speed, boolean requiresWalkTarget, int radius) {
        this((livingEntity) -> {
            return true;
        }, speed, requiresWalkTarget, radius);
    }

    public GoToWantedItem(Predicate<E> startCondition, float speed, boolean requiresWalkTarget, int radius) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.WALK_TARGET, requiresWalkTarget ? MemoryStatus.REGISTERED : MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryStatus.VALUE_PRESENT));
        this.predicate = startCondition;
        this.maxDistToWalk = radius;
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        return this.predicate.test(entity) && this.getClosestLovedItem(entity).closerThan(entity, (double)this.maxDistToWalk);
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        BehaviorUtils.setWalkAndLookTargetMemories(entity, this.getClosestLovedItem(entity), this.speedModifier, 0);
    }

    private ItemEntity getClosestLovedItem(E entity) {
        return entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM).get();
    }
}
