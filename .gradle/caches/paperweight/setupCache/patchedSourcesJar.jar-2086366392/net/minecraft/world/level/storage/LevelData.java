package net.minecraft.world.level.storage;

import net.minecraft.CrashReportCategory;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelHeightAccessor;

public interface LevelData {
    int getXSpawn();

    int getYSpawn();

    int getZSpawn();

    float getSpawnAngle();

    long getGameTime();

    long getDayTime();

    boolean isThundering();

    boolean isRaining();

    void setRaining(boolean raining);

    boolean isHardcore();

    GameRules getGameRules();

    Difficulty getDifficulty();

    boolean isDifficultyLocked();

    default void fillCrashReportCategory(CrashReportCategory reportSection, LevelHeightAccessor world) {
        reportSection.setDetail("Level spawn location", () -> {
            return CrashReportCategory.formatLocation(world, this.getXSpawn(), this.getYSpawn(), this.getZSpawn());
        });
        reportSection.setDetail("Level time", () -> {
            return String.format("%d game time, %d day time", this.getGameTime(), this.getDayTime());
        });
    }
}
