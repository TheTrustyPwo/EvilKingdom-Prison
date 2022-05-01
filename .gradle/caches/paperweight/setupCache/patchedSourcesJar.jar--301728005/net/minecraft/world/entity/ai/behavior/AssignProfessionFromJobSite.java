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

// CraftBukkit start
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
// CraftBukkit end

public class AssignProfessionFromJobSite extends Behavior<Villager> {

    public AssignProfessionFromJobSite() {
        super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT));
    }

    protected boolean checkExtraStartConditions(ServerLevel world, Villager entity) {
        BlockPos blockposition = ((GlobalPos) entity.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get()).pos();

        return blockposition.closerToCenterThan(entity.position(), 2.0D) || entity.assignProfessionWhenSpawned();
    }

    protected void start(ServerLevel world, Villager entity, long time) {
        GlobalPos globalpos = (GlobalPos) entity.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get();

        entity.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        entity.getBrain().setMemory(MemoryModuleType.JOB_SITE, globalpos); // CraftBukkit - decompile error
        world.broadcastEntityEvent(entity, (byte) 14);
        if (entity.getVillagerData().getProfession() == VillagerProfession.NONE) {
            MinecraftServer minecraftserver = world.getServer();

            Optional.ofNullable(minecraftserver.getLevel(globalpos.dimension())).flatMap((worldserver1) -> {
                return worldserver1.getPoiManager().getType(globalpos.pos());
            }).flatMap((villageplacetype) -> {
                return Registry.VILLAGER_PROFESSION.stream().filter((villagerprofession) -> {
                    return villagerprofession.getJobPoiType() == villageplacetype;
                }).findFirst();
            }).ifPresent((villagerprofession) -> {
                // CraftBukkit start - Fire VillagerCareerChangeEvent where Villager gets employed
                VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(entity, CraftVillager.nmsToBukkitProfession(villagerprofession), VillagerCareerChangeEvent.ChangeReason.EMPLOYED);
                if (event.isCancelled()) {
                    return;
                }

                entity.setVillagerData(entity.getVillagerData().setProfession(CraftVillager.bukkitToNmsProfession(event.getProfession())));
                // CraftBukkit end
                entity.refreshBrain(world);
            });
        }
    }
}
