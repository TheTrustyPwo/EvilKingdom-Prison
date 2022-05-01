package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;

public class WorkAtPoi extends Behavior<Villager> {
    private static final int CHECK_COOLDOWN = 300;
    private static final double DISTANCE = 1.73D;
    private long lastCheck;

    public WorkAtPoi() {
        super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverLevel, Villager villager) {
        if (serverLevel.getGameTime() - this.lastCheck < 300L) {
            return false;
        } else if (serverLevel.random.nextInt(2) != 0) {
            return false;
        } else {
            this.lastCheck = serverLevel.getGameTime();
            GlobalPos globalPos = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).get();
            return globalPos.dimension() == serverLevel.dimension() && globalPos.pos().closerToCenterThan(villager.position(), 1.73D);
        }
    }

    @Override
    protected void start(ServerLevel world, Villager entity, long time) {
        Brain<Villager> brain = entity.getBrain();
        brain.setMemory(MemoryModuleType.LAST_WORKED_AT_POI, time);
        brain.getMemory(MemoryModuleType.JOB_SITE).ifPresent((globalPos) -> {
            brain.setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(globalPos.pos()));
        });
        entity.playWorkSound();
        this.useWorkstation(world, entity);
        if (entity.shouldRestock()) {
            entity.restock();
        }

    }

    protected void useWorkstation(ServerLevel world, Villager entity) {
    }

    @Override
    protected boolean canStillUse(ServerLevel world, Villager entity, long time) {
        Optional<GlobalPos> optional = entity.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (!optional.isPresent()) {
            return false;
        } else {
            GlobalPos globalPos = optional.get();
            return globalPos.dimension() == world.dimension() && globalPos.pos().closerToCenterThan(entity.position(), 1.73D);
        }
    }
}
