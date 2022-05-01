package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

public class VillagerCalmDown extends Behavior<Villager> {
    private static final int SAFE_DISTANCE_FROM_DANGER = 36;

    public VillagerCalmDown() {
        super(ImmutableMap.of());
    }

    @Override
    protected void start(ServerLevel world, Villager entity, long time) {
        boolean bl = VillagerPanicTrigger.isHurt(entity) || VillagerPanicTrigger.hasHostile(entity) || isCloseToEntityThatHurtMe(entity);
        if (!bl) {
            entity.getBrain().eraseMemory(MemoryModuleType.HURT_BY);
            entity.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            entity.getBrain().updateActivityFromSchedule(world.getDayTime(), world.getGameTime());
        }

    }

    private static boolean isCloseToEntityThatHurtMe(Villager entity) {
        return entity.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).filter((livingEntity) -> {
            return livingEntity.distanceToSqr(entity) <= 36.0D;
        }).isPresent();
    }
}
