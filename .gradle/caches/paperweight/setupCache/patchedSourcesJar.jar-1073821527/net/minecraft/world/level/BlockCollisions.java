package net.minecraft.world.level;

import com.google.common.collect.AbstractIterator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockCollisions extends AbstractIterator<VoxelShape> {
    private final AABB box;
    private final CollisionContext context;
    private final Cursor3D cursor;
    private final BlockPos.MutableBlockPos pos;
    private final VoxelShape entityShape;
    private final CollisionGetter collisionGetter;
    private final boolean onlySuffocatingBlocks;
    @Nullable
    private BlockGetter cachedBlockGetter;
    private long cachedBlockGetterPos;

    public BlockCollisions(CollisionGetter world, @Nullable Entity entity, AABB box) {
        this(world, entity, box, false);
    }

    public BlockCollisions(CollisionGetter world, @Nullable Entity entity, AABB box, boolean forEntity) {
        this.context = entity == null ? CollisionContext.empty() : CollisionContext.of(entity);
        this.pos = new BlockPos.MutableBlockPos();
        this.entityShape = Shapes.create(box);
        this.collisionGetter = world;
        this.box = box;
        this.onlySuffocatingBlocks = forEntity;
        int i = Mth.floor(box.minX - 1.0E-7D) - 1;
        int j = Mth.floor(box.maxX + 1.0E-7D) + 1;
        int k = Mth.floor(box.minY - 1.0E-7D) - 1;
        int l = Mth.floor(box.maxY + 1.0E-7D) + 1;
        int m = Mth.floor(box.minZ - 1.0E-7D) - 1;
        int n = Mth.floor(box.maxZ + 1.0E-7D) + 1;
        this.cursor = new Cursor3D(i, k, m, j, l, n);
    }

    @Nullable
    private BlockGetter getChunk(int x, int z) {
        int i = SectionPos.blockToSectionCoord(x);
        int j = SectionPos.blockToSectionCoord(z);
        long l = ChunkPos.asLong(i, j);
        if (this.cachedBlockGetter != null && this.cachedBlockGetterPos == l) {
            return this.cachedBlockGetter;
        } else {
            BlockGetter blockGetter = this.collisionGetter.getChunkForCollisions(i, j);
            this.cachedBlockGetter = blockGetter;
            this.cachedBlockGetterPos = l;
            return blockGetter;
        }
    }

    @Override
    protected VoxelShape computeNext() {
        while(true) {
            if (this.cursor.advance()) {
                int i = this.cursor.nextX();
                int j = this.cursor.nextY();
                int k = this.cursor.nextZ();
                int l = this.cursor.getNextType();
                if (l == 3) {
                    continue;
                }

                BlockGetter blockGetter = this.getChunk(i, k);
                if (blockGetter == null) {
                    continue;
                }

                this.pos.set(i, j, k);
                BlockState blockState = blockGetter.getBlockState(this.pos);
                if (this.onlySuffocatingBlocks && !blockState.isSuffocating(blockGetter, this.pos) || l == 1 && !blockState.hasLargeCollisionShape() || l == 2 && !blockState.is(Blocks.MOVING_PISTON)) {
                    continue;
                }

                VoxelShape voxelShape = blockState.getCollisionShape(this.collisionGetter, this.pos, this.context);
                if (voxelShape == Shapes.block()) {
                    if (!this.box.intersects((double)i, (double)j, (double)k, (double)i + 1.0D, (double)j + 1.0D, (double)k + 1.0D)) {
                        continue;
                    }

                    return voxelShape.move((double)i, (double)j, (double)k);
                }

                VoxelShape voxelShape2 = voxelShape.move((double)i, (double)j, (double)k);
                if (!Shapes.joinIsNotEmpty(voxelShape2, this.entityShape, BooleanOp.AND)) {
                    continue;
                }

                return voxelShape2;
            }

            return this.endOfData();
        }
    }
}
