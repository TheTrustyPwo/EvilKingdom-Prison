package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;

public class LocateHidingPlaceDuringRaid extends LocateHidingPlace {
    public LocateHidingPlaceDuringRaid(int maxDistance, float walkSpeed) {
        super(maxDistance, walkSpeed, 1);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        Raid raid = world.getRaidAt(entity.blockPosition());
        return super.checkExtraStartConditions(world, entity) && raid != null && raid.isActive() && !raid.isVictory() && !raid.isLoss();
    }
}
