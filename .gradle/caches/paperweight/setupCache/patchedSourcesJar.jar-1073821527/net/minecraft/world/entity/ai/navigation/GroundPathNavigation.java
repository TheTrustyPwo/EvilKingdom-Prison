package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation extends PathNavigation {
    private boolean avoidSun;

    public GroundPathNavigation(Mob mob, Level world) {
        super(mob, world);
    }

    @Override
    protected PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, range);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.mob.isOnGround() || this.isInLiquid() || this.mob.isPassenger();
    }

    @Override
    protected Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), (double)this.getSurfaceY(), this.mob.getZ());
    }

    @Override
    public Path createPath(BlockPos target, int distance) {
        if (this.level.getBlockState(target).isAir()) {
            BlockPos blockPos;
            for(blockPos = target.below(); blockPos.getY() > this.level.getMinBuildHeight() && this.level.getBlockState(blockPos).isAir(); blockPos = blockPos.below()) {
            }

            if (blockPos.getY() > this.level.getMinBuildHeight()) {
                return super.createPath(blockPos.above(), distance);
            }

            while(blockPos.getY() < this.level.getMaxBuildHeight() && this.level.getBlockState(blockPos).isAir()) {
                blockPos = blockPos.above();
            }

            target = blockPos;
        }

        if (!this.level.getBlockState(target).getMaterial().isSolid()) {
            return super.createPath(target, distance);
        } else {
            BlockPos blockPos2;
            for(blockPos2 = target.above(); blockPos2.getY() < this.level.getMaxBuildHeight() && this.level.getBlockState(blockPos2).getMaterial().isSolid(); blockPos2 = blockPos2.above()) {
            }

            return super.createPath(blockPos2, distance);
        }
    }

    @Override
    public Path createPath(Entity entity, int distance) {
        return this.createPath(entity.blockPosition(), distance);
    }

    private int getSurfaceY() {
        if (this.mob.isInWater() && this.canFloat()) {
            int i = this.mob.getBlockY();
            BlockState blockState = this.level.getBlockState(new BlockPos(this.mob.getX(), (double)i, this.mob.getZ()));
            int j = 0;

            while(blockState.is(Blocks.WATER)) {
                ++i;
                blockState = this.level.getBlockState(new BlockPos(this.mob.getX(), (double)i, this.mob.getZ()));
                ++j;
                if (j > 16) {
                    return this.mob.getBlockY();
                }
            }

            return i;
        } else {
            return Mth.floor(this.mob.getY() + 0.5D);
        }
    }

    @Override
    protected void trimPath() {
        super.trimPath();
        if (this.avoidSun) {
            if (this.level.canSeeSky(new BlockPos(this.mob.getX(), this.mob.getY() + 0.5D, this.mob.getZ()))) {
                return;
            }

            for(int i = 0; i < this.path.getNodeCount(); ++i) {
                Node node = this.path.getNode(i);
                if (this.level.canSeeSky(new BlockPos(node.x, node.y, node.z))) {
                    this.path.truncateNodes(i);
                    return;
                }
            }
        }

    }

    protected boolean hasValidPathType(BlockPathTypes pathType) {
        if (pathType == BlockPathTypes.WATER) {
            return false;
        } else if (pathType == BlockPathTypes.LAVA) {
            return false;
        } else {
            return pathType != BlockPathTypes.OPEN;
        }
    }

    public void setCanOpenDoors(boolean canPathThroughDoors) {
        this.nodeEvaluator.setCanOpenDoors(canPathThroughDoors);
    }

    public boolean canPassDoors() {
        return this.nodeEvaluator.canPassDoors();
    }

    public void setCanPassDoors(boolean canEnterOpenDoors) {
        this.nodeEvaluator.setCanPassDoors(canEnterOpenDoors);
    }

    public boolean canOpenDoors() {
        return this.nodeEvaluator.canPassDoors();
    }

    public void setAvoidSun(boolean avoidSunlight) {
        this.avoidSun = avoidSunlight;
    }
}
