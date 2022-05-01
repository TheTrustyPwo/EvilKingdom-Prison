package net.minecraft.world.level.storage;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.timers.TimerQueue;

public interface ServerLevelData extends WritableLevelData {
    String getLevelName();

    void setThundering(boolean thundering);

    int getRainTime();

    void setRainTime(int rainTime);

    void setThunderTime(int thunderTime);

    int getThunderTime();

    @Override
    default void fillCrashReportCategory(CrashReportCategory reportSection, LevelHeightAccessor world) {
        WritableLevelData.super.fillCrashReportCategory(reportSection, world);
        reportSection.setDetail("Level name", this::getLevelName);
        reportSection.setDetail("Level game mode", () -> {
            return String.format("Game mode: %s (ID %d). Hardcore: %b. Cheats: %b", this.getGameType().getName(), this.getGameType().getId(), this.isHardcore(), this.getAllowCommands());
        });
        reportSection.setDetail("Level weather", () -> {
            return String.format("Rain time: %d (now: %b), thunder time: %d (now: %b)", this.getRainTime(), this.isRaining(), this.getThunderTime(), this.isThundering());
        });
    }

    int getClearWeatherTime();

    void setClearWeatherTime(int clearWeatherTime);

    int getWanderingTraderSpawnDelay();

    void setWanderingTraderSpawnDelay(int wanderingTraderSpawnDelay);

    int getWanderingTraderSpawnChance();

    void setWanderingTraderSpawnChance(int wanderingTraderSpawnChance);

    @Nullable
    UUID getWanderingTraderId();

    void setWanderingTraderId(UUID wanderingTraderId);

    GameType getGameType();

    void setWorldBorder(WorldBorder.Settings worldBorder);

    WorldBorder.Settings getWorldBorder();

    boolean isInitialized();

    void setInitialized(boolean initialized);

    boolean getAllowCommands();

    void setGameType(GameType gameMode);

    TimerQueue<MinecraftServer> getScheduledEvents();

    void setGameTime(long time);

    void setDayTime(long timeOfDay);
}
