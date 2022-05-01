package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SerializableUUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.timers.TimerCallbacks;
import net.minecraft.world.level.timers.TimerQueue;
import org.slf4j.Logger;

public class PrimaryLevelData implements ServerLevelData, WorldData {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static final String PLAYER = "Player";
    protected static final String WORLD_GEN_SETTINGS = "WorldGenSettings";
    public LevelSettings settings;
    private final WorldGenSettings worldGenSettings;
    private final Lifecycle worldGenSettingsLifecycle;
    private int xSpawn;
    private int ySpawn;
    private int zSpawn;
    private float spawnAngle;
    private long gameTime;
    private long dayTime;
    @Nullable
    private final DataFixer fixerUpper;
    private final int playerDataVersion;
    private boolean upgradedPlayerTag;
    @Nullable
    private CompoundTag loadedPlayerTag;
    private final int version;
    private int clearWeatherTime;
    private boolean raining;
    private int rainTime;
    private boolean thundering;
    private int thunderTime;
    private boolean initialized;
    private boolean difficultyLocked;
    private WorldBorder.Settings worldBorder;
    private CompoundTag endDragonFightData;
    @Nullable
    private CompoundTag customBossEvents;
    private int wanderingTraderSpawnDelay;
    private int wanderingTraderSpawnChance;
    @Nullable
    private UUID wanderingTraderId;
    private final Set<String> knownServerBrands;
    private boolean wasModded;
    private final TimerQueue<MinecraftServer> scheduledEvents;

    private PrimaryLevelData(@Nullable DataFixer dataFixer, int dataVersion, @Nullable CompoundTag playerData, boolean modded, int spawnX, int spawnY, int spawnZ, float spawnAngle, long time, long timeOfDay, int version, int clearWeatherTime, int rainTime, boolean raining, int thunderTime, boolean thundering, boolean initialized, boolean difficultyLocked, WorldBorder.Settings worldBorder, int wanderingTraderSpawnDelay, int wanderingTraderSpawnChance, @Nullable UUID wanderingTraderId, Set<String> serverBrands, TimerQueue<MinecraftServer> scheduledEvents, @Nullable CompoundTag customBossEvents, CompoundTag dragonFight, LevelSettings levelInfo, WorldGenSettings generatorOptions, Lifecycle lifecycle) {
        this.fixerUpper = dataFixer;
        this.wasModded = modded;
        this.xSpawn = spawnX;
        this.ySpawn = spawnY;
        this.zSpawn = spawnZ;
        this.spawnAngle = spawnAngle;
        this.gameTime = time;
        this.dayTime = timeOfDay;
        this.version = version;
        this.clearWeatherTime = clearWeatherTime;
        this.rainTime = rainTime;
        this.raining = raining;
        this.thunderTime = thunderTime;
        this.thundering = thundering;
        this.initialized = initialized;
        this.difficultyLocked = difficultyLocked;
        this.worldBorder = worldBorder;
        this.wanderingTraderSpawnDelay = wanderingTraderSpawnDelay;
        this.wanderingTraderSpawnChance = wanderingTraderSpawnChance;
        this.wanderingTraderId = wanderingTraderId;
        this.knownServerBrands = serverBrands;
        this.loadedPlayerTag = playerData;
        this.playerDataVersion = dataVersion;
        this.scheduledEvents = scheduledEvents;
        this.customBossEvents = customBossEvents;
        this.endDragonFightData = dragonFight;
        this.settings = levelInfo;
        this.worldGenSettings = generatorOptions;
        this.worldGenSettingsLifecycle = lifecycle;
    }

    public PrimaryLevelData(LevelSettings levelInfo, WorldGenSettings generatorOptions, Lifecycle lifecycle) {
        this((DataFixer)null, SharedConstants.getCurrentVersion().getWorldVersion(), (CompoundTag)null, false, 0, 0, 0, 0.0F, 0L, 0L, 19133, 0, 0, false, 0, false, false, false, WorldBorder.DEFAULT_SETTINGS, 0, 0, (UUID)null, Sets.newLinkedHashSet(), new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS), (CompoundTag)null, new CompoundTag(), levelInfo.copy(), generatorOptions, lifecycle);
    }

    public static PrimaryLevelData parse(Dynamic<Tag> dynamic, DataFixer dataFixer, int dataVersion, @Nullable CompoundTag playerData, LevelSettings levelInfo, LevelVersion saveVersionInfo, WorldGenSettings generatorOptions, Lifecycle lifecycle) {
        long l = dynamic.get("Time").asLong(0L);
        CompoundTag compoundTag = dynamic.get("DragonFight").result().map(Dynamic::getValue).orElseGet(() -> {
            return dynamic.get("DimensionData").get("1").get("DragonFight").orElseEmptyMap().getValue();
        });
        return new PrimaryLevelData(dataFixer, dataVersion, playerData, dynamic.get("WasModded").asBoolean(false), dynamic.get("SpawnX").asInt(0), dynamic.get("SpawnY").asInt(0), dynamic.get("SpawnZ").asInt(0), dynamic.get("SpawnAngle").asFloat(0.0F), l, dynamic.get("DayTime").asLong(l), saveVersionInfo.levelDataVersion(), dynamic.get("clearWeatherTime").asInt(0), dynamic.get("rainTime").asInt(0), dynamic.get("raining").asBoolean(false), dynamic.get("thunderTime").asInt(0), dynamic.get("thundering").asBoolean(false), dynamic.get("initialized").asBoolean(true), dynamic.get("DifficultyLocked").asBoolean(false), WorldBorder.Settings.read(dynamic, WorldBorder.DEFAULT_SETTINGS), dynamic.get("WanderingTraderSpawnDelay").asInt(0), dynamic.get("WanderingTraderSpawnChance").asInt(0), dynamic.get("WanderingTraderId").read(SerializableUUID.CODEC).result().orElse((UUID)null), dynamic.get("ServerBrands").asStream().flatMap((dynamicx) -> {
            return dynamicx.asString().result().stream();
        }).collect(Collectors.toCollection(Sets::newLinkedHashSet)), new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS, dynamic.get("ScheduledEvents").asStream()), (CompoundTag)dynamic.get("CustomBossEvents").orElseEmptyMap().getValue(), compoundTag, levelInfo, generatorOptions, lifecycle);
    }

    @Override
    public CompoundTag createTag(RegistryAccess registryManager, @Nullable CompoundTag playerNbt) {
        this.updatePlayerTag();
        if (playerNbt == null) {
            playerNbt = this.loadedPlayerTag;
        }

        CompoundTag compoundTag = new CompoundTag();
        this.setTagData(registryManager, compoundTag, playerNbt);
        return compoundTag;
    }

    private void setTagData(RegistryAccess registryManager, CompoundTag levelNbt, @Nullable CompoundTag playerNbt) {
        ListTag listTag = new ListTag();
        this.knownServerBrands.stream().map(StringTag::valueOf).forEach(listTag::add);
        levelNbt.put("ServerBrands", listTag);
        levelNbt.putBoolean("WasModded", this.wasModded);
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("Name", SharedConstants.getCurrentVersion().getName());
        compoundTag.putInt("Id", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        compoundTag.putBoolean("Snapshot", !SharedConstants.getCurrentVersion().isStable());
        compoundTag.putString("Series", SharedConstants.getCurrentVersion().getDataVersion().getSeries());
        levelNbt.put("Version", compoundTag);
        levelNbt.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        DynamicOps<Tag> dynamicOps = RegistryOps.create(NbtOps.INSTANCE, registryManager);
        WorldGenSettings.CODEC.encodeStart(dynamicOps, this.worldGenSettings).resultOrPartial(Util.prefix("WorldGenSettings: ", LOGGER::error)).ifPresent((tag) -> {
            levelNbt.put("WorldGenSettings", tag);
        });
        levelNbt.putInt("GameType", this.settings.gameType().getId());
        levelNbt.putInt("SpawnX", this.xSpawn);
        levelNbt.putInt("SpawnY", this.ySpawn);
        levelNbt.putInt("SpawnZ", this.zSpawn);
        levelNbt.putFloat("SpawnAngle", this.spawnAngle);
        levelNbt.putLong("Time", this.gameTime);
        levelNbt.putLong("DayTime", this.dayTime);
        levelNbt.putLong("LastPlayed", Util.getEpochMillis());
        levelNbt.putString("LevelName", this.settings.levelName());
        levelNbt.putInt("version", 19133);
        levelNbt.putInt("clearWeatherTime", this.clearWeatherTime);
        levelNbt.putInt("rainTime", this.rainTime);
        levelNbt.putBoolean("raining", this.raining);
        levelNbt.putInt("thunderTime", this.thunderTime);
        levelNbt.putBoolean("thundering", this.thundering);
        levelNbt.putBoolean("hardcore", this.settings.hardcore());
        levelNbt.putBoolean("allowCommands", this.settings.allowCommands());
        levelNbt.putBoolean("initialized", this.initialized);
        this.worldBorder.write(levelNbt);
        levelNbt.putByte("Difficulty", (byte)this.settings.difficulty().getId());
        levelNbt.putBoolean("DifficultyLocked", this.difficultyLocked);
        levelNbt.put("GameRules", this.settings.gameRules().createTag());
        levelNbt.put("DragonFight", this.endDragonFightData);
        if (playerNbt != null) {
            levelNbt.put("Player", playerNbt);
        }

        DataPackConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.settings.getDataPackConfig()).result().ifPresent((tag) -> {
            levelNbt.put("DataPacks", tag);
        });
        if (this.customBossEvents != null) {
            levelNbt.put("CustomBossEvents", this.customBossEvents);
        }

        levelNbt.put("ScheduledEvents", this.scheduledEvents.store());
        levelNbt.putInt("WanderingTraderSpawnDelay", this.wanderingTraderSpawnDelay);
        levelNbt.putInt("WanderingTraderSpawnChance", this.wanderingTraderSpawnChance);
        if (this.wanderingTraderId != null) {
            levelNbt.putUUID("WanderingTraderId", this.wanderingTraderId);
        }

    }

    @Override
    public int getXSpawn() {
        return this.xSpawn;
    }

    @Override
    public int getYSpawn() {
        return this.ySpawn;
    }

    @Override
    public int getZSpawn() {
        return this.zSpawn;
    }

    @Override
    public float getSpawnAngle() {
        return this.spawnAngle;
    }

    @Override
    public long getGameTime() {
        return this.gameTime;
    }

    @Override
    public long getDayTime() {
        return this.dayTime;
    }

    private void updatePlayerTag() {
        if (!this.upgradedPlayerTag && this.loadedPlayerTag != null) {
            if (this.playerDataVersion < SharedConstants.getCurrentVersion().getWorldVersion()) {
                if (this.fixerUpper == null) {
                    throw (NullPointerException)Util.pauseInIde(new NullPointerException("Fixer Upper not set inside LevelData, and the player tag is not upgraded."));
                }

                this.loadedPlayerTag = NbtUtils.update(this.fixerUpper, DataFixTypes.PLAYER, this.loadedPlayerTag, this.playerDataVersion);
            }

            this.upgradedPlayerTag = true;
        }
    }

    @Override
    public CompoundTag getLoadedPlayerTag() {
        this.updatePlayerTag();
        return this.loadedPlayerTag;
    }

    @Override
    public void setXSpawn(int spawnX) {
        this.xSpawn = spawnX;
    }

    @Override
    public void setYSpawn(int spawnY) {
        this.ySpawn = spawnY;
    }

    @Override
    public void setZSpawn(int spawnZ) {
        this.zSpawn = spawnZ;
    }

    @Override
    public void setSpawnAngle(float spawnAngle) {
        this.spawnAngle = spawnAngle;
    }

    @Override
    public void setGameTime(long time) {
        this.gameTime = time;
    }

    @Override
    public void setDayTime(long timeOfDay) {
        this.dayTime = timeOfDay;
    }

    @Override
    public void setSpawn(BlockPos pos, float angle) {
        this.xSpawn = pos.getX();
        this.ySpawn = pos.getY();
        this.zSpawn = pos.getZ();
        this.spawnAngle = angle;
    }

    @Override
    public String getLevelName() {
        return this.settings.levelName();
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public int getClearWeatherTime() {
        return this.clearWeatherTime;
    }

    @Override
    public void setClearWeatherTime(int clearWeatherTime) {
        this.clearWeatherTime = clearWeatherTime;
    }

    @Override
    public boolean isThundering() {
        return this.thundering;
    }

    @Override
    public void setThundering(boolean thundering) {
        this.thundering = thundering;
    }

    @Override
    public int getThunderTime() {
        return this.thunderTime;
    }

    @Override
    public void setThunderTime(int thunderTime) {
        this.thunderTime = thunderTime;
    }

    @Override
    public boolean isRaining() {
        return this.raining;
    }

    @Override
    public void setRaining(boolean raining) {
        this.raining = raining;
    }

    @Override
    public int getRainTime() {
        return this.rainTime;
    }

    @Override
    public void setRainTime(int rainTime) {
        this.rainTime = rainTime;
    }

    @Override
    public GameType getGameType() {
        return this.settings.gameType();
    }

    @Override
    public void setGameType(GameType gameMode) {
        this.settings = this.settings.withGameType(gameMode);
    }

    @Override
    public boolean isHardcore() {
        return this.settings.hardcore();
    }

    @Override
    public boolean getAllowCommands() {
        return this.settings.allowCommands();
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    @Override
    public GameRules getGameRules() {
        return this.settings.gameRules();
    }

    @Override
    public WorldBorder.Settings getWorldBorder() {
        return this.worldBorder;
    }

    @Override
    public void setWorldBorder(WorldBorder.Settings worldBorder) {
        this.worldBorder = worldBorder;
    }

    @Override
    public Difficulty getDifficulty() {
        return this.settings.difficulty();
    }

    @Override
    public void setDifficulty(Difficulty difficulty) {
        this.settings = this.settings.withDifficulty(difficulty);
    }

    @Override
    public boolean isDifficultyLocked() {
        return this.difficultyLocked;
    }

    @Override
    public void setDifficultyLocked(boolean difficultyLocked) {
        this.difficultyLocked = difficultyLocked;
    }

    @Override
    public TimerQueue<MinecraftServer> getScheduledEvents() {
        return this.scheduledEvents;
    }

    @Override
    public void fillCrashReportCategory(CrashReportCategory reportSection, LevelHeightAccessor world) {
        ServerLevelData.super.fillCrashReportCategory(reportSection, world);
        WorldData.super.fillCrashReportCategory(reportSection);
    }

    @Override
    public WorldGenSettings worldGenSettings() {
        return this.worldGenSettings;
    }

    @Override
    public Lifecycle worldGenSettingsLifecycle() {
        return this.worldGenSettingsLifecycle;
    }

    @Override
    public CompoundTag endDragonFightData() {
        return this.endDragonFightData;
    }

    @Override
    public void setEndDragonFightData(CompoundTag dragonFight) {
        this.endDragonFightData = dragonFight;
    }

    @Override
    public DataPackConfig getDataPackConfig() {
        return this.settings.getDataPackConfig();
    }

    @Override
    public void setDataPackConfig(DataPackConfig dataPackSettings) {
        this.settings = this.settings.withDataPackConfig(dataPackSettings);
    }

    @Nullable
    @Override
    public CompoundTag getCustomBossEvents() {
        return this.customBossEvents;
    }

    @Override
    public void setCustomBossEvents(@Nullable CompoundTag customBossEvents) {
        this.customBossEvents = customBossEvents;
    }

    @Override
    public int getWanderingTraderSpawnDelay() {
        return this.wanderingTraderSpawnDelay;
    }

    @Override
    public void setWanderingTraderSpawnDelay(int wanderingTraderSpawnDelay) {
        this.wanderingTraderSpawnDelay = wanderingTraderSpawnDelay;
    }

    @Override
    public int getWanderingTraderSpawnChance() {
        return this.wanderingTraderSpawnChance;
    }

    @Override
    public void setWanderingTraderSpawnChance(int wanderingTraderSpawnChance) {
        this.wanderingTraderSpawnChance = wanderingTraderSpawnChance;
    }

    @Nullable
    @Override
    public UUID getWanderingTraderId() {
        return this.wanderingTraderId;
    }

    @Override
    public void setWanderingTraderId(UUID wanderingTraderId) {
        this.wanderingTraderId = wanderingTraderId;
    }

    @Override
    public void setModdedInfo(String brand, boolean modded) {
        this.knownServerBrands.add(brand);
        this.wasModded |= modded;
    }

    @Override
    public boolean wasModded() {
        return this.wasModded;
    }

    @Override
    public Set<String> getKnownServerBrands() {
        return ImmutableSet.copyOf(this.knownServerBrands);
    }

    @Override
    public ServerLevelData overworldData() {
        return this;
    }

    @Override
    public LevelSettings getLevelSettings() {
        return this.settings.copy();
    }
}
