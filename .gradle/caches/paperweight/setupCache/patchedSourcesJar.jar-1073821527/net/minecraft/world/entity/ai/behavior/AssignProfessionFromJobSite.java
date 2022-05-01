package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class AssignProfessionFromJobSite extends Behavior<Villager> {
    public AssignProfessionFromJobSite() {
        super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Villager entity) {
        BlockPos blockPos = entity.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get().pos();
        return blockPos.closerToCenterThan(entity.position(), 2.0D) || entity.assignProfessionWhenSpawned();
    }

    @Override
    protected void start(ServerLevel world, Villager entity, long time) {
        GlobalPos globalPos = entity.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get();
        entity.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        entity.getBrain().setMemory(MemoryModuleType.JOB_SITE, globalPos);
        world.broadcastEntityEvent(entity, (byte)14);
        if (entity.getVillagerData().getProfession() == VillagerProfession.NONE) {
            MinecraftServer minecraftServer = world.getServer();
            Optional.ofNullable(minecraftServer.getLevel(globalPos.dimension())).flatMap((worldx) -> {
                return worldx.getPoiManager().getType(globalPos.pos());
            }).flatMap((poiType) -> {
                return Registry.VILLAGER_PROFESSION.stream().filter((profession) -> {
                    return profession.getJobPoiType() == poiType;
                }).findFirst();
            }).ifPresent((profession) -> {
                entity.setVillagerData(entity.getVillagerData().setProfession(profession));
                entity.refreshBrain(world);
            });
        }
    }
}
