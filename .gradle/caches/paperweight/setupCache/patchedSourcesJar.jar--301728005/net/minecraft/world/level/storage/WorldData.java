package net.minecraft.world.level.storage;

import com.mojang.serialization.Lifecycle;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;

public interface WorldData {
    int ANVIL_VERSION_ID = 19133;
    int MCREGION_VERSION_ID = 19132;

    DataPackConfig getDataPackConfig();

    void setDataPackConfig(DataPackConfig dataPackSettings);

    boolean wasModded();

    Set<String> getKnownServerBrands();

    void setModdedInfo(String brand, boolean modded);

    default void fillCrashReportCategory(CrashReportCategory section) {
        section.setDetail("Known server brands", () -> {
            return String.join(", ", this.getKnownServerBrands());
        });
        section.setDetail("Level was modded", () -> {
            return Boolean.toString(this.wasModded());
        });
        section.setDetail("Level storage version", () -> {
            int i = this.getVersion();
            return String.format("0x%05X - %s", i, this.getStorageVersionName(i));
        });
    }

    default String getStorageVersionName(int id) {
        switch(id) {
        case 19132:
            return "McRegion";
        case 19133:
            return "Anvil";
        default:
            return "Unknown?";
        }
    }

    @Nullable
    CompoundTag getCustomBossEvents();

    void setCustomBossEvents(@Nullable CompoundTag customBossEvents);

    ServerLevelData overworldData();

    LevelSettings getLevelSettings();

    CompoundTag createTag(RegistryAccess registryManager, @Nullable CompoundTag playerNbt);

    boolean isHardcore();

    int getVersion();

    String getLevelName();

    GameType getGameType();

    void setGameType(GameType gameMode);

    boolean getAllowCommands();

    Difficulty getDifficulty();

    void setDifficulty(Difficulty difficulty);

    boolean isDifficultyLocked();

    void setDifficultyLocked(boolean difficultyLocked);

    GameRules getGameRules();

    @Nullable
    CompoundTag getLoadedPlayerTag();

    CompoundTag endDragonFightData();

    void setEndDragonFightData(CompoundTag dragonFight);

    WorldGenSettings worldGenSettings();

    Lifecycle worldGenSettingsLifecycle();
}
