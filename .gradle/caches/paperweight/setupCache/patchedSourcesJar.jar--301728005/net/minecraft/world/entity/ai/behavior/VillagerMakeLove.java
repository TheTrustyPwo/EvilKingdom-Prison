package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.pathfinder.Path;

public class VillagerMakeLove extends Behavior<Villager> {

    private static final int INTERACT_DIST_SQR = 5;
    private static final float SPEED_MODIFIER = 0.5F;
    private long birthTimestamp;

    public VillagerMakeLove() {
        super(ImmutableMap.of(MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT), 350, 350);
    }

    protected boolean checkExtraStartConditions(ServerLevel world, Villager entity) {
        return this.isBreedingPossible(entity);
    }

    protected boolean canStillUse(ServerLevel worldserver, Villager entityvillager, long i) {
        return i <= this.birthTimestamp && this.isBreedingPossible(entityvillager);
    }

    protected void start(ServerLevel worldserver, Villager entityvillager, long i) {
        AgeableMob entityageable = (AgeableMob) entityvillager.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();

        BehaviorUtils.lockGazeAndWalkToEachOther(entityvillager, entityageable, 0.5F);
        worldserver.broadcastEntityEvent(entityageable, (byte) 18);
        worldserver.broadcastEntityEvent(entityvillager, (byte) 18);
        int j = 275 + entityvillager.getRandom().nextInt(50);

        this.birthTimestamp = i + (long) j;
    }

    protected void tick(ServerLevel worldserver, Villager entityvillager, long i) {
        Villager entityvillager1 = (Villager) entityvillager.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();

        if (entityvillager.distanceToSqr((Entity) entityvillager1) <= 5.0D) {
            BehaviorUtils.lockGazeAndWalkToEachOther(entityvillager, entityvillager1, 0.5F);
            if (i >= this.birthTimestamp) {
                entityvillager.eatAndDigestFood();
                entityvillager1.eatAndDigestFood();
                this.tryToGiveBirth(worldserver, entityvillager, entityvillager1);
            } else if (entityvillager.getRandom().nextInt(35) == 0) {
                worldserver.broadcastEntityEvent(entityvillager1, (byte) 12);
                worldserver.broadcastEntityEvent(entityvillager, (byte) 12);
            }

        }
    }

    private void tryToGiveBirth(ServerLevel world, Villager first, Villager second) {
        Optional<BlockPos> optional = this.takeVacantBed(world, first);

        if (!optional.isPresent()) {
            world.broadcastEntityEvent(second, (byte) 13);
            world.broadcastEntityEvent(first, (byte) 13);
        } else {
            Optional<Villager> optional1 = this.breed(world, first, second);

            if (optional1.isPresent()) {
                this.giveBedToChild(world, (Villager) optional1.get(), (BlockPos) optional.get());
            } else {
                world.getPoiManager().release((BlockPos) optional.get());
                DebugPackets.sendPoiTicketCountPacket(world, (BlockPos) optional.get());
            }
        }

    }

    protected void stop(ServerLevel worldserver, Villager entityvillager, long i) {
        entityvillager.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
    }

    private boolean isBreedingPossible(Villager villager) {
        Brain<Villager> behaviorcontroller = villager.getBrain();
        Optional<AgeableMob> optional = behaviorcontroller.getMemory(MemoryModuleType.BREED_TARGET).filter((entityageable) -> {
            return entityageable.getType() == EntityType.VILLAGER;
        });

        return !optional.isPresent() ? false : BehaviorUtils.targetIsValid(behaviorcontroller, MemoryModuleType.BREED_TARGET, EntityType.VILLAGER) && villager.canBreed() && ((AgeableMob) optional.get()).canBreed();
    }

    private Optional<BlockPos> takeVacantBed(ServerLevel world, Villager villager) {
        return world.getPoiManager().take(PoiType.HOME.getPredicate(), (blockposition) -> {
            return this.canReach(villager, blockposition);
        }, villager.blockPosition(), 48);
    }

    private boolean canReach(Villager villager, BlockPos pos) {
        Path pathentity = villager.getNavigation().createPath(pos, PoiType.HOME.getValidRange());

        return pathentity != null && pathentity.canReach();
    }

    private Optional<Villager> breed(ServerLevel world, Villager parent, Villager partner) {
        Villager entityvillager2 = parent.getBreedOffspring(world, partner);

        if (entityvillager2 == null) {
            return Optional.empty();
        } else {
            // CraftBukkit start - call EntityBreedEvent
            entityvillager2.setAge(-24000);
            entityvillager2.moveTo(parent.getX(), parent.getY(), parent.getZ(), 0.0F, 0.0F);
            if (org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callEntityBreedEvent(entityvillager2, parent, partner, null, null, 0).isCancelled()) {
                return Optional.empty();
            }
            // CraftBukkit end
            parent.setAge(6000);
            partner.setAge(6000);
            world.addFreshEntityWithPassengers(entityvillager2, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.BREEDING); // CraftBukkit - added SpawnReason
            world.broadcastEntityEvent(entityvillager2, (byte) 12);
            return Optional.of(entityvillager2);
        }
    }

    private void giveBedToChild(ServerLevel world, Villager child, BlockPos pos) {
        GlobalPos globalpos = GlobalPos.of(world.dimension(), pos);

        child.getBrain().setMemory(MemoryModuleType.HOME, globalpos); // CraftBukkit - decompile error
    }
}
