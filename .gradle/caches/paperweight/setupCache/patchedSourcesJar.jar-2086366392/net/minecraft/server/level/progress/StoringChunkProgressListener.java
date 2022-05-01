package net.minecraft.server.level.progress;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

public class StoringChunkProgressListener implements ChunkProgressListener {
    private final LoggerChunkProgressListener delegate;
    private final Long2ObjectOpenHashMap<ChunkStatus> statuses;
    private ChunkPos spawnPos = new ChunkPos(0, 0);
    private final int fullDiameter;
    private final int radius;
    private final int diameter;
    private boolean started;

    public StoringChunkProgressListener(int radius) {
        this.delegate = new LoggerChunkProgressListener(radius);
        this.fullDiameter = radius * 2 + 1;
        this.radius = radius + ChunkStatus.maxDistance();
        this.diameter = this.radius * 2 + 1;
        this.statuses = new Long2ObjectOpenHashMap<>();
    }

    @Override
    public void updateSpawnPos(ChunkPos spawnPos) {
        if (this.started) {
            this.delegate.updateSpawnPos(spawnPos);
            this.spawnPos = spawnPos;
        }
    }

    @Override
    public void onStatusChange(ChunkPos pos, @Nullable ChunkStatus status) {
        if (this.started) {
            this.delegate.onStatusChange(pos, status);
            if (status == null) {
                this.statuses.remove(pos.toLong());
            } else {
                this.statuses.put(pos.toLong(), status);
            }

        }
    }

    @Override
    public void start() {
        this.started = true;
        this.statuses.clear();
        this.delegate.start();
    }

    @Override
    public void stop() {
        this.started = false;
        this.delegate.stop();
    }

    public int getFullDiameter() {
        return this.fullDiameter;
    }

    public int getDiameter() {
        return this.diameter;
    }

    public int getProgress() {
        return this.delegate.getProgress();
    }

    @Nullable
    public ChunkStatus getStatus(int x, int z) {
        return this.statuses.get(ChunkPos.asLong(x + this.spawnPos.x - this.radius, z + this.spawnPos.z - this.radius));
    }
}
