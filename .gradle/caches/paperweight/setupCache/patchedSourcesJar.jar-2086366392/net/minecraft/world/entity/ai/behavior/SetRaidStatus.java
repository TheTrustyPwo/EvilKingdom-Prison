package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class SetRaidStatus extends Behavior<LivingEntity> {
    public SetRaidStatus() {
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
        if (raid != null) {
            if (raid.hasFirstWaveSpawned() && !raid.isBetweenWaves()) {
                brain.setDefaultActivity(Activity.RAID);
                brain.setActiveActivityIfPossible(Activity.RAID);
            } else {
                brain.setDefaultActivity(Activity.PRE_RAID);
                brain.setActiveActivityIfPossible(Activity.PRE_RAID);
            }
        }

    }
}
