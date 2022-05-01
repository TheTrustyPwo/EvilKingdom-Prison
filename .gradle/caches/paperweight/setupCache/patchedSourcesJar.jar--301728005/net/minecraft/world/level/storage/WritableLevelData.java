package net.minecraft.world.level.storage;

import net.minecraft.core.BlockPos;

public interface WritableLevelData extends LevelData {
    void setXSpawn(int spawnX);

    void setYSpawn(int spawnY);

    void setZSpawn(int spawnZ);

    void setSpawnAngle(float spawnAngle);

    default void setSpawn(BlockPos pos, float angle) {
        this.setXSpawn(pos.getX());
        this.setYSpawn(pos.getY());
        this.setZSpawn(pos.getZ());
        this.setSpawnAngle(angle);
    }
}
