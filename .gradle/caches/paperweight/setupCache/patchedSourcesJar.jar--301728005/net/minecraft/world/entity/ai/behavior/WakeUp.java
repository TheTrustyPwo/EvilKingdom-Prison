package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.schedule.Activity;

public class WakeUp extends Behavior<LivingEntity> {
    public WakeUp() {
        super(ImmutableMap.of());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        return !entity.getBrain().isActive(Activity.REST) && entity.isSleeping();
    }

    @Override
    protected void start(ServerLevel world, LivingEntity entity, long time) {
        entity.stopSleeping();
    }
}
