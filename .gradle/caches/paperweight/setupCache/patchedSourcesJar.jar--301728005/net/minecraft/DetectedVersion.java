package net.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.bridge.game.PackType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.DataVersion;
import org.slf4j.Logger;

public class DetectedVersion implements WorldVersion {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final WorldVersion BUILT_IN = new DetectedVersion();
    private final String id;
    private final String name;
    private final boolean stable;
    private final DataVersion worldVersion;
    private final int protocolVersion;
    private final int resourcePackVersion;
    private final int dataPackVersion;
    private final Date buildTime;
    private final String releaseTarget;

    private DetectedVersion() {
        this.id = UUID.randomUUID().toString().replaceAll("-", "");
        this.name = "1.18.2";
        this.stable = true;
        this.worldVersion = new DataVersion(2975, "main");
        this.protocolVersion = SharedConstants.getProtocolVersion();
        this.resourcePackVersion = 8;
        this.dataPackVersion = 9;
        this.buildTime = new Date();
        this.releaseTarget = "1.18.2";
    }

    private DetectedVersion(JsonObject json) {
        this.id = GsonHelper.getAsString(json, "id");
        this.name = GsonHelper.getAsString(json, "name");
        this.releaseTarget = GsonHelper.getAsString(json, "release_target");
        this.stable = GsonHelper.getAsBoolean(json, "stable");
        this.worldVersion = new DataVersion(GsonHelper.getAsInt(json, "world_version"), GsonHelper.getAsString(json, "series_id", DataVersion.MAIN_SERIES));
        this.protocolVersion = GsonHelper.getAsInt(json, "protocol_version");
        JsonObject jsonObject = GsonHelper.getAsJsonObject(json, "pack_version");
        this.resourcePackVersion = GsonHelper.getAsInt(jsonObject, "resource");
        this.dataPackVersion = GsonHelper.getAsInt(jsonObject, "data");
        this.buildTime = Date.from(ZonedDateTime.parse(GsonHelper.getAsString(json, "build_time")).toInstant());
    }

    public static WorldVersion tryDetectVersion() {
        try {
            InputStream inputStream = DetectedVersion.class.getResourceAsStream("/version.json");

            WorldVersion var9;
            label63: {
                DetectedVersion var2;
                try {
                    if (inputStream == null) {
                        LOGGER.warn("Missing version information!");
                        var9 = BUILT_IN;
                        break label63;
                    }

                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                    try {
                        var2 = new DetectedVersion(GsonHelper.parse(inputStreamReader));
                    } catch (Throwable var6) {
                        try {
                            inputStreamReader.close();
                        } catch (Throwable var5) {
                            var6.addSuppressed(var5);
                        }

                        throw var6;
                    }

                    inputStreamReader.close();
                } catch (Throwable var7) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable var4) {
                            var7.addSuppressed(var4);
                        }
                    }

                    throw var7;
                }

                if (inputStream != null) {
                    inputStream.close();
                }

                return var2;
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return var9;
        } catch (JsonParseException | IOException var8) {
            throw new IllegalStateException("Game version information is corrupt", var8);
        }
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getReleaseTarget() {
        return this.releaseTarget;
    }

    @Override
    public DataVersion getDataVersion() {
        return this.worldVersion;
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    public int getPackVersion(PackType packType) {
        return packType == PackType.DATA ? this.dataPackVersion : this.resourcePackVersion;
    }

    public Date getBuildTime() {
        return this.buildTime;
    }

    public boolean isStable() {
        return this.stable;
    }
}
