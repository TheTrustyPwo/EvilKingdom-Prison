package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class PoiCompetitorScan extends Behavior<Villager> {
    final VillagerProfession profession;

    public PoiCompetitorScan(VillagerProfession profession) {
        super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
        this.profession = profession;
    }

    @Override
    protected void start(ServerLevel world, Villager entity, long time) {
        GlobalPos globalPos = entity.getBrain().getMemory(MemoryModuleType.JOB_SITE).get();
        world.getPoiManager().getType(globalPos.pos()).ifPresent((poiType) -> {
            BehaviorUtils.getNearbyVillagersWithCondition(entity, (villager) -> {
                return this.competesForSameJobsite(globalPos, poiType, villager);
            }).reduce(entity, PoiCompetitorScan::selectWinner);
        });
    }

    private static Villager selectWinner(Villager first, Villager second) {
        Villager villager;
        Villager villager2;
        if (first.getVillagerXp() > second.getVillagerXp()) {
            villager = first;
            villager2 = second;
        } else {
            villager = second;
            villager2 = first;
        }

        villager2.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        return villager;
    }

    private boolean competesForSameJobsite(GlobalPos pos, PoiType poiType, Villager villager) {
        return this.hasJobSite(villager) && pos.equals(villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).get()) && this.hasMatchingProfession(poiType, villager.getVillagerData().getProfession());
    }

    private boolean hasMatchingProfession(PoiType poiType, VillagerProfession profession) {
        return profession.getJobPoiType().getPredicate().test(poiType);
    }

    private boolean hasJobSite(Villager villager) {
        return villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).isPresent();
    }
}
