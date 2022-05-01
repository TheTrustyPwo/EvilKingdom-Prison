package net.minecraft.world.level;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CommonLevelAccessor extends EntityGetter, LevelReader, LevelSimulatedRW {
    @Override
    default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        return LevelReader.super.getBlockEntity(pos, type);
    }

    @Override
    default List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB box) {
        return EntityGetter.super.getEntityCollisions(entity, box);
    }

    @Override
    default boolean isUnobstructed(@Nullable Entity except, VoxelShape shape) {
        return EntityGetter.super.isUnobstructed(except, shape);
    }

    @Override
    default BlockPos getHeightmapPos(Heightmap.Types heightmap, BlockPos pos) {
        return LevelReader.super.getHeightmapPos(heightmap, pos);
    }

    RegistryAccess registryAccess();
}
