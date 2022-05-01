package net.minecraft.world.level.levelgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public abstract class ScatteredFeaturePiece extends StructurePiece {
    protected final int width;
    protected final int height;
    protected final int depth;
    protected int heightPosition = -1;

    protected ScatteredFeaturePiece(StructurePieceType type, int x, int y, int z, int width, int height, int depth, Direction orientation) {
        super(type, 0, StructurePiece.makeBoundingBox(x, y, z, orientation, width, height, depth));
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.setOrientation(orientation);
    }

    protected ScatteredFeaturePiece(StructurePieceType type, CompoundTag nbt) {
        super(type, nbt);
        this.width = nbt.getInt("Width");
        this.height = nbt.getInt("Height");
        this.depth = nbt.getInt("Depth");
        this.heightPosition = nbt.getInt("HPos");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        nbt.putInt("Width", this.width);
        nbt.putInt("Height", this.height);
        nbt.putInt("Depth", this.depth);
        nbt.putInt("HPos", this.heightPosition);
    }

    protected boolean updateAverageGroundHeight(LevelAccessor world, BoundingBox boundingBox, int deltaY) {
        if (this.heightPosition >= 0) {
            return true;
        } else {
            int i = 0;
            int j = 0;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for(int k = this.boundingBox.minZ(); k <= this.boundingBox.maxZ(); ++k) {
                for(int l = this.boundingBox.minX(); l <= this.boundingBox.maxX(); ++l) {
                    mutableBlockPos.set(l, 64, k);
                    if (boundingBox.isInside(mutableBlockPos)) {
                        i += world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mutableBlockPos).getY();
                        ++j;
                    }
                }
            }

            if (j == 0) {
                return false;
            } else {
                this.heightPosition = i / j;
                this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + deltaY, 0);
                return true;
            }
        }
    }

    protected boolean updateHeightPositionToLowestGroundHeight(LevelAccessor world, int i) {
        if (this.heightPosition >= 0) {
            return true;
        } else {
            int j = world.getMaxBuildHeight();
            boolean bl = false;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for(int k = this.boundingBox.minZ(); k <= this.boundingBox.maxZ(); ++k) {
                for(int l = this.boundingBox.minX(); l <= this.boundingBox.maxX(); ++l) {
                    mutableBlockPos.set(l, 0, k);
                    j = Math.min(j, world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mutableBlockPos).getY());
                    bl = true;
                }
            }

            if (!bl) {
                return false;
            } else {
                this.heightPosition = j;
                this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + i, 0);
                return true;
            }
        }
    }
}
