package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.ItemEntity;

public class StopAdmiringIfItemTooFarAway<E extends Piglin> extends Behavior<E> {
    private final int maxDistanceToItem;

    public StopAdmiringIfItemTooFarAway(int range) {
        super(ImmutableMap.of(MemoryModuleType.ADMIRING_ITEM, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryStatus.REGISTERED));
        this.maxDistanceToItem = range;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        if (!entity.getOffhandItem().isEmpty()) {
            return false;
        } else {
            Optional<ItemEntity> optional = entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
            if (!optional.isPresent()) {
                return true;
            } else {
                return !optional.get().closerThan(entity, (double)this.maxDistanceToItem);
            }
        }
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        entity.getBrain().eraseMemory(MemoryModuleType.ADMIRING_ITEM);
    }
}
