package net.minecraft.server.level;

import net.minecraft.core.BlockPos;

public class BlockDestructionProgress implements Comparable<BlockDestructionProgress> {
    private final int id;
    private final BlockPos pos;
    private int progress;
    private int updatedRenderTick;

    public BlockDestructionProgress(int breakingEntityId, BlockPos pos) {
        this.id = breakingEntityId;
        this.pos = pos;
    }

    public int getId() {
        return this.id;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public void setProgress(int stage) {
        if (stage > 10) {
            stage = 10;
        }

        this.progress = stage;
    }

    public int getProgress() {
        return this.progress;
    }

    public void updateTick(int lastUpdateTick) {
        this.updatedRenderTick = lastUpdateTick;
    }

    public int getUpdatedRenderTick() {
        return this.updatedRenderTick;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            BlockDestructionProgress blockDestructionProgress = (BlockDestructionProgress)object;
            return this.id == blockDestructionProgress.id;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.id);
    }

    @Override
    public int compareTo(BlockDestructionProgress blockDestructionProgress) {
        return this.progress != blockDestructionProgress.progress ? Integer.compare(this.progress, blockDestructionProgress.progress) : Integer.compare(this.id, blockDestructionProgress.id);
    }
}
