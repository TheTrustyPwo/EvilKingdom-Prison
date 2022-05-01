package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

public class StartHuntingHoglin<E extends Piglin> extends Behavior<E> {
    public StartHuntingHoglin() {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ANGRY_AT, MemoryStatus.VALUE_ABSENT, MemoryModuleType.HUNTED_RECENTLY, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Piglin entity) {
        return !entity.isBaby() && !PiglinAi.hasAnyoneNearbyHuntedRecently(entity);
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        Hoglin hoglin = entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN).get();
        PiglinAi.setAngerTarget(entity, hoglin);
        PiglinAi.dontKillAnyMoreHoglinsForAWhile(entity);
        PiglinAi.broadcastAngerTarget(entity, hoglin);
        PiglinAi.broadcastDontKillAnyMoreHoglinsForAWhile(entity);
    }
}
