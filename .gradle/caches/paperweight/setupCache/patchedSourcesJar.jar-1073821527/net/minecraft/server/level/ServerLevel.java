package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddVibrationSignalPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListenerRegistrar;
import net.minecraft.world.level.gameevent.vibrations.VibrationPath;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.level.saveddata.maps.MapIndex;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelTicks;
import org.slf4j.Logger;

public class ServerLevel extends Level implements WorldGenLevel {
    public static final BlockPos END_SPAWN_POINT = new BlockPos(100, 50, 0);
    private static final int MIN_RAIN_DELAY_TIME = 12000;
    private static final int MAX_RAIN_DELAY_TIME = 180000;
    private static final int MIN_RAIN_TIME = 12000;
    private static final int MAX_RAIN_TIME = 24000;
    private static final int MIN_THUNDER_DELAY_TIME = 12000;
    private static final int MAX_THUNDER_DELAY_TIME = 180000;
    private static final int MIN_THUNDER_TIME = 3600;
    private static final int MAX_THUNDER_TIME = 15600;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int EMPTY_TIME_NO_TICK = 300;
    private static final int MAX_SCHEDULED_TICKS_PER_TICK = 65536;
    public final List<ServerPlayer> players = Lists.newArrayList();
    public final ServerChunkCache chunkSource;
    private final MinecraftServer server;
    public final ServerLevelData serverLevelData;
    final EntityTickList entityTickList = new EntityTickList();
    public final PersistentEntitySectionManager<Entity> entityManager;
    public boolean noSave;
    private final SleepStatus sleepStatus;
    private int emptyTime;
    private final PortalForcer portalForcer;
    private final LevelTicks<Block> blockTicks = new LevelTicks<>(this::isPositionTickingWithEntitiesLoaded, this.getProfilerSupplier());
    private final LevelTicks<Fluid> fluidTicks = new LevelTicks<>(this::isPositionTickingWithEntitiesLoaded, this.getProfilerSupplier());
    final Set<Mob> navigatingMobs = new ObjectOpenHashSet<>();
    volatile boolean isUpdatingNavigations;
    protected final Raids raids;
    private final ObjectLinkedOpenHashSet<BlockEventData> blockEvents = new ObjectLinkedOpenHashSet<>();
    private final List<BlockEventData> blockEventsToReschedule = new ArrayList<>(64);
    private boolean handlingTick;
    private final List<CustomSpawner> customSpawners;
    @Nullable
    private final EndDragonFight dragonFight;
    final Int2ObjectMap<EnderDragonPart> dragonParts = new Int2ObjectOpenHashMap<>();
    private final StructureFeatureManager structureFeatureManager;
    private final StructureCheck structureCheck;
    private final boolean tickTime;

    public ServerLevel(MinecraftServer server, Executor workerExecutor, LevelStorageSource.LevelStorageAccess session, ServerLevelData properties, ResourceKey<Level> worldKey, Holder<DimensionType> holder, ChunkProgressListener worldGenerationProgressListener, ChunkGenerator chunkGenerator, boolean debugWorld, long seed, List<CustomSpawner> spawners, boolean shouldTickTime) {
        super(properties, worldKey, holder, server::getProfiler, false, debugWorld, seed);
        this.tickTime = shouldTickTime;
        this.server = server;
        this.customSpawners = spawners;
        this.serverLevelData = properties;
        chunkGenerator.ensureStructuresGenerated();
        boolean bl = server.forceSynchronousWrites();
        DataFixer dataFixer = server.getFixerUpper();
        EntityPersistentStorage<Entity> entityPersistentStorage = new EntityStorage(this, session.getDimensionPath(worldKey).resolve("entities"), dataFixer, bl, server);
        this.entityManager = new PersistentEntitySectionManager<>(Entity.class, new ServerLevel.EntityCallbacks(), entityPersistentStorage);
        this.chunkSource = new ServerChunkCache(this, session, dataFixer, server.getStructureManager(), workerExecutor, chunkGenerator, server.getPlayerList().getViewDistance(), server.getPlayerList().getSimulationDistance(), bl, worldGenerationProgressListener, this.entityManager::updateChunkStatus, () -> {
            return server.overworld().getDataStorage();
        });
        this.portalForcer = new PortalForcer(this);
        this.updateSkyBrightness();
        this.prepareWeather();
        this.getWorldBorder().setAbsoluteMaxSize(server.getAbsoluteMaxWorldSize());
        this.raids = this.getDataStorage().computeIfAbsent((nbt) -> {
            return Raids.load(this, nbt);
        }, () -> {
            return new Raids(this);
        }, Raids.getFileId(this.dimensionTypeRegistration()));
        if (!server.isSingleplayer()) {
            properties.setGameType(server.getDefaultGameType());
        }

        long l = server.getWorldData().worldGenSettings().seed();
        this.structureCheck = new StructureCheck(this.chunkSource.chunkScanner(), this.registryAccess(), server.getStructureManager(), worldKey, chunkGenerator, this, chunkGenerator.getBiomeSource(), l, dataFixer);
        this.structureFeatureManager = new StructureFeatureManager(this, server.getWorldData().worldGenSettings(), this.structureCheck);
        if (this.dimensionType().createDragonFight()) {
            this.dragonFight = new EndDragonFight(this, l, server.getWorldData().endDragonFightData());
        } else {
            this.dragonFight = null;
        }

        this.sleepStatus = new SleepStatus();
    }

    public void setWeatherParameters(int clearDuration, int rainDuration, boolean raining, boolean thundering) {
        this.serverLevelData.setClearWeatherTime(clearDuration);
        this.serverLevelData.setRainTime(rainDuration);
        this.serverLevelData.setThunderTime(rainDuration);
        this.serverLevelData.setRaining(raining);
        this.serverLevelData.setThundering(thundering);
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return this.getChunkSource().getGenerator().getNoiseBiome(biomeX, biomeY, biomeZ);
    }

    public StructureFeatureManager structureFeatureManager() {
        return this.structureFeatureManager;
    }

    public void tick(BooleanSupplier shouldKeepTicking) {
        ProfilerFiller profilerFiller = this.getProfiler();
        this.handlingTick = true;
        profilerFiller.push("world border");
        this.getWorldBorder().tick();
        profilerFiller.popPush("weather");
        this.advanceWeatherCycle();
        int i = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        if (this.sleepStatus.areEnoughSleeping(i) && this.sleepStatus.areEnoughDeepSleeping(i, this.players)) {
            if (this.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
                long l = this.levelData.getDayTime() + 24000L;
                this.setDayTime(l - l % 24000L);
            }

            this.wakeUpAllPlayers();
            if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE) && this.isRaining()) {
                this.resetWeatherCycle();
            }
        }

        this.updateSkyBrightness();
        this.tickTime();
        profilerFiller.popPush("tickPending");
        if (!this.isDebug()) {
            long m = this.getGameTime();
            profilerFiller.push("blockTicks");
            this.blockTicks.tick(m, 65536, this::tickBlock);
            profilerFiller.popPush("fluidTicks");
            this.fluidTicks.tick(m, 65536, this::tickFluid);
            profilerFiller.pop();
        }

        profilerFiller.popPush("raid");
        this.raids.tick();
        profilerFiller.popPush("chunkSource");
        this.getChunkSource().tick(shouldKeepTicking, true);
        profilerFiller.popPush("blockEvents");
        this.runBlockEvents();
        this.handlingTick = false;
        profilerFiller.pop();
        boolean bl = !this.players.isEmpty() || !this.getForcedChunks().isEmpty();
        if (bl) {
            this.resetEmptyTime();
        }

        if (bl || this.emptyTime++ < 300) {
            profilerFiller.push("entities");
            if (this.dragonFight != null) {
                profilerFiller.push("dragonFight");
                this.dragonFight.tick();
                profilerFiller.pop();
            }

            this.entityTickList.forEach((entity) -> {
                if (!entity.isRemoved()) {
                    if (this.shouldDiscardEntity(entity)) {
                        entity.discard();
                    } else {
                        profilerFiller.push("checkDespawn");
                        entity.checkDespawn();
                        profilerFiller.pop();
                        if (this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(entity.chunkPosition().toLong())) {
                            Entity entity2 = entity.getVehicle();
                            if (entity2 != null) {
                                if (!entity2.isRemoved() && entity2.hasPassenger(entity)) {
                                    return;
                                }

                                entity.stopRiding();
                            }

                            profilerFiller.push("tick");
                            this.guardEntityTick(this::tickNonPassenger, entity);
                            profilerFiller.pop();
                        }
                    }
                }
            });
            profilerFiller.pop();
            this.tickBlockEntities();
        }

        profilerFiller.push("entityManagement");
        this.entityManager.tick();
        profilerFiller.pop();
    }

    @Override
    public boolean shouldTickBlocksAt(long chunkPos) {
        return this.chunkSource.chunkMap.getDistanceManager().inBlockTickingRange(chunkPos);
    }

    protected void tickTime() {
        if (this.tickTime) {
            long l = this.levelData.getGameTime() + 1L;
            this.serverLevelData.setGameTime(l);
            this.serverLevelData.getScheduledEvents().tick(this.server, l);
            if (this.levelData.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
                this.setDayTime(this.levelData.getDayTime() + 1L);
            }

        }
    }

    public void setDayTime(long timeOfDay) {
        this.serverLevelData.setDayTime(timeOfDay);
    }

    public void tickCustomSpawners(boolean spawnMonsters, boolean spawnAnimals) {
        for(CustomSpawner customSpawner : this.customSpawners) {
            customSpawner.tick(this, spawnMonsters, spawnAnimals);
        }

    }

    private boolean shouldDiscardEntity(Entity entity) {
        if (this.server.isSpawningAnimals() || !(entity instanceof Animal) && !(entity instanceof WaterAnimal)) {
            return !this.server.areNpcsEnabled() && entity instanceof Npc;
        } else {
            return true;
        }
    }

    private void wakeUpAllPlayers() {
        this.sleepStatus.removeAllSleepers();
        this.players.stream().filter(LivingEntity::isSleeping).collect(Collectors.toList()).forEach((player) -> {
            player.stopSleepInBed(false, false);
        });
    }

    public void tickChunk(LevelChunk chunk, int randomTickSpeed) {
        ChunkPos chunkPos = chunk.getPos();
        boolean bl = this.isRaining();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        ProfilerFiller profilerFiller = this.getProfiler();
        profilerFiller.push("thunder");
        if (bl && this.isThundering() && this.random.nextInt(100000) == 0) {
            BlockPos blockPos = this.findLightningTargetAround(this.getBlockRandomPos(i, 0, j, 15));
            if (this.isRainingAt(blockPos)) {
                DifficultyInstance difficultyInstance = this.getCurrentDifficultyAt(blockPos);
                boolean bl2 = this.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) && this.random.nextDouble() < (double)difficultyInstance.getEffectiveDifficulty() * 0.01D && !this.getBlockState(blockPos.below()).is(Blocks.LIGHTNING_ROD);
                if (bl2) {
                    SkeletonHorse skeletonHorse = EntityType.SKELETON_HORSE.create(this);
                    skeletonHorse.setTrap(true);
                    skeletonHorse.setAge(0);
                    skeletonHorse.setPos((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
                    this.addFreshEntity(skeletonHorse);
                }

                LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(this);
                lightningBolt.moveTo(Vec3.atBottomCenterOf(blockPos));
                lightningBolt.setVisualOnly(bl2);
                this.addFreshEntity(lightningBolt);
            }
        }

        profilerFiller.popPush("iceandsnow");
        if (this.random.nextInt(16) == 0) {
            BlockPos blockPos2 = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, this.getBlockRandomPos(i, 0, j, 15));
            BlockPos blockPos3 = blockPos2.below();
            Biome biome = this.getBiome(blockPos2).value();
            if (biome.shouldFreeze(this, blockPos3)) {
                this.setBlockAndUpdate(blockPos3, Blocks.ICE.defaultBlockState());
            }

            if (bl) {
                if (biome.shouldSnow(this, blockPos2)) {
                    this.setBlockAndUpdate(blockPos2, Blocks.SNOW.defaultBlockState());
                }

                BlockState blockState = this.getBlockState(blockPos3);
                Biome.Precipitation precipitation = biome.getPrecipitation();
                if (precipitation == Biome.Precipitation.RAIN && biome.coldEnoughToSnow(blockPos3)) {
                    precipitation = Biome.Precipitation.SNOW;
                }

                blockState.getBlock().handlePrecipitation(blockState, this, blockPos3, precipitation);
            }
        }

        profilerFiller.popPush("tickBlocks");
        if (randomTickSpeed > 0) {
            for(LevelChunkSection levelChunkSection : chunk.getSections()) {
                if (levelChunkSection.isRandomlyTicking()) {
                    int k = levelChunkSection.bottomBlockY();

                    for(int l = 0; l < randomTickSpeed; ++l) {
                        BlockPos blockPos4 = this.getBlockRandomPos(i, k, j, 15);
                        profilerFiller.push("randomTick");
                        BlockState blockState2 = levelChunkSection.getBlockState(blockPos4.getX() - i, blockPos4.getY() - k, blockPos4.getZ() - j);
                        if (blockState2.isRandomlyTicking()) {
                            blockState2.randomTick(this, blockPos4, this.random);
                        }

                        FluidState fluidState = blockState2.getFluidState();
                        if (fluidState.isRandomlyTicking()) {
                            fluidState.randomTick(this, blockPos4, this.random);
                        }

                        profilerFiller.pop();
                    }
                }
            }
        }

        profilerFiller.pop();
    }

    public Optional<BlockPos> findLightningRod(BlockPos pos) {
        Optional<BlockPos> optional = this.getPoiManager().findClosest((poiType) -> {
            return poiType == PoiType.LIGHTNING_ROD;
        }, (posx) -> {
            return posx.getY() == this.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE, posx.getX(), posx.getZ()) - 1;
        }, pos, 128, PoiManager.Occupancy.ANY);
        return optional.map((posx) -> {
            return posx.above(1);
        });
    }

    protected BlockPos findLightningTargetAround(BlockPos pos) {
        BlockPos blockPos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
        Optional<BlockPos> optional = this.findLightningRod(blockPos);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            AABB aABB = (new AABB(blockPos, new BlockPos(blockPos.getX(), this.getMaxBuildHeight(), blockPos.getZ()))).inflate(3.0D);
            List<LivingEntity> list = this.getEntitiesOfClass(LivingEntity.class, aABB, (entity) -> {
                return entity != null && entity.isAlive() && this.canSeeSky(entity.blockPosition());
            });
            if (!list.isEmpty()) {
                return list.get(this.random.nextInt(list.size())).blockPosition();
            } else {
                if (blockPos.getY() == this.getMinBuildHeight() - 1) {
                    blockPos = blockPos.above(2);
                }

                return blockPos;
            }
        }
    }

    public boolean isHandlingTick() {
        return this.handlingTick;
    }

    public boolean canSleepThroughNights() {
        return this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE) <= 100;
    }

    private void announceSleepStatus() {
        if (this.canSleepThroughNights()) {
            if (!this.getServer().isSingleplayer() || this.getServer().isPublished()) {
                int i = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
                Component component;
                if (this.sleepStatus.areEnoughSleeping(i)) {
                    component = new TranslatableComponent("sleep.skipping_night");
                } else {
                    component = new TranslatableComponent("sleep.players_sleeping", this.sleepStatus.amountSleeping(), this.sleepStatus.sleepersNeeded(i));
                }

                for(ServerPlayer serverPlayer : this.players) {
                    serverPlayer.displayClientMessage(component, true);
                }

            }
        }
    }

    public void updateSleepingPlayerList() {
        if (!this.players.isEmpty() && this.sleepStatus.update(this.players)) {
            this.announceSleepStatus();
        }

    }

    @Override
    public ServerScoreboard getScoreboard() {
        return this.server.getScoreboard();
    }

    private void advanceWeatherCycle() {
        boolean bl = this.isRaining();
        if (this.dimensionType().hasSkyLight()) {
            if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
                int i = this.serverLevelData.getClearWeatherTime();
                int j = this.serverLevelData.getThunderTime();
                int k = this.serverLevelData.getRainTime();
                boolean bl2 = this.levelData.isThundering();
                boolean bl3 = this.levelData.isRaining();
                if (i > 0) {
                    --i;
                    j = bl2 ? 0 : 1;
                    k = bl3 ? 0 : 1;
                    bl2 = false;
                    bl3 = false;
                } else {
                    if (j > 0) {
                        --j;
                        if (j == 0) {
                            bl2 = !bl2;
                        }
                    } else if (bl2) {
                        j = Mth.randomBetweenInclusive(this.random, 3600, 15600);
                    } else {
                        j = Mth.randomBetweenInclusive(this.random, 12000, 180000);
                    }

                    if (k > 0) {
                        --k;
                        if (k == 0) {
                            bl3 = !bl3;
                        }
                    } else if (bl3) {
                        k = Mth.randomBetweenInclusive(this.random, 12000, 24000);
                    } else {
                        k = Mth.randomBetweenInclusive(this.random, 12000, 180000);
                    }
                }

                this.serverLevelData.setThunderTime(j);
                this.serverLevelData.setRainTime(k);
                this.serverLevelData.setClearWeatherTime(i);
                this.serverLevelData.setThundering(bl2);
                this.serverLevelData.setRaining(bl3);
            }

            this.oThunderLevel = this.thunderLevel;
            if (this.levelData.isThundering()) {
                this.thunderLevel += 0.01F;
            } else {
                this.thunderLevel -= 0.01F;
            }

            this.thunderLevel = Mth.clamp(this.thunderLevel, 0.0F, 1.0F);
            this.oRainLevel = this.rainLevel;
            if (this.levelData.isRaining()) {
                this.rainLevel += 0.01F;
            } else {
                this.rainLevel -= 0.01F;
            }

            this.rainLevel = Mth.clamp(this.rainLevel, 0.0F, 1.0F);
        }

        if (this.oRainLevel != this.rainLevel) {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
        }

        if (this.oThunderLevel != this.thunderLevel) {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
        }

        if (bl != this.isRaining()) {
            if (bl) {
                this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F));
            } else {
                this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
            }

            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel));
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel));
        }

    }

    private void resetWeatherCycle() {
        this.serverLevelData.setRainTime(0);
        this.serverLevelData.setRaining(false);
        this.serverLevelData.setThunderTime(0);
        this.serverLevelData.setThundering(false);
    }

    public void resetEmptyTime() {
        this.emptyTime = 0;
    }

    private void tickFluid(BlockPos pos, Fluid fluid) {
        FluidState fluidState = this.getFluidState(pos);
        if (fluidState.is(fluid)) {
            fluidState.tick(this, pos);
        }

    }

    private void tickBlock(BlockPos pos, Block block) {
        BlockState blockState = this.getBlockState(pos);
        if (blockState.is(block)) {
            blockState.tick(this, pos, this.random);
        }

    }

    public void tickNonPassenger(Entity entity) {
        entity.setOldPosAndRot();
        ProfilerFiller profilerFiller = this.getProfiler();
        ++entity.tickCount;
        this.getProfiler().push(() -> {
            return Registry.ENTITY_TYPE.getKey(entity.getType()).toString();
        });
        profilerFiller.incrementCounter("tickNonPassenger");
        entity.tick();
        this.getProfiler().pop();

        for(Entity entity2 : entity.getPassengers()) {
            this.tickPassenger(entity, entity2);
        }

    }

    private void tickPassenger(Entity vehicle, Entity passenger) {
        if (!passenger.isRemoved() && passenger.getVehicle() == vehicle) {
            if (passenger instanceof Player || this.entityTickList.contains(passenger)) {
                passenger.setOldPosAndRot();
                ++passenger.tickCount;
                ProfilerFiller profilerFiller = this.getProfiler();
                profilerFiller.push(() -> {
                    return Registry.ENTITY_TYPE.getKey(passenger.getType()).toString();
                });
                profilerFiller.incrementCounter("tickPassenger");
                passenger.rideTick();
                profilerFiller.pop();

                for(Entity entity : passenger.getPassengers()) {
                    this.tickPassenger(passenger, entity);
                }

            }
        } else {
            passenger.stopRiding();
        }
    }

    @Override
    public boolean mayInteract(Player player, BlockPos pos) {
        return !this.server.isUnderSpawnProtection(this, pos, player) && this.getWorldBorder().isWithinBounds(pos);
    }

    public void save(@Nullable ProgressListener progressListener, boolean flush, boolean savingDisabled) {
        ServerChunkCache serverChunkCache = this.getChunkSource();
        if (!savingDisabled) {
            if (progressListener != null) {
                progressListener.progressStartNoAbort(new TranslatableComponent("menu.savingLevel"));
            }

            this.saveLevelData();
            if (progressListener != null) {
                progressListener.progressStage(new TranslatableComponent("menu.savingChunks"));
            }

            serverChunkCache.save(flush);
            if (flush) {
                this.entityManager.saveAll();
            } else {
                this.entityManager.autoSave();
            }

        }
    }

    private void saveLevelData() {
        if (this.dragonFight != null) {
            this.server.getWorldData().setEndDragonFightData(this.dragonFight.saveData());
        }

        this.getChunkSource().getDataStorage().save();
    }

    public <T extends Entity> List<? extends T> getEntities(EntityTypeTest<Entity, T> filter, Predicate<? super T> predicate) {
        List<T> list = Lists.newArrayList();
        this.getEntities().get(filter, (entity) -> {
            if (predicate.test(entity)) {
                list.add(entity);
            }

        });
        return list;
    }

    public List<? extends EnderDragon> getDragons() {
        return this.getEntities(EntityType.ENDER_DRAGON, LivingEntity::isAlive);
    }

    public List<ServerPlayer> getPlayers(Predicate<? super ServerPlayer> predicate) {
        List<ServerPlayer> list = Lists.newArrayList();

        for(ServerPlayer serverPlayer : this.players) {
            if (predicate.test(serverPlayer)) {
                list.add(serverPlayer);
            }
        }

        return list;
    }

    @Nullable
    public ServerPlayer getRandomPlayer() {
        List<ServerPlayer> list = this.getPlayers(LivingEntity::isAlive);
        return list.isEmpty() ? null : list.get(this.random.nextInt(list.size()));
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        return this.addEntity(entity);
    }

    public boolean addWithUUID(Entity entity) {
        return this.addEntity(entity);
    }

    public void addDuringTeleport(Entity entity) {
        this.addEntity(entity);
    }

    public void addDuringCommandTeleport(ServerPlayer player) {
        this.addPlayer(player);
    }

    public void addDuringPortalTeleport(ServerPlayer player) {
        this.addPlayer(player);
    }

    public void addNewPlayer(ServerPlayer player) {
        this.addPlayer(player);
    }

    public void addRespawnedPlayer(ServerPlayer player) {
        this.addPlayer(player);
    }

    private void addPlayer(ServerPlayer player) {
        Entity entity = this.getEntities().get(player.getUUID());
        if (entity != null) {
            LOGGER.warn("Force-added player with duplicate UUID {}", (Object)player.getUUID().toString());
            entity.unRide();
            this.removePlayerImmediately((ServerPlayer)entity, Entity.RemovalReason.DISCARDED);
        }

        this.entityManager.addNewEntity(player);
    }

    private boolean addEntity(Entity entity) {
        if (entity.isRemoved()) {
            LOGGER.warn("Tried to add entity {} but it was marked as removed already", (Object)EntityType.getKey(entity.getType()));
            return false;
        } else {
            return this.entityManager.addNewEntity(entity);
        }
    }

    public boolean tryAddFreshEntityWithPassengers(Entity entity) {
        if (entity.getSelfAndPassengers().map(Entity::getUUID).anyMatch(this.entityManager::isLoaded)) {
            return false;
        } else {
            this.addFreshEntityWithPassengers(entity);
            return true;
        }
    }

    public void unload(LevelChunk chunk) {
        chunk.clearAllBlockEntities();
        chunk.unregisterTickContainerFromLevel(this);
    }

    public void removePlayerImmediately(ServerPlayer player, Entity.RemovalReason reason) {
        player.remove(reason);
    }

    @Override
    public void destroyBlockProgress(int entityId, BlockPos pos, int progress) {
        for(ServerPlayer serverPlayer : this.server.getPlayerList().getPlayers()) {
            if (serverPlayer != null && serverPlayer.level == this && serverPlayer.getId() != entityId) {
                double d = (double)pos.getX() - serverPlayer.getX();
                double e = (double)pos.getY() - serverPlayer.getY();
                double f = (double)pos.getZ() - serverPlayer.getZ();
                if (d * d + e * e + f * f < 1024.0D) {
                    serverPlayer.connection.send(new ClientboundBlockDestructionPacket(entityId, pos, progress));
                }
            }
        }

    }

    @Override
    public void playSound(@Nullable Player except, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch) {
        this.server.getPlayerList().broadcast(except, x, y, z, volume > 1.0F ? (double)(16.0F * volume) : 16.0D, this.dimension(), new ClientboundSoundPacket(sound, category, x, y, z, volume, pitch));
    }

    @Override
    public void playSound(@Nullable Player except, Entity entity, SoundEvent sound, SoundSource category, float volume, float pitch) {
        this.server.getPlayerList().broadcast(except, entity.getX(), entity.getY(), entity.getZ(), volume > 1.0F ? (double)(16.0F * volume) : 16.0D, this.dimension(), new ClientboundSoundEntityPacket(sound, category, entity, volume, pitch));
    }

    @Override
    public void globalLevelEvent(int eventId, BlockPos pos, int data) {
        this.server.getPlayerList().broadcastAll(new ClientboundLevelEventPacket(eventId, pos, data, true));
    }

    @Override
    public void levelEvent(@Nullable Player player, int eventId, BlockPos pos, int data) {
        this.server.getPlayerList().broadcast(player, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), 64.0D, this.dimension(), new ClientboundLevelEventPacket(eventId, pos, data, false));
    }

    public int getLogicalHeight() {
        return this.dimensionType().logicalHeight();
    }

    @Override
    public void gameEvent(@Nullable Entity entity, GameEvent event, BlockPos pos) {
        this.postGameEventInRadius(entity, event, pos, event.getNotificationRadius());
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
        if (this.isUpdatingNavigations) {
            String string = "recursive call to sendBlockUpdated";
            Util.logAndPauseIfInIde("recursive call to sendBlockUpdated", new IllegalStateException("recursive call to sendBlockUpdated"));
        }

        this.getChunkSource().blockChanged(pos);
        VoxelShape voxelShape = oldState.getCollisionShape(this, pos);
        VoxelShape voxelShape2 = newState.getCollisionShape(this, pos);
        if (Shapes.joinIsNotEmpty(voxelShape, voxelShape2, BooleanOp.NOT_SAME)) {
            List<PathNavigation> list = new ObjectArrayList<>();

            for(Mob mob : this.navigatingMobs) {
                PathNavigation pathNavigation = mob.getNavigation();
                if (pathNavigation.shouldRecomputePath(pos)) {
                    list.add(pathNavigation);
                }
            }

            try {
                this.isUpdatingNavigations = true;

                for(PathNavigation pathNavigation2 : list) {
                    pathNavigation2.recomputePath();
                }
            } finally {
                this.isUpdatingNavigations = false;
            }

        }
    }

    @Override
    public void broadcastEntityEvent(Entity entity, byte status) {
        this.getChunkSource().broadcastAndSend(entity, new ClientboundEntityEventPacket(entity, status));
    }

    @Override
    public ServerChunkCache getChunkSource() {
        return this.chunkSource;
    }

    @Override
    public Explosion explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator behavior, double x, double y, double z, float power, boolean createFire, Explosion.BlockInteraction destructionType) {
        Explosion explosion = new Explosion(this, entity, damageSource, behavior, x, y, z, power, createFire, destructionType);
        explosion.explode();
        explosion.finalizeExplosion(false);
        if (destructionType == Explosion.BlockInteraction.NONE) {
            explosion.clearToBlow();
        }

        for(ServerPlayer serverPlayer : this.players) {
            if (serverPlayer.distanceToSqr(x, y, z) < 4096.0D) {
                serverPlayer.connection.send(new ClientboundExplodePacket(x, y, z, power, explosion.getToBlow(), explosion.getHitPlayers().get(serverPlayer)));
            }
        }

        return explosion;
    }

    @Override
    public void blockEvent(BlockPos pos, Block block, int type, int data) {
        this.blockEvents.add(new BlockEventData(pos, block, type, data));
    }

    private void runBlockEvents() {
        this.blockEventsToReschedule.clear();

        while(!this.blockEvents.isEmpty()) {
            BlockEventData blockEventData = this.blockEvents.removeFirst();
            if (this.shouldTickBlocksAt(ChunkPos.asLong(blockEventData.pos()))) {
                if (this.doBlockEvent(blockEventData)) {
                    this.server.getPlayerList().broadcast((Player)null, (double)blockEventData.pos().getX(), (double)blockEventData.pos().getY(), (double)blockEventData.pos().getZ(), 64.0D, this.dimension(), new ClientboundBlockEventPacket(blockEventData.pos(), blockEventData.block(), blockEventData.paramA(), blockEventData.paramB()));
                }
            } else {
                this.blockEventsToReschedule.add(blockEventData);
            }
        }

        this.blockEvents.addAll(this.blockEventsToReschedule);
    }

    private boolean doBlockEvent(BlockEventData event) {
        BlockState blockState = this.getBlockState(event.pos());
        return blockState.is(event.block()) ? blockState.triggerEvent(this, event.pos(), event.paramA(), event.paramB()) : false;
    }

    @Override
    public LevelTicks<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public LevelTicks<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Nonnull
    @Override
    public MinecraftServer getServer() {
        return this.server;
    }

    public PortalForcer getPortalForcer() {
        return this.portalForcer;
    }

    public StructureManager getStructureManager() {
        return this.server.getStructureManager();
    }

    public void sendVibrationParticle(VibrationPath vibration) {
        BlockPos blockPos = vibration.getOrigin();
        ClientboundAddVibrationSignalPacket clientboundAddVibrationSignalPacket = new ClientboundAddVibrationSignalPacket(vibration);
        this.players.forEach((player) -> {
            this.sendParticles(player, false, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), clientboundAddVibrationSignalPacket);
        });
    }

    public <T extends ParticleOptions> int sendParticles(T particle, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed) {
        ClientboundLevelParticlesPacket clientboundLevelParticlesPacket = new ClientboundLevelParticlesPacket(particle, false, x, y, z, (float)deltaX, (float)deltaY, (float)deltaZ, (float)speed, count);
        int i = 0;

        for(int j = 0; j < this.players.size(); ++j) {
            ServerPlayer serverPlayer = this.players.get(j);
            if (this.sendParticles(serverPlayer, false, x, y, z, clientboundLevelParticlesPacket)) {
                ++i;
            }
        }

        return i;
    }

    public <T extends ParticleOptions> boolean sendParticles(ServerPlayer viewer, T particle, boolean force, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed) {
        Packet<?> packet = new ClientboundLevelParticlesPacket(particle, force, x, y, z, (float)deltaX, (float)deltaY, (float)deltaZ, (float)speed, count);
        return this.sendParticles(viewer, force, x, y, z, packet);
    }

    private boolean sendParticles(ServerPlayer player, boolean force, double x, double y, double z, Packet<?> packet) {
        if (player.getLevel() != this) {
            return false;
        } else {
            BlockPos blockPos = player.blockPosition();
            if (blockPos.closerToCenterThan(new Vec3(x, y, z), force ? 512.0D : 32.0D)) {
                player.connection.send(packet);
                return true;
            } else {
                return false;
            }
        }
    }

    @Nullable
    @Override
    public Entity getEntity(int id) {
        return this.getEntities().get(id);
    }

    /** @deprecated */
    @Deprecated
    @Nullable
    public Entity getEntityOrPart(int id) {
        Entity entity = this.getEntities().get(id);
        return entity != null ? entity : this.dragonParts.get(id);
    }

    @Nullable
    public Entity getEntity(UUID uuid) {
        return this.getEntities().get(uuid);
    }

    @Nullable
    public BlockPos findNearestMapFeature(TagKey<ConfiguredStructureFeature<?, ?>> structureTag, BlockPos pos, int radius, boolean skipExistingChunks) {
        if (!this.server.getWorldData().worldGenSettings().generateFeatures()) {
            return null;
        } else {
            Optional<HolderSet.Named<ConfiguredStructureFeature<?, ?>>> optional = this.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).getTag(structureTag);
            if (optional.isEmpty()) {
                return null;
            } else {
                Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> pair = this.getChunkSource().getGenerator().findNearestMapFeature(this, optional.get(), pos, radius, skipExistingChunks);
                return pair != null ? pair.getFirst() : null;
            }
        }
    }

    @Nullable
    public Pair<BlockPos, Holder<Biome>> findNearestBiome(Predicate<Holder<Biome>> biomeEntryPredicate, BlockPos pos, int radius, int blockCheckInterval) {
        return this.getChunkSource().getGenerator().getBiomeSource().findBiomeHorizontal(pos.getX(), pos.getY(), pos.getZ(), radius, blockCheckInterval, biomeEntryPredicate, this.random, true, this.getChunkSource().getGenerator().climateSampler());
    }

    @Override
    public RecipeManager getRecipeManager() {
        return this.server.getRecipeManager();
    }

    @Override
    public boolean noSave() {
        return this.noSave;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.server.registryAccess();
    }

    public DimensionDataStorage getDataStorage() {
        return this.getChunkSource().getDataStorage();
    }

    @Nullable
    @Override
    public MapItemSavedData getMapData(String id) {
        return this.getServer().overworld().getDataStorage().get(MapItemSavedData::load, id);
    }

    @Override
    public void setMapData(String id, MapItemSavedData state) {
        this.getServer().overworld().getDataStorage().set(id, state);
    }

    @Override
    public int getFreeMapId() {
        return this.getServer().overworld().getDataStorage().computeIfAbsent(MapIndex::load, MapIndex::new, "idcounts").getFreeAuxValueForMap();
    }

    public void setDefaultSpawnPos(BlockPos pos, float angle) {
        ChunkPos chunkPos = new ChunkPos(new BlockPos(this.levelData.getXSpawn(), 0, this.levelData.getZSpawn()));
        this.levelData.setSpawn(pos, angle);
        this.getChunkSource().removeRegionTicket(TicketType.START, chunkPos, 11, Unit.INSTANCE);
        this.getChunkSource().addRegionTicket(TicketType.START, new ChunkPos(pos), 11, Unit.INSTANCE);
        this.getServer().getPlayerList().broadcastAll(new ClientboundSetDefaultSpawnPositionPacket(pos, angle));
    }

    public BlockPos getSharedSpawnPos() {
        BlockPos blockPos = new BlockPos(this.levelData.getXSpawn(), this.levelData.getYSpawn(), this.levelData.getZSpawn());
        if (!this.getWorldBorder().isWithinBounds(blockPos)) {
            blockPos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(this.getWorldBorder().getCenterX(), 0.0D, this.getWorldBorder().getCenterZ()));
        }

        return blockPos;
    }

    public float getSharedSpawnAngle() {
        return this.levelData.getSpawnAngle();
    }

    public LongSet getForcedChunks() {
        ForcedChunksSavedData forcedChunksSavedData = this.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
        return (LongSet)(forcedChunksSavedData != null ? LongSets.unmodifiable(forcedChunksSavedData.getChunks()) : LongSets.EMPTY_SET);
    }

    public boolean setChunkForced(int x, int z, boolean forced) {
        ForcedChunksSavedData forcedChunksSavedData = this.getDataStorage().computeIfAbsent(ForcedChunksSavedData::load, ForcedChunksSavedData::new, "chunks");
        ChunkPos chunkPos = new ChunkPos(x, z);
        long l = chunkPos.toLong();
        boolean bl;
        if (forced) {
            bl = forcedChunksSavedData.getChunks().add(l);
            if (bl) {
                this.getChunk(x, z);
            }
        } else {
            bl = forcedChunksSavedData.getChunks().remove(l);
        }

        forcedChunksSavedData.setDirty(bl);
        if (bl) {
            this.getChunkSource().updateChunkForced(chunkPos, forced);
        }

        return bl;
    }

    @Override
    public List<ServerPlayer> players() {
        return this.players;
    }

    @Override
    public void onBlockStateChange(BlockPos pos, BlockState oldBlock, BlockState newBlock) {
        Optional<PoiType> optional = PoiType.forState(oldBlock);
        Optional<PoiType> optional2 = PoiType.forState(newBlock);
        if (!Objects.equals(optional, optional2)) {
            BlockPos blockPos = pos.immutable();
            optional.ifPresent((poiType) -> {
                this.getServer().execute(() -> {
                    this.getPoiManager().remove(blockPos);
                    DebugPackets.sendPoiRemovedPacket(this, blockPos);
                });
            });
            optional2.ifPresent((poiType) -> {
                this.getServer().execute(() -> {
                    this.getPoiManager().add(blockPos, poiType);
                    DebugPackets.sendPoiAddedPacket(this, blockPos);
                });
            });
        }
    }

    public PoiManager getPoiManager() {
        return this.getChunkSource().getPoiManager();
    }

    public boolean isVillage(BlockPos pos) {
        return this.isCloseToVillage(pos, 1);
    }

    public boolean isVillage(SectionPos sectionPos) {
        return this.isVillage(sectionPos.center());
    }

    public boolean isCloseToVillage(BlockPos pos, int maxDistance) {
        if (maxDistance > 6) {
            return false;
        } else {
            return this.sectionsToVillage(SectionPos.of(pos)) <= maxDistance;
        }
    }

    public int sectionsToVillage(SectionPos pos) {
        return this.getPoiManager().sectionsToVillage(pos);
    }

    public Raids getRaids() {
        return this.raids;
    }

    @Nullable
    public Raid getRaidAt(BlockPos pos) {
        return this.raids.getNearbyRaid(pos, 9216);
    }

    public boolean isRaided(BlockPos pos) {
        return this.getRaidAt(pos) != null;
    }

    public void onReputationEvent(ReputationEventType interaction, Entity entity, ReputationEventHandler observer) {
        observer.onReputationEventFrom(interaction, entity);
    }

    public void saveDebugReport(Path path) throws IOException {
        ChunkMap chunkMap = this.getChunkSource().chunkMap;
        Writer writer = Files.newBufferedWriter(path.resolve("stats.txt"));

        try {
            writer.write(String.format("spawning_chunks: %d\n", chunkMap.getDistanceManager().getNaturalSpawnChunkCount()));
            NaturalSpawner.SpawnState spawnState = this.getChunkSource().getLastSpawnState();
            if (spawnState != null) {
                for(Entry<MobCategory> entry : spawnState.getMobCategoryCounts().object2IntEntrySet()) {
                    writer.write(String.format("spawn_count.%s: %d\n", entry.getKey().getName(), entry.getIntValue()));
                }
            }

            writer.write(String.format("entities: %s\n", this.entityManager.gatherStats()));
            writer.write(String.format("block_entity_tickers: %d\n", this.blockEntityTickers.size()));
            writer.write(String.format("block_ticks: %d\n", this.getBlockTicks().count()));
            writer.write(String.format("fluid_ticks: %d\n", this.getFluidTicks().count()));
            writer.write("distance_manager: " + chunkMap.getDistanceManager().getDebugStatus() + "\n");
            writer.write(String.format("pending_tasks: %d\n", this.getChunkSource().getPendingTasksCount()));
        } catch (Throwable var22) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable var16) {
                    var22.addSuppressed(var16);
                }
            }

            throw var22;
        }

        if (writer != null) {
            writer.close();
        }

        CrashReport crashReport = new CrashReport("Level dump", new Exception("dummy"));
        this.fillReportDetails(crashReport);
        Writer writer2 = Files.newBufferedWriter(path.resolve("example_crash.txt"));

        try {
            writer2.write(crashReport.getFriendlyReport());
        } catch (Throwable var21) {
            if (writer2 != null) {
                try {
                    writer2.close();
                } catch (Throwable var15) {
                    var21.addSuppressed(var15);
                }
            }

            throw var21;
        }

        if (writer2 != null) {
            writer2.close();
        }

        Path path2 = path.resolve("chunks.csv");
        Writer writer3 = Files.newBufferedWriter(path2);

        try {
            chunkMap.dumpChunks(writer3);
        } catch (Throwable var20) {
            if (writer3 != null) {
                try {
                    writer3.close();
                } catch (Throwable var14) {
                    var20.addSuppressed(var14);
                }
            }

            throw var20;
        }

        if (writer3 != null) {
            writer3.close();
        }

        Path path3 = path.resolve("entity_chunks.csv");
        Writer writer4 = Files.newBufferedWriter(path3);

        try {
            this.entityManager.dumpSections(writer4);
        } catch (Throwable var19) {
            if (writer4 != null) {
                try {
                    writer4.close();
                } catch (Throwable var13) {
                    var19.addSuppressed(var13);
                }
            }

            throw var19;
        }

        if (writer4 != null) {
            writer4.close();
        }

        Path path4 = path.resolve("entities.csv");
        Writer writer5 = Files.newBufferedWriter(path4);

        try {
            dumpEntities(writer5, this.getEntities().getAll());
        } catch (Throwable var18) {
            if (writer5 != null) {
                try {
                    writer5.close();
                } catch (Throwable var12) {
                    var18.addSuppressed(var12);
                }
            }

            throw var18;
        }

        if (writer5 != null) {
            writer5.close();
        }

        Path path5 = path.resolve("block_entities.csv");
        Writer writer6 = Files.newBufferedWriter(path5);

        try {
            this.dumpBlockEntityTickers(writer6);
        } catch (Throwable var17) {
            if (writer6 != null) {
                try {
                    writer6.close();
                } catch (Throwable var11) {
                    var17.addSuppressed(var11);
                }
            }

            throw var17;
        }

        if (writer6 != null) {
            writer6.close();
        }

    }

    private static void dumpEntities(Writer writer, Iterable<Entity> entities) throws IOException {
        CsvOutput csvOutput = CsvOutput.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("uuid").addColumn("type").addColumn("alive").addColumn("display_name").addColumn("custom_name").build(writer);

        for(Entity entity : entities) {
            Component component = entity.getCustomName();
            Component component2 = entity.getDisplayName();
            csvOutput.writeRow(entity.getX(), entity.getY(), entity.getZ(), entity.getUUID(), Registry.ENTITY_TYPE.getKey(entity.getType()), entity.isAlive(), component2.getString(), component != null ? component.getString() : null);
        }

    }

    private void dumpBlockEntityTickers(Writer writer) throws IOException {
        CsvOutput csvOutput = CsvOutput.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("type").build(writer);

        for(TickingBlockEntity tickingBlockEntity : this.blockEntityTickers) {
            BlockPos blockPos = tickingBlockEntity.getPos();
            csvOutput.writeRow(blockPos.getX(), blockPos.getY(), blockPos.getZ(), tickingBlockEntity.getType());
        }

    }

    @VisibleForTesting
    public void clearBlockEvents(BoundingBox box) {
        this.blockEvents.removeIf((event) -> {
            return box.isInside(event.pos());
        });
    }

    @Override
    public void blockUpdated(BlockPos pos, Block block) {
        if (!this.isDebug()) {
            this.updateNeighborsAt(pos, block);
        }

    }

    @Override
    public float getShade(Direction direction, boolean shaded) {
        return 1.0F;
    }

    public Iterable<Entity> getAllEntities() {
        return this.getEntities().getAll();
    }

    @Override
    public String toString() {
        return "ServerLevel[" + this.serverLevelData.getLevelName() + "]";
    }

    public boolean isFlat() {
        return this.server.getWorldData().worldGenSettings().isFlatWorld();
    }

    @Override
    public long getSeed() {
        return this.server.getWorldData().worldGenSettings().seed();
    }

    @Nullable
    public EndDragonFight dragonFight() {
        return this.dragonFight;
    }

    @Override
    public ServerLevel getLevel() {
        return this;
    }

    @VisibleForTesting
    public String getWatchdogStats() {
        return String.format("players: %s, entities: %s [%s], block_entities: %d [%s], block_ticks: %d, fluid_ticks: %d, chunk_source: %s", this.players.size(), this.entityManager.gatherStats(), getTypeCount(this.entityManager.getEntityGetter().getAll(), (entity) -> {
            return Registry.ENTITY_TYPE.getKey(entity.getType()).toString();
        }), this.blockEntityTickers.size(), getTypeCount(this.blockEntityTickers, TickingBlockEntity::getType), this.getBlockTicks().count(), this.getFluidTicks().count(), this.gatherChunkSourceStats());
    }

    private static <T> String getTypeCount(Iterable<T> items, Function<T, String> classifier) {
        try {
            Object2IntOpenHashMap<String> object2IntOpenHashMap = new Object2IntOpenHashMap<>();

            for(T object : items) {
                String string = classifier.apply(object);
                object2IntOpenHashMap.addTo(string, 1);
            }

            return object2IntOpenHashMap.object2IntEntrySet().stream().sorted(Comparator.comparing(Entry::getIntValue).reversed()).limit(5L).map((entry) -> {
                return (String)entry.getKey() + ":" + entry.getIntValue();
            }).collect(Collectors.joining(","));
        } catch (Exception var6) {
            return "";
        }
    }

    public static void makeObsidianPlatform(ServerLevel world) {
        BlockPos blockPos = END_SPAWN_POINT;
        int i = blockPos.getX();
        int j = blockPos.getY() - 2;
        int k = blockPos.getZ();
        BlockPos.betweenClosed(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2).forEach((pos) -> {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        });
        BlockPos.betweenClosed(i - 2, j, k - 2, i + 2, j, k + 2).forEach((pos) -> {
            world.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
        });
    }

    @Override
    public LevelEntityGetter<Entity> getEntities() {
        return this.entityManager.getEntityGetter();
    }

    public void addLegacyChunkEntities(Stream<Entity> entities) {
        this.entityManager.addLegacyChunkEntities(entities);
    }

    public void addWorldGenChunkEntities(Stream<Entity> entities) {
        this.entityManager.addWorldGenChunkEntities(entities);
    }

    public void startTickingChunk(LevelChunk chunk) {
        chunk.unpackTicks(this.getLevelData().getGameTime());
    }

    public void onStructureStartsAvailable(ChunkAccess chunk) {
        this.server.execute(() -> {
            this.structureCheck.onStructureLoad(chunk.getPos(), chunk.getAllStarts());
        });
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.entityManager.close();
    }

    @Override
    public String gatherChunkSourceStats() {
        return "Chunks[S] W: " + this.chunkSource.gatherStats() + " E: " + this.entityManager.gatherStats();
    }

    public boolean areEntitiesLoaded(long chunkPos) {
        return this.entityManager.areEntitiesLoaded(chunkPos);
    }

    private boolean isPositionTickingWithEntitiesLoaded(long chunkPos) {
        return this.areEntitiesLoaded(chunkPos) && this.chunkSource.isPositionTicking(chunkPos);
    }

    public boolean isPositionEntityTicking(BlockPos pos) {
        return this.entityManager.canPositionTick(pos) && this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(ChunkPos.asLong(pos));
    }

    public boolean isNaturalSpawningAllowed(BlockPos pos) {
        return this.entityManager.canPositionTick(pos);
    }

    public boolean isNaturalSpawningAllowed(ChunkPos pos) {
        return this.entityManager.canPositionTick(pos);
    }

    final class EntityCallbacks implements LevelCallback<Entity> {
        @Override
        public void onCreated(Entity entity) {
        }

        @Override
        public void onDestroyed(Entity entity) {
            ServerLevel.this.getScoreboard().entityRemoved(entity);
        }

        @Override
        public void onTickingStart(Entity entity) {
            ServerLevel.this.entityTickList.add(entity);
        }

        @Override
        public void onTickingEnd(Entity entity) {
            ServerLevel.this.entityTickList.remove(entity);
        }

        @Override
        public void onTrackingStart(Entity entity) {
            ServerLevel.this.getChunkSource().addEntity(entity);
            if (entity instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)entity;
                ServerLevel.this.players.add(serverPlayer);
                ServerLevel.this.updateSleepingPlayerList();
            }

            if (entity instanceof Mob) {
                Mob mob = (Mob)entity;
                if (ServerLevel.this.isUpdatingNavigations) {
                    String string = "onTrackingStart called during navigation iteration";
                    Util.logAndPauseIfInIde("onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration"));
                }

                ServerLevel.this.navigatingMobs.add(mob);
            }

            if (entity instanceof EnderDragon) {
                EnderDragon enderDragon = (EnderDragon)entity;

                for(EnderDragonPart enderDragonPart : enderDragon.getSubEntities()) {
                    ServerLevel.this.dragonParts.put(enderDragonPart.getId(), enderDragonPart);
                }
            }

        }

        @Override
        public void onTrackingEnd(Entity entity) {
            ServerLevel.this.getChunkSource().removeEntity(entity);
            if (entity instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)entity;
                ServerLevel.this.players.remove(serverPlayer);
                ServerLevel.this.updateSleepingPlayerList();
            }

            if (entity instanceof Mob) {
                Mob mob = (Mob)entity;
                if (ServerLevel.this.isUpdatingNavigations) {
                    String string = "onTrackingStart called during navigation iteration";
                    Util.logAndPauseIfInIde("onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration"));
                }

                ServerLevel.this.navigatingMobs.remove(mob);
            }

            if (entity instanceof EnderDragon) {
                EnderDragon enderDragon = (EnderDragon)entity;

                for(EnderDragonPart enderDragonPart : enderDragon.getSubEntities()) {
                    ServerLevel.this.dragonParts.remove(enderDragonPart.getId());
                }
            }

            GameEventListenerRegistrar gameEventListenerRegistrar = entity.getGameEventListenerRegistrar();
            if (gameEventListenerRegistrar != null) {
                gameEventListenerRegistrar.onListenerRemoved(entity.level);
            }

        }
    }
}
