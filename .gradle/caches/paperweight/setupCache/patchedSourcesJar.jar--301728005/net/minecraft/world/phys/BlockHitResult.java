package net.minecraft.world.phys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class BlockHitResult extends HitResult {
    private final Direction direction;
    private final BlockPos blockPos;
    private final boolean miss;
    private final boolean inside;

    public static BlockHitResult miss(Vec3 pos, Direction side, BlockPos blockPos) {
        return new BlockHitResult(true, pos, side, blockPos, false);
    }

    public BlockHitResult(Vec3 pos, Direction side, BlockPos blockPos, boolean insideBlock) {
        this(false, pos, side, blockPos, insideBlock);
    }

    private BlockHitResult(boolean missed, Vec3 pos, Direction side, BlockPos blockPos, boolean insideBlock) {
        super(pos);
        this.miss = missed;
        this.direction = side;
        this.blockPos = blockPos;
        this.inside = insideBlock;
    }

    public BlockHitResult withDirection(Direction side) {
        return new BlockHitResult(this.miss, this.location, side, this.blockPos, this.inside);
    }

    public BlockHitResult withPosition(BlockPos blockPos) {
        return new BlockHitResult(this.miss, this.location, this.direction, blockPos, this.inside);
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public Direction getDirection() {
        return this.direction;
    }

    @Override
    public HitResult.Type getType() {
        return this.miss ? HitResult.Type.MISS : HitResult.Type.BLOCK;
    }

    public boolean isInside() {
        return this.inside;
    }
}
