package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import java.net.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;

public class GameTestServer extends MinecraftServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PROGRESS_REPORT_INTERVAL = 20;
    private final List<GameTestBatch> testBatches;
    private final BlockPos spawnPos;
    private static final GameRules TEST_GAME_RULES = Util.make(new GameRules(), (gameRules) -> {
        gameRules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, (MinecraftServer)null);
        gameRules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, (MinecraftServer)null);
    });
    private static final LevelSettings TEST_SETTINGS = new LevelSettings("Test Level", GameType.CREATIVE, false, Difficulty.NORMAL, true, TEST_GAME_RULES, DataPackConfig.DEFAULT);
    @Nullable
    private MultipleTestTracker testTracker;

    public static GameTestServer create(Thread thread, LevelStorageSource.LevelStorageAccess session, PackRepository resourcePackManager, Collection<GameTestBatch> batches, BlockPos pos) {
        if (batches.isEmpty()) {
            throw new IllegalArgumentException("No test batches were given!");
        } else {
            WorldStem.InitConfig initConfig = new WorldStem.InitConfig(resourcePackManager, Commands.CommandSelection.DEDICATED, 4, false);

            try {
                WorldStem worldStem = WorldStem.load(initConfig, () -> {
                    return DataPackConfig.DEFAULT;
                }, (resourceManager, dataPackSettings) -> {
                    RegistryAccess.Frozen frozen = RegistryAccess.BUILTIN.get();
                    Registry<Biome> registry = frozen.registryOrThrow(Registry.BIOME_REGISTRY);
                    Registry<StructureSet> registry2 = frozen.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
                    Registry<DimensionType> registry3 = frozen.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
                    WorldData worldData = new PrimaryLevelData(TEST_SETTINGS, new WorldGenSettings(0L, false, false, WorldGenSettings.withOverworld(registry3, DimensionType.defaultDimensions(frozen, 0L), new FlatLevelSource(registry2, FlatLevelGeneratorSettings.getDefault(registry, registry2)))), Lifecycle.stable());
                    return Pair.of(worldData, frozen);
                }, Util.backgroundExecutor(), Runnable::run).get();
                worldStem.updateGlobals();
                return new GameTestServer(thread, session, resourcePackManager, worldStem, batches, pos);
            } catch (Exception var7) {
                LOGGER.warn("Failed to load vanilla datapack, bit oops", (Throwable)var7);
                System.exit(-1);
                throw new IllegalStateException();
            }
        }
    }

    private GameTestServer(Thread serverThread, LevelStorageSource.LevelStorageAccess session, PackRepository dataPackManager, WorldStem saveLoader, Collection<GameTestBatch> batches, BlockPos pos) {
        super(serverThread, session, dataPackManager, saveLoader, Proxy.NO_PROXY, DataFixers.getDataFixer(), (MinecraftSessionService)null, (GameProfileRepository)null, (GameProfileCache)null, LoggerChunkProgressListener::new);
        this.testBatches = Lists.newArrayList(batches);
        this.spawnPos = pos;
    }

    @Override
    public boolean initServer() {
        this.setPlayerList(new PlayerList(this, this.registryAccess(), this.playerDataStorage, 1) {
        });
        this.loadLevel();
        ServerLevel serverLevel = this.overworld();
        serverLevel.setDefaultSpawnPos(this.spawnPos, 0.0F);
        int i = 20000000;
        serverLevel.setWeatherParameters(20000000, 20000000, false, false);
        return true;
    }

    @Override
    public void tickServer(BooleanSupplier shouldKeepTicking) {
        super.tickServer(shouldKeepTicking);
        ServerLevel serverLevel = this.overworld();
        if (!this.haveTestsStarted()) {
            this.startTests(serverLevel);
        }

        if (serverLevel.getGameTime() % 20L == 0L) {
            LOGGER.info(this.testTracker.getProgressBar());
        }

        if (this.testTracker.isDone()) {
            this.halt(false);
            LOGGER.info(this.testTracker.getProgressBar());
            GlobalTestReporter.finish();
            LOGGER.info("========= {} GAME TESTS COMPLETE ======================", (int)this.testTracker.getTotalCount());
            if (this.testTracker.hasFailedRequired()) {
                LOGGER.info("{} required tests failed :(", (int)this.testTracker.getFailedRequiredCount());
                this.testTracker.getFailedRequired().forEach((test) -> {
                    LOGGER.info("   - {}", (Object)test.getTestName());
                });
            } else {
                LOGGER.info("All {} required tests passed :)", (int)this.testTracker.getTotalCount());
            }

            if (this.testTracker.hasFailedOptional()) {
                LOGGER.info("{} optional tests failed", (int)this.testTracker.getFailedOptionalCount());
                this.testTracker.getFailedOptional().forEach((test) -> {
                    LOGGER.info("   - {}", (Object)test.getTestName());
                });
            }

            LOGGER.info("====================================================");
        }

    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport details) {
        details.setDetail("Type", "Game test server");
        return details;
    }

    @Override
    public void onServerExit() {
        super.onServerExit();
        System.exit(this.testTracker.getFailedRequiredCount());
    }

    @Override
    public void onServerCrash(CrashReport report) {
        System.exit(1);
    }

    private void startTests(ServerLevel world) {
        Collection<GameTestInfo> collection = GameTestRunner.runTestBatches(this.testBatches, new BlockPos(0, -60, 0), Rotation.NONE, world, GameTestTicker.SINGLETON, 8);
        this.testTracker = new MultipleTestTracker(collection);
        LOGGER.info("{} tests are now running!", (int)this.testTracker.getTotalCount());
    }

    private boolean haveTestsStarted() {
        return this.testTracker != null;
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public int getOperatorUserPermissionLevel() {
        return 0;
    }

    @Override
    public int getFunctionCompilationLevel() {
        return 4;
    }

    @Override
    public boolean shouldRconBroadcast() {
        return false;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return 0;
    }

    @Override
    public boolean isEpollEnabled() {
        return false;
    }

    @Override
    public boolean isCommandBlockEnabled() {
        return true;
    }

    @Override
    public boolean isPublished() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    @Override
    public boolean isSingleplayerOwner(GameProfile profile) {
        return false;
    }
}
