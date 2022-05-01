package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
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

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Villager entity) {
        return this.isBreedingPossible(entity);
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Villager villager, long l) {
        return l <= this.birthTimestamp && this.isBreedingPossible(villager);
    }

    @Override
    protected void start(ServerLevel serverLevel, Villager villager, long l) {
        AgeableMob ageableMob = villager.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
        BehaviorUtils.lockGazeAndWalkToEachOther(villager, ageableMob, 0.5F);
        serverLevel.broadcastEntityEvent(ageableMob, (byte)18);
        serverLevel.broadcastEntityEvent(villager, (byte)18);
        int i = 275 + villager.getRandom().nextInt(50);
        this.birthTimestamp = l + (long)i;
    }

    @Override
    protected void tick(ServerLevel serverLevel, Villager villager, long l) {
        Villager villager2 = (Villager)villager.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
        if (!(villager.distanceToSqr(villager2) > 5.0D)) {
            BehaviorUtils.lockGazeAndWalkToEachOther(villager, villager2, 0.5F);
            if (l >= this.birthTimestamp) {
                villager.eatAndDigestFood();
                villager2.eatAndDigestFood();
                this.tryToGiveBirth(serverLevel, villager, villager2);
            } else if (villager.getRandom().nextInt(35) == 0) {
                serverLevel.broadcastEntityEvent(villager2, (byte)12);
                serverLevel.broadcastEntityEvent(villager, (byte)12);
            }

        }
    }

    private void tryToGiveBirth(ServerLevel world, Villager first, Villager second) {
        Optional<BlockPos> optional = this.takeVacantBed(world, first);
        if (!optional.isPresent()) {
            world.broadcastEntityEvent(second, (byte)13);
            world.broadcastEntityEvent(first, (byte)13);
        } else {
            Optional<Villager> optional2 = this.breed(world, first, second);
            if (optional2.isPresent()) {
                this.giveBedToChild(world, optional2.get(), optional.get());
            } else {
                world.getPoiManager().release(optional.get());
                DebugPackets.sendPoiTicketCountPacket(world, optional.get());
            }
        }

    }

    @Override
    protected void stop(ServerLevel serverLevel, Villager villager, long l) {
        villager.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
    }

    private boolean isBreedingPossible(Villager villager) {
        Brain<Villager> brain = villager.getBrain();
        Optional<AgeableMob> optional = brain.getMemory(MemoryModuleType.BREED_TARGET).filter((ageableMob) -> {
            return ageableMob.getType() == EntityType.VILLAGER;
        });
        if (!optional.isPresent()) {
            return false;
        } else {
            return BehaviorUtils.targetIsValid(brain, MemoryModuleType.BREED_TARGET, EntityType.VILLAGER) && villager.canBreed() && optional.get().canBreed();
        }
    }

    private Optional<BlockPos> takeVacantBed(ServerLevel world, Villager villager) {
        return world.getPoiManager().take(PoiType.HOME.getPredicate(), (blockPos) -> {
            return this.canReach(villager, blockPos);
        }, villager.blockPosition(), 48);
    }

    private boolean canReach(Villager villager, BlockPos pos) {
        Path path = villager.getNavigation().createPath(pos, PoiType.HOME.getValidRange());
        return path != null && path.canReach();
    }

    private Optional<Villager> breed(ServerLevel world, Villager parent, Villager partner) {
        Villager villager = parent.getBreedOffspring(world, partner);
        if (villager == null) {
            return Optional.empty();
        } else {
            parent.setAge(6000);
            partner.setAge(6000);
            villager.setAge(-24000);
            villager.moveTo(parent.getX(), parent.getY(), parent.getZ(), 0.0F, 0.0F);
            world.addFreshEntityWithPassengers(villager);
            world.broadcastEntityEvent(villager, (byte)12);
            return Optional.of(villager);
        }
    }

    private void giveBedToChild(ServerLevel world, Villager child, BlockPos pos) {
        GlobalPos globalPos = GlobalPos.of(world.dimension(), pos);
        child.getBrain().setMemory(MemoryModuleType.HOME, globalPos);
    }
}
