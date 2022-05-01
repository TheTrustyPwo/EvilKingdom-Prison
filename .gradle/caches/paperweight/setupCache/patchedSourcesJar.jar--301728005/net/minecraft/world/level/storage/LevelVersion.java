package net.minecraft.world.level.storage;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.SharedConstants;

public class LevelVersion {
    private final int levelDataVersion;
    private final long lastPlayed;
    private final String minecraftVersionName;
    private final DataVersion minecraftVersion;
    private final boolean snapshot;

    private LevelVersion(int levelFormatVersion, long lastPlayed, String versionName, int versionId, String series, boolean stable) {
        this.levelDataVersion = levelFormatVersion;
        this.lastPlayed = lastPlayed;
        this.minecraftVersionName = versionName;
        this.minecraftVersion = new DataVersion(versionId, series);
        this.snapshot = stable;
    }

    public static LevelVersion parse(Dynamic<?> dynamic) {
        int i = dynamic.get("version").asInt(0);
        long l = dynamic.get("LastPlayed").asLong(0L);
        OptionalDynamic<?> optionalDynamic = dynamic.get("Version");
        return optionalDynamic.result().isPresent() ? new LevelVersion(i, l, optionalDynamic.get("Name").asString(SharedConstants.getCurrentVersion().getName()), optionalDynamic.get("Id").asInt(SharedConstants.getCurrentVersion().getDataVersion().getVersion()), optionalDynamic.get("Series").asString(DataVersion.MAIN_SERIES), optionalDynamic.get("Snapshot").asBoolean(!SharedConstants.getCurrentVersion().isStable())) : new LevelVersion(i, l, "", 0, DataVersion.MAIN_SERIES, false);
    }

    public int levelDataVersion() {
        return this.levelDataVersion;
    }

    public long lastPlayed() {
        return this.lastPlayed;
    }

    public String minecraftVersionName() {
        return this.minecraftVersionName;
    }

    public DataVersion minecraftVersion() {
        return this.minecraftVersion;
    }

    public boolean snapshot() {
        return this.snapshot;
    }
}
