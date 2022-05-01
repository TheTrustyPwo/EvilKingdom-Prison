package net.minecraft.world.level;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.NetherFortressFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class NaturalSpawner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MIN_SPAWN_DISTANCE = 24;
    public static final int SPAWN_DISTANCE_CHUNK = 8;
    public static final int SPAWN_DISTANCE_BLOCK = 128;
    static final int MAGIC_NUMBER = (int)Math.pow(17.0D, 2.0D);
    public static final MobCategory[] SPAWNING_CATEGORIES = Stream.of(MobCategory.values()).filter((spawnGroup) -> {
        return spawnGroup != MobCategory.MISC;
    }).toArray((i) -> {
        return new MobCategory[i];
    });

    private NaturalSpawner() {
    }

    public static NaturalSpawner.SpawnState createState(int spawningChunkCount, Iterable<Entity> entities, NaturalSpawner.ChunkGetter chunkSource, LocalMobCapCalculator localMobCapCalculator) {
        PotentialCalculator potentialCalculator = new PotentialCalculator();
        Object2IntOpenHashMap<MobCategory> object2IntOpenHashMap = new Object2IntOpenHashMap<>();
        Iterator var6 = entities.iterator();

        while(true) {
            Entity entity;
            Mob mob;
            do {
                if (!var6.hasNext()) {
                    return new NaturalSpawner.SpawnState(spawningChunkCount, object2IntOpenHashMap, potentialCalculator, localMobCapCalculator);
                }

                entity = (Entity)var6.next();
                if (!(entity instanceof Mob)) {
                    break;
                }

                mob = (Mob)entity;
            } while(mob.isPersistenceRequired() || mob.requiresCustomPersistence());

            MobCategory mobCategory = entity.getType().getCategory();
            if (mobCategory != MobCategory.MISC) {
                BlockPos blockPos = entity.blockPosition();
                chunkSource.query(ChunkPos.asLong(blockPos), (levelChunk) -> {
                    MobSpawnSettings.MobSpawnCost mobSpawnCost = getRoughBiome(blockPos, levelChunk).getMobSettings().getMobSpawnCost(entity.getType());
                    if (mobSpawnCost != null) {
                        potentialCalculator.addCharge(entity.blockPosition(), mobSpawnCost.getCharge());
                    }

                    if (entity instanceof Mob) {
                        localMobCapCalculator.addMob(levelChunk.getPos(), mobCategory);
                    }

                    object2IntOpenHashMap.addTo(mobCategory, 1);
                });
            }
        }
    }

    static Biome getRoughBiome(BlockPos pos, ChunkAccess chunk) {
        return chunk.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ())).value();
    }

    public static void spawnForChunk(ServerLevel world, LevelChunk chunk, NaturalSpawner.SpawnState info, boolean spawnAnimals, boolean spawnMonsters, boolean rareSpawn) {
        world.getProfiler().push("spawner");

        for(MobCategory mobCategory : SPAWNING_CATEGORIES) {
            if ((spawnAnimals || !mobCategory.isFriendly()) && (spawnMonsters || mobCategory.isFriendly()) && (rareSpawn || !mobCategory.isPersistent()) && info.canSpawnForCategory(mobCategory, chunk.getPos())) {
                spawnCategoryForChunk(mobCategory, world, chunk, info::canSpawn, info::afterSpawn);
            }
        }

        world.getProfiler().pop();
    }

    public static void spawnCategoryForChunk(MobCategory group, ServerLevel world, LevelChunk chunk, NaturalSpawner.SpawnPredicate checker, NaturalSpawner.AfterSpawnCallback runner) {
        BlockPos blockPos = getRandomPosWithin(world, chunk);
        if (blockPos.getY() >= world.getMinBuildHeight() + 1) {
            spawnCategoryForPosition(group, world, chunk, blockPos, checker, runner);
        }
    }

    @VisibleForDebug
    public static void spawnCategoryForPosition(MobCategory group, ServerLevel world, BlockPos pos) {
        spawnCategoryForPosition(group, world, world.getChunk(pos), pos, (type, posx, chunk) -> {
            return true;
        }, (entity, chunk) -> {
        });
    }

    public static void spawnCategoryForPosition(MobCategory group, ServerLevel world, ChunkAccess chunk, BlockPos pos, NaturalSpawner.SpawnPredicate checker, NaturalSpawner.AfterSpawnCallback runner) {
        StructureFeatureManager structureFeatureManager = world.structureFeatureManager();
        ChunkGenerator chunkGenerator = world.getChunkSource().getGenerator();
        int i = pos.getY();
        BlockState blockState = chunk.getBlockState(pos);
        if (!blockState.isRedstoneConductor(chunk, pos)) {
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            int j = 0;

            for(int k = 0; k < 3; ++k) {
                int l = pos.getX();
                int m = pos.getZ();
                int n = 6;
                MobSpawnSettings.SpawnerData spawnerData = null;
                SpawnGroupData spawnGroupData = null;
                int o = Mth.ceil(world.random.nextFloat() * 4.0F);
                int p = 0;

                for(int q = 0; q < o; ++q) {
                    l += world.random.nextInt(6) - world.random.nextInt(6);
                    m += world.random.nextInt(6) - world.random.nextInt(6);
                    mutableBlockPos.set(l, i, m);
                    double d = (double)l + 0.5D;
                    double e = (double)m + 0.5D;
                    Player player = world.getNearestPlayer(d, (double)i, e, -1.0D, false);
                    if (player != null) {
                        double f = player.distanceToSqr(d, (double)i, e);
                        if (isRightDistanceToPlayerAndSpawnPoint(world, chunk, mutableBlockPos, f)) {
                            if (spawnerData == null) {
                                Optional<MobSpawnSettings.SpawnerData> optional = getRandomSpawnMobAt(world, structureFeatureManager, chunkGenerator, group, world.random, mutableBlockPos);
                                if (optional.isEmpty()) {
                                    break;
                                }

                                spawnerData = optional.get();
                                o = spawnerData.minCount + world.random.nextInt(1 + spawnerData.maxCount - spawnerData.minCount);
                            }

                            if (isValidSpawnPostitionForType(world, group, structureFeatureManager, chunkGenerator, spawnerData, mutableBlockPos, f) && checker.test(spawnerData.type, mutableBlockPos, chunk)) {
                                Mob mob = getMobForSpawn(world, spawnerData.type);
                                if (mob == null) {
                                    return;
                                }

                                mob.moveTo(d, (double)i, e, world.random.nextFloat() * 360.0F, 0.0F);
                                if (isValidPositionForMob(world, mob, f)) {
                                    spawnGroupData = mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.NATURAL, spawnGroupData, (CompoundTag)null);
                                    ++j;
                                    ++p;
                                    world.addFreshEntityWithPassengers(mob);
                                    runner.run(mob, chunk);
                                    if (j >= mob.getMaxSpawnClusterSize()) {
                                        return;
                                    }

                                    if (mob.isMaxGroupSizeReached(p)) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private static boolean isRightDistanceToPlayerAndSpawnPoint(ServerLevel world, ChunkAccess chunk, BlockPos.MutableBlockPos pos, double squaredDistance) {
        if (squaredDistance <= 576.0D) {
            return false;
        } else if (world.getSharedSpawnPos().closerToCenterThan(new Vec3((double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D), 24.0D)) {
            return false;
        } else {
            return Objects.equals(new ChunkPos(pos), chunk.getPos()) || world.isNaturalSpawningAllowed(pos);
        }
    }

    private static boolean isValidSpawnPostitionForType(ServerLevel world, MobCategory group, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, MobSpawnSettings.SpawnerData spawnEntry, BlockPos.MutableBlockPos pos, double squaredDistance) {
        EntityType<?> entityType = spawnEntry.type;
        if (entityType.getCategory() == MobCategory.MISC) {
            return false;
        } else if (!entityType.canSpawnFarFromPlayer() && squaredDistance > (double)(entityType.getCategory().getDespawnDistance() * entityType.getCategory().getDespawnDistance())) {
            return false;
        } else if (entityType.canSummon() && canSpawnMobAt(world, structureAccessor, chunkGenerator, group, spawnEntry, pos)) {
            SpawnPlacements.Type type = SpawnPlacements.getPlacementType(entityType);
            if (!isSpawnPositionOk(type, world, pos, entityType)) {
                return false;
            } else if (!SpawnPlacements.checkSpawnRules(entityType, world, MobSpawnType.NATURAL, pos, world.random)) {
                return false;
            } else {
                return world.noCollision(entityType.getAABB((double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D));
            }
        } else {
            return false;
        }
    }

    @Nullable
    private static Mob getMobForSpawn(ServerLevel world, EntityType<?> type) {
        try {
            Entity entity = type.create(world);
            if (!(entity instanceof Mob)) {
                throw new IllegalStateException("Trying to spawn a non-mob: " + Registry.ENTITY_TYPE.getKey(type));
            } else {
                return (Mob)entity;
            }
        } catch (Exception var4) {
            LOGGER.warn("Failed to create mob", (Throwable)var4);
            return null;
        }
    }

    private static boolean isValidPositionForMob(ServerLevel world, Mob entity, double squaredDistance) {
        if (squaredDistance > (double)(entity.getType().getCategory().getDespawnDistance() * entity.getType().getCategory().getDespawnDistance()) && entity.removeWhenFarAway(squaredDistance)) {
            return false;
        } else {
            return entity.checkSpawnRules(world, MobSpawnType.NATURAL) && entity.checkSpawnObstruction(world);
        }
    }

    private static Optional<MobSpawnSettings.SpawnerData> getRandomSpawnMobAt(ServerLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, MobCategory spawnGroup, Random random, BlockPos pos) {
        Holder<Biome> holder = world.getBiome(pos);
        return spawnGroup == MobCategory.WATER_AMBIENT && Biome.getBiomeCategory(holder) == Biome.BiomeCategory.RIVER && random.nextFloat() < 0.98F ? Optional.empty() : mobsAt(world, structureAccessor, chunkGenerator, spawnGroup, pos, holder).getRandom(random);
    }

    private static boolean canSpawnMobAt(ServerLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, MobCategory spawnGroup, MobSpawnSettings.SpawnerData spawnEntry, BlockPos pos) {
        return mobsAt(world, structureAccessor, chunkGenerator, spawnGroup, pos, (Holder<Biome>)null).unwrap().contains(spawnEntry);
    }

    private static WeightedRandomList<MobSpawnSettings.SpawnerData> mobsAt(ServerLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, MobCategory spawnGroup, BlockPos pos, @Nullable Holder<Biome> biomeEntry) {
        return isInNetherFortressBounds(pos, world, spawnGroup, structureAccessor) ? NetherFortressFeature.FORTRESS_ENEMIES : chunkGenerator.getMobsAt(biomeEntry != null ? biomeEntry : world.getBiome(pos), structureAccessor, spawnGroup, pos);
    }

    public static boolean isInNetherFortressBounds(BlockPos pos, ServerLevel world, MobCategory spawnGroup, StructureFeatureManager structureAccessor) {
        if (spawnGroup == MobCategory.MONSTER && world.getBlockState(pos.below()).is(Blocks.NETHER_BRICKS)) {
            ConfiguredStructureFeature<?, ?> configuredStructureFeature = structureAccessor.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).get(BuiltinStructures.FORTRESS);
            return configuredStructureFeature == null ? false : structureAccessor.getStructureAt(pos, configuredStructureFeature).isValid();
        } else {
            return false;
        }
    }

    private static BlockPos getRandomPosWithin(Level world, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.getMinBlockX() + world.random.nextInt(16);
        int j = chunkPos.getMinBlockZ() + world.random.nextInt(16);
        int k = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, i, j) + 1;
        int l = Mth.randomBetweenInclusive(world.random, world.getMinBuildHeight(), k);
        return new BlockPos(i, l, j);
    }

    public static boolean isValidEmptySpawnBlock(BlockGetter blockView, BlockPos pos, BlockState state, FluidState fluidState, EntityType<?> entityType) {
        if (state.isCollisionShapeFullBlock(blockView, pos)) {
            return false;
        } else if (state.isSignalSource()) {
            return false;
        } else if (!fluidState.isEmpty()) {
            return false;
        } else if (state.is(BlockTags.PREVENT_MOB_SPAWNING_INSIDE)) {
            return false;
        } else {
            return !entityType.isBlockDangerous(state);
        }
    }

    public static boolean isSpawnPositionOk(SpawnPlacements.Type location, LevelReader world, BlockPos pos, @Nullable EntityType<?> entityType) {
        if (location == SpawnPlacements.Type.NO_RESTRICTIONS) {
            return true;
        } else if (entityType != null && world.getWorldBorder().isWithinBounds(pos)) {
            BlockState blockState = world.getBlockState(pos);
            FluidState fluidState = world.getFluidState(pos);
            BlockPos blockPos = pos.above();
            BlockPos blockPos2 = pos.below();
            switch(location) {
            case IN_WATER:
                return fluidState.is(FluidTags.WATER) && !world.getBlockState(blockPos).isRedstoneConductor(world, blockPos);
            case IN_LAVA:
                return fluidState.is(FluidTags.LAVA);
            case ON_GROUND:
            default:
                BlockState blockState2 = world.getBlockState(blockPos2);
                if (!blockState2.isValidSpawn(world, blockPos2, entityType)) {
                    return false;
                } else {
                    return isValidEmptySpawnBlock(world, pos, blockState, fluidState, entityType) && isValidEmptySpawnBlock(world, blockPos, world.getBlockState(blockPos), world.getFluidState(blockPos), entityType);
                }
            }
        } else {
            return false;
        }
    }

    public static void spawnMobsForChunkGeneration(ServerLevelAccessor world, Holder<Biome> holder, ChunkPos chunkPos, Random random) {
        MobSpawnSettings mobSpawnSettings = holder.value().getMobSettings();
        WeightedRandomList<MobSpawnSettings.SpawnerData> weightedRandomList = mobSpawnSettings.getMobs(MobCategory.CREATURE);
        if (!weightedRandomList.isEmpty()) {
            int i = chunkPos.getMinBlockX();
            int j = chunkPos.getMinBlockZ();

            while(random.nextFloat() < mobSpawnSettings.getCreatureProbability()) {
                Optional<MobSpawnSettings.SpawnerData> optional = weightedRandomList.getRandom(random);
                if (optional.isPresent()) {
                    MobSpawnSettings.SpawnerData spawnerData = optional.get();
                    int k = spawnerData.minCount + random.nextInt(1 + spawnerData.maxCount - spawnerData.minCount);
                    SpawnGroupData spawnGroupData = null;
                    int l = i + random.nextInt(16);
                    int m = j + random.nextInt(16);
                    int n = l;
                    int o = m;

                    for(int p = 0; p < k; ++p) {
                        boolean bl = false;

                        for(int q = 0; !bl && q < 4; ++q) {
                            BlockPos blockPos = getTopNonCollidingPos(world, spawnerData.type, l, m);
                            if (spawnerData.type.canSummon() && isSpawnPositionOk(SpawnPlacements.getPlacementType(spawnerData.type), world, blockPos, spawnerData.type)) {
                                float f = spawnerData.type.getWidth();
                                double d = Mth.clamp((double)l, (double)i + (double)f, (double)i + 16.0D - (double)f);
                                double e = Mth.clamp((double)m, (double)j + (double)f, (double)j + 16.0D - (double)f);
                                if (!world.noCollision(spawnerData.type.getAABB(d, (double)blockPos.getY(), e)) || !SpawnPlacements.checkSpawnRules(spawnerData.type, world, MobSpawnType.CHUNK_GENERATION, new BlockPos(d, (double)blockPos.getY(), e), world.getRandom())) {
                                    continue;
                                }

                                Entity entity;
                                try {
                                    entity = spawnerData.type.create(world.getLevel());
                                } catch (Exception var27) {
                                    LOGGER.warn("Failed to create mob", (Throwable)var27);
                                    continue;
                                }

                                entity.moveTo(d, (double)blockPos.getY(), e, random.nextFloat() * 360.0F, 0.0F);
                                if (entity instanceof Mob) {
                                    Mob mob = (Mob)entity;
                                    if (mob.checkSpawnRules(world, MobSpawnType.CHUNK_GENERATION) && mob.checkSpawnObstruction(world)) {
                                        spawnGroupData = mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.CHUNK_GENERATION, spawnGroupData, (CompoundTag)null);
                                        world.addFreshEntityWithPassengers(mob);
                                        bl = true;
                                    }
                                }
                            }

                            l += random.nextInt(5) - random.nextInt(5);

                            for(m += random.nextInt(5) - random.nextInt(5); l < i || l >= i + 16 || m < j || m >= j + 16; m = o + random.nextInt(5) - random.nextInt(5)) {
                                l = n + random.nextInt(5) - random.nextInt(5);
                            }
                        }
                    }
                }
            }

        }
    }

    private static BlockPos getTopNonCollidingPos(LevelReader world, EntityType<?> entityType, int x, int z) {
        int i = world.getHeight(SpawnPlacements.getHeightmapType(entityType), x, z);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(x, i, z);
        if (world.dimensionType().hasCeiling()) {
            do {
                mutableBlockPos.move(Direction.DOWN);
            } while(!world.getBlockState(mutableBlockPos).isAir());

            do {
                mutableBlockPos.move(Direction.DOWN);
            } while(world.getBlockState(mutableBlockPos).isAir() && mutableBlockPos.getY() > world.getMinBuildHeight());
        }

        if (SpawnPlacements.getPlacementType(entityType) == SpawnPlacements.Type.ON_GROUND) {
            BlockPos blockPos = mutableBlockPos.below();
            if (world.getBlockState(blockPos).isPathfindable(world, blockPos, PathComputationType.LAND)) {
                return blockPos;
            }
        }

        return mutableBlockPos.immutable();
    }

    @FunctionalInterface
    public interface AfterSpawnCallback {
        void run(Mob entity, ChunkAccess chunk);
    }

    @FunctionalInterface
    public interface ChunkGetter {
        void query(long pos, Consumer<LevelChunk> chunkConsumer);
    }

    @FunctionalInterface
    public interface SpawnPredicate {
        boolean test(EntityType<?> type, BlockPos pos, ChunkAccess chunk);
    }

    public static class SpawnState {
        private final int spawnableChunkCount;
        private final Object2IntOpenHashMap<MobCategory> mobCategoryCounts;
        private final PotentialCalculator spawnPotential;
        private final Object2IntMap<MobCategory> unmodifiableMobCategoryCounts;
        private final LocalMobCapCalculator localMobCapCalculator;
        @Nullable
        private BlockPos lastCheckedPos;
        @Nullable
        private EntityType<?> lastCheckedType;
        private double lastCharge;

        SpawnState(int spawningChunkCount, Object2IntOpenHashMap<MobCategory> groupToCount, PotentialCalculator densityField, LocalMobCapCalculator densityCapper) {
            this.spawnableChunkCount = spawningChunkCount;
            this.mobCategoryCounts = groupToCount;
            this.spawnPotential = densityField;
            this.localMobCapCalculator = densityCapper;
            this.unmodifiableMobCategoryCounts = Object2IntMaps.unmodifiable(groupToCount);
        }

        private boolean canSpawn(EntityType<?> type, BlockPos pos, ChunkAccess chunk) {
            this.lastCheckedPos = pos;
            this.lastCheckedType = type;
            MobSpawnSettings.MobSpawnCost mobSpawnCost = NaturalSpawner.getRoughBiome(pos, chunk).getMobSettings().getMobSpawnCost(type);
            if (mobSpawnCost == null) {
                this.lastCharge = 0.0D;
                return true;
            } else {
                double d = mobSpawnCost.getCharge();
                this.lastCharge = d;
                double e = this.spawnPotential.getPotentialEnergyChange(pos, d);
                return e <= mobSpawnCost.getEnergyBudget();
            }
        }

        private void afterSpawn(Mob entity, ChunkAccess chunk) {
            EntityType<?> entityType = entity.getType();
            BlockPos blockPos = entity.blockPosition();
            double d;
            if (blockPos.equals(this.lastCheckedPos) && entityType == this.lastCheckedType) {
                d = this.lastCharge;
            } else {
                MobSpawnSettings.MobSpawnCost mobSpawnCost = NaturalSpawner.getRoughBiome(blockPos, chunk).getMobSettings().getMobSpawnCost(entityType);
                if (mobSpawnCost != null) {
                    d = mobSpawnCost.getCharge();
                } else {
                    d = 0.0D;
                }
            }

            this.spawnPotential.addCharge(blockPos, d);
            MobCategory mobCategory = entityType.getCategory();
            this.mobCategoryCounts.addTo(mobCategory, 1);
            this.localMobCapCalculator.addMob(new ChunkPos(blockPos), mobCategory);
        }

        public int getSpawnableChunkCount() {
            return this.spawnableChunkCount;
        }

        public Object2IntMap<MobCategory> getMobCategoryCounts() {
            return this.unmodifiableMobCategoryCounts;
        }

        boolean canSpawnForCategory(MobCategory group, ChunkPos chunkPos) {
            int i = group.getMaxInstancesPerChunk() * this.spawnableChunkCount / NaturalSpawner.MAGIC_NUMBER;
            if (this.mobCategoryCounts.getInt(group) >= i) {
                return false;
            } else {
                return this.localMobCapCalculator.canSpawn(group, chunkPos);
            }
        }
    }
}
