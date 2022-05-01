package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.Items;

public class StopHoldingItemIfNoLongerAdmiring<E extends Piglin> extends Behavior<E> {
    public StopHoldingItemIfNoLongerAdmiring() {
        super(ImmutableMap.of(MemoryModuleType.ADMIRING_ITEM, MemoryStatus.VALUE_ABSENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        return !entity.getOffhandItem().isEmpty() && !entity.getOffhandItem().is(Items.SHIELD);
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        PiglinAi.stopHoldingOffHandItem(entity, true);
    }
}
