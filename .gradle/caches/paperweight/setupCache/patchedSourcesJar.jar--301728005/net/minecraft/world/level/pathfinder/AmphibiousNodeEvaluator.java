package net.minecraft.world.level.pathfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;

public class AmphibiousNodeEvaluator extends WalkNodeEvaluator {
    private final boolean prefersShallowSwimming;
    private float oldWalkableCost;
    private float oldWaterBorderCost;

    public AmphibiousNodeEvaluator(boolean penalizeDeepWater) {
        this.prefersShallowSwimming = penalizeDeepWater;
    }

    @Override
    public void prepare(PathNavigationRegion cachedWorld, Mob entity) {
        super.prepare(cachedWorld, entity);
        entity.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.oldWalkableCost = entity.getPathfindingMalus(BlockPathTypes.WALKABLE);
        entity.setPathfindingMalus(BlockPathTypes.WALKABLE, 6.0F);
        this.oldWaterBorderCost = entity.getPathfindingMalus(BlockPathTypes.WATER_BORDER);
        entity.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 4.0F);
    }

    @Override
    public void done() {
        this.mob.setPathfindingMalus(BlockPathTypes.WALKABLE, this.oldWalkableCost);
        this.mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, this.oldWaterBorderCost);
        super.done();
    }

    @Override
    public Node getStart() {
        return this.getNode(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5D), Mth.floor(this.mob.getBoundingBox().minZ));
    }

    @Override
    public Target getGoal(double x, double y, double z) {
        return new Target(this.getNode(Mth.floor(x), Mth.floor(y + 0.5D), Mth.floor(z)));
    }

    @Override
    public int getNeighbors(Node[] successors, Node node) {
        int i = super.getNeighbors(successors, node);
        BlockPathTypes blockPathTypes = this.getCachedBlockType(this.mob, node.x, node.y + 1, node.z);
        BlockPathTypes blockPathTypes2 = this.getCachedBlockType(this.mob, node.x, node.y, node.z);
        int j;
        if (this.mob.getPathfindingMalus(blockPathTypes) >= 0.0F && blockPathTypes2 != BlockPathTypes.STICKY_HONEY) {
            j = Mth.floor(Math.max(1.0F, this.mob.maxUpStep));
        } else {
            j = 0;
        }

        double d = this.getFloorLevel(new BlockPos(node.x, node.y, node.z));
        Node node2 = this.findAcceptedNode(node.x, node.y + 1, node.z, Math.max(0, j - 1), d, Direction.UP, blockPathTypes2);
        Node node3 = this.findAcceptedNode(node.x, node.y - 1, node.z, j, d, Direction.DOWN, blockPathTypes2);
        if (this.isNeighborValid(node2, node)) {
            successors[i++] = node2;
        }

        if (this.isNeighborValid(node3, node) && blockPathTypes2 != BlockPathTypes.TRAPDOOR) {
            successors[i++] = node3;
        }

        for(int l = 0; l < i; ++l) {
            Node node4 = successors[l];
            if (node4.type == BlockPathTypes.WATER && this.prefersShallowSwimming && node4.y < this.mob.level.getSeaLevel() - 10) {
                ++node4.costMalus;
            }
        }

        return i;
    }

    @Override
    protected double getFloorLevel(BlockPos pos) {
        return this.mob.isInWater() ? (double)pos.getY() + 0.5D : super.getFloorLevel(pos);
    }

    @Override
    protected boolean isAmphibious() {
        return true;
    }

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter world, int x, int y, int z) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPathTypes blockPathTypes = getBlockPathTypeRaw(world, mutableBlockPos.set(x, y, z));
        if (blockPathTypes == BlockPathTypes.WATER) {
            for(Direction direction : Direction.values()) {
                BlockPathTypes blockPathTypes2 = getBlockPathTypeRaw(world, mutableBlockPos.set(x, y, z).move(direction));
                if (blockPathTypes2 == BlockPathTypes.BLOCKED) {
                    return BlockPathTypes.WATER_BORDER;
                }
            }

            return BlockPathTypes.WATER;
        } else {
            return getBlockPathTypeStatic(world, mutableBlockPos);
        }
    }
}
