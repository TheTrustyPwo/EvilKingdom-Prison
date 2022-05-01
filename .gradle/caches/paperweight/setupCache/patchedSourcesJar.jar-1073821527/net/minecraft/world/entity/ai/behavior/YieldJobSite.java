package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.pathfinder.Path;

public class YieldJobSite extends Behavior<Villager> {
    private final float speedModifier;

    public YieldJobSite(float speed) {
        super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT, MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Villager entity) {
        if (entity.isBaby()) {
            return false;
        } else {
            return entity.getVillagerData().getProfession() == VillagerProfession.NONE;
        }
    }

    @Override
    protected void start(ServerLevel world, Villager entity, long time) {
        BlockPos blockPos = entity.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get().pos();
        Optional<PoiType> optional = world.getPoiManager().getType(blockPos);
        if (optional.isPresent()) {
            BehaviorUtils.getNearbyVillagersWithCondition(entity, (villager) -> {
                return this.nearbyWantsJobsite(optional.get(), villager, blockPos);
            }).findFirst().ifPresent((villager2) -> {
                this.yieldJobSite(world, entity, villager2, blockPos, villager2.getBrain().getMemory(MemoryModuleType.JOB_SITE).isPresent());
            });
        }
    }

    private boolean nearbyWantsJobsite(PoiType poiType, Villager villager, BlockPos pos) {
        boolean bl = villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isPresent();
        if (bl) {
            return false;
        } else {
            Optional<GlobalPos> optional = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
            VillagerProfession villagerProfession = villager.getVillagerData().getProfession();
            if (villager.getVillagerData().getProfession() != VillagerProfession.NONE && villagerProfession.getJobPoiType().getPredicate().test(poiType)) {
                return !optional.isPresent() ? this.canReachPos(villager, pos, poiType) : optional.get().pos().equals(pos);
            } else {
                return false;
            }
        }
    }

    private void yieldJobSite(ServerLevel world, Villager previousOwner, Villager newOwner, BlockPos pos, boolean jobSitePresent) {
        this.eraseMemories(previousOwner);
        if (!jobSitePresent) {
            BehaviorUtils.setWalkAndLookTargetMemories(newOwner, pos, this.speedModifier, 1);
            newOwner.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(world.dimension(), pos));
            DebugPackets.sendPoiTicketCountPacket(world, pos);
        }

    }

    private boolean canReachPos(Villager villager, BlockPos pos, PoiType poiType) {
        Path path = villager.getNavigation().createPath(pos, poiType.getValidRange());
        return path != null && path.canReach();
    }

    private void eraseMemories(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
    }
}
