package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ValidateNearbyPoi extends Behavior<LivingEntity> {
    private static final int MAX_DISTANCE = 16;
    private final MemoryModuleType<GlobalPos> memoryType;
    private final Predicate<PoiType> poiPredicate;

    public ValidateNearbyPoi(PoiType poiType, MemoryModuleType<GlobalPos> memoryModule) {
        super(ImmutableMap.of(memoryModule, MemoryStatus.VALUE_PRESENT));
        this.poiPredicate = poiType.getPredicate();
        this.memoryType = memoryModule;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, LivingEntity entity) {
        GlobalPos globalPos = entity.getBrain().getMemory(this.memoryType).get();
        return world.dimension() == globalPos.dimension() && globalPos.pos().closerToCenterThan(entity.position(), 16.0D);
    }

    @Override
    protected void start(ServerLevel world, LivingEntity entity, long time) {
        Brain<?> brain = entity.getBrain();
        GlobalPos globalPos = brain.getMemory(this.memoryType).get();
        BlockPos blockPos = globalPos.pos();
        ServerLevel serverLevel = world.getServer().getLevel(globalPos.dimension());
        if (serverLevel != null && !this.poiDoesntExist(serverLevel, blockPos)) {
            if (this.bedIsOccupied(serverLevel, blockPos, entity)) {
                brain.eraseMemory(this.memoryType);
                world.getPoiManager().release(blockPos);
                DebugPackets.sendPoiTicketCountPacket(world, blockPos);
            }
        } else {
            brain.eraseMemory(this.memoryType);
        }

    }

    private boolean bedIsOccupied(ServerLevel world, BlockPos pos, LivingEntity entity) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED) && !entity.isSleeping();
    }

    private boolean poiDoesntExist(ServerLevel world, BlockPos pos) {
        return !world.getPoiManager().exists(pos, this.poiPredicate);
    }
}
