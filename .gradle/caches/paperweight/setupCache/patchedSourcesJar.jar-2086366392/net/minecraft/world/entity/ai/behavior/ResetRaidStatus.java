package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ResetRaidStatus extends Behavior<LivingEntity> {
    public ResetRaidStatus() {
        super(ImmutableMap.of());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        return world.random.nextInt(20) == 0;
    }

    @Override
    protected void start(ServerLevel world, LivingEntity entity, long time) {
        Brain<?> brain = entity.getBrain();
        Raid raid = world.getRaidAt(entity.blockPosition());
        if (raid == null || raid.isStopped() || raid.isLoss()) {
            brain.setDefaultActivity(Activity.IDLE);
            brain.updateActivityFromSchedule(world.getDayTime(), world.getGameTime());
        }

    }
}
