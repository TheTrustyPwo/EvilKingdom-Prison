package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SwimNodeEvaluator extends NodeEvaluator {
    private final boolean allowBreaching;
    private final Long2ObjectMap<BlockPathTypes> pathTypesByPosCache = new Long2ObjectOpenHashMap<>();

    public SwimNodeEvaluator(boolean canJumpOutOfWater) {
        this.allowBreaching = canJumpOutOfWater;
    }

    @Override
    public void prepare(PathNavigationRegion cachedWorld, Mob entity) {
        super.prepare(cachedWorld, entity);
        this.pathTypesByPosCache.clear();
    }

    @Override
    public void done() {
        super.done();
        this.pathTypesByPosCache.clear();
    }

    @Override
    public Node getStart() {
        return super.getNode(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5D), Mth.floor(this.mob.getBoundingBox().minZ));
    }

    @Override
    public Target getGoal(double x, double y, double z) {
        return new Target(super.getNode(Mth.floor(x), Mth.floor(y), Mth.floor(z)));
    }

    @Override
    public int getNeighbors(Node[] successors, Node node) {
        int i = 0;
        Map<Direction, Node> map = Maps.newEnumMap(Direction.class);

        for(Direction direction : Direction.values()) {
            Node node2 = this.getNode(node.x + direction.getStepX(), node.y + direction.getStepY(), node.z + direction.getStepZ());
            map.put(direction, node2);
            if (this.isNodeValid(node2)) {
                successors[i++] = node2;
            }
        }

        for(Direction direction2 : Direction.Plane.HORIZONTAL) {
            Direction direction3 = direction2.getClockWise();
            Node node3 = this.getNode(node.x + direction2.getStepX() + direction3.getStepX(), node.y, node.z + direction2.getStepZ() + direction3.getStepZ());
            if (this.isDiagonalNodeValid(node3, map.get(direction2), map.get(direction3))) {
                successors[i++] = node3;
            }
        }

        return i;
    }

    protected boolean isNodeValid(@Nullable Node node) {
        return node != null && !node.closed;
    }

    protected boolean isDiagonalNodeValid(@Nullable Node node, @Nullable Node node2, @Nullable Node node3) {
        return this.isNodeValid(node) && node2 != null && node2.costMalus >= 0.0F && node3 != null && node3.costMalus >= 0.0F;
    }

    @Nullable
    @Override
    protected Node getNode(int x, int y, int z) {
        Node node = null;
        BlockPathTypes blockPathTypes = this.getCachedBlockType(x, y, z);
        if (this.allowBreaching && blockPathTypes == BlockPathTypes.BREACH || blockPathTypes == BlockPathTypes.WATER) {
            float f = this.mob.getPathfindingMalus(blockPathTypes);
            if (f >= 0.0F) {
                node = super.getNode(x, y, z);
                node.type = blockPathTypes;
                node.costMalus = Math.max(node.costMalus, f);
                if (this.level.getFluidState(new BlockPos(x, y, z)).isEmpty()) {
                    node.costMalus += 8.0F;
                }
            }
        }

        return node;
    }

    protected BlockPathTypes getCachedBlockType(int i, int j, int k) {
        return this.pathTypesByPosCache.computeIfAbsent(BlockPos.asLong(i, j, k), (l) -> {
            return this.getBlockPathType(this.level, i, j, k);
        });
    }

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter world, int x, int y, int z) {
        return this.getBlockPathType(world, x, y, z, this.mob, this.entityWidth, this.entityHeight, this.entityDepth, this.canOpenDoors(), this.canPassDoors());
    }

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter world, int x, int y, int z, Mob mob, int sizeX, int sizeY, int sizeZ, boolean canOpenDoors, boolean canEnterOpenDoors) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(int i = x; i < x + sizeX; ++i) {
            for(int j = y; j < y + sizeY; ++j) {
                for(int k = z; k < z + sizeZ; ++k) {
                    FluidState fluidState = world.getFluidState(mutableBlockPos.set(i, j, k));
                    BlockState blockState = world.getBlockState(mutableBlockPos.set(i, j, k));
                    if (fluidState.isEmpty() && blockState.isPathfindable(world, mutableBlockPos.below(), PathComputationType.WATER) && blockState.isAir()) {
                        return BlockPathTypes.BREACH;
                    }

                    if (!fluidState.is(FluidTags.WATER)) {
                        return BlockPathTypes.BLOCKED;
                    }
                }
            }
        }

        BlockState blockState2 = world.getBlockState(mutableBlockPos);
        return blockState2.isPathfindable(world, mutableBlockPos, PathComputationType.WATER) ? BlockPathTypes.WATER : BlockPathTypes.BLOCKED;
    }
}
