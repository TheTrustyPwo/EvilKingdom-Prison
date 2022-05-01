package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Scoreboard;

public abstract class Level implements LevelAccessor, AutoCloseable {
    public static final Codec<ResourceKey<Level>> RESOURCE_KEY_CODEC = ResourceLocation.CODEC.xmap(ResourceKey.elementKey(Registry.DIMENSION_REGISTRY), ResourceKey::location);
    public static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation("overworld"));
    public static final ResourceKey<Level> NETHER = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation("the_nether"));
    public static final ResourceKey<Level> END = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation("the_end"));
    public static final int MAX_LEVEL_SIZE = 30000000;
    public static final int LONG_PARTICLE_CLIP_RANGE = 512;
    public static final int SHORT_PARTICLE_CLIP_RANGE = 32;
    private static final Direction[] DIRECTIONS = Direction.values();
    public static final int MAX_BRIGHTNESS = 15;
    public static final int TICKS_PER_DAY = 24000;
    public static final int MAX_ENTITY_SPAWN_Y = 20000000;
    public static final int MIN_ENTITY_SPAWN_Y = -20000000;
    protected final List<TickingBlockEntity> blockEntityTickers = Lists.newArrayList();
    private final List<TickingBlockEntity> pendingBlockEntityTickers = Lists.newArrayList();
    private boolean tickingBlockEntities;
    public final Thread thread;
    private final boolean isDebug;
    private int skyDarken;
    protected int randValue = (new Random()).nextInt();
    protected final int addend = 1013904223;
    protected float oRainLevel;
    public float rainLevel;
    protected float oThunderLevel;
    public float thunderLevel;
    public final Random random = new Random();
    final DimensionType dimensionType;
    private final Holder<DimensionType> dimensionTypeRegistration;
    public final WritableLevelData levelData;
    private final Supplier<ProfilerFiller> profiler;
    public final boolean isClientSide;
    private final WorldBorder worldBorder;
    private final BiomeManager biomeManager;
    private final ResourceKey<Level> dimension;
    private long subTickCount;

    protected Level(WritableLevelData properties, ResourceKey<Level> registryRef, Holder<DimensionType> holder, Supplier<ProfilerFiller> profiler, boolean isClient, boolean debugWorld, long seed) {
        this.profiler = profiler;
        this.levelData = properties;
        this.dimensionTypeRegistration = holder;
        this.dimensionType = holder.value();
        this.dimension = registryRef;
        this.isClientSide = isClient;
        if (this.dimensionType.coordinateScale() != 1.0D) {
            this.worldBorder = new WorldBorder() {
                @Override
                public double getCenterX() {
                    return super.getCenterX() / Level.this.dimensionType.coordinateScale();
                }

                @Override
                public double getCenterZ() {
                    return super.getCenterZ() / Level.this.dimensionType.coordinateScale();
                }
            };
        } else {
            this.worldBorder = new WorldBorder();
        }

        this.thread = Thread.currentThread();
        this.biomeManager = new BiomeManager(this, seed);
        this.isDebug = debugWorld;
    }

    @Override
    public boolean isClientSide() {
        return this.isClientSide;
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return null;
    }

    public boolean isInWorldBounds(BlockPos pos) {
        return !this.isOutsideBuildHeight(pos) && isInWorldBoundsHorizontal(pos);
    }

    public static boolean isInSpawnableBounds(BlockPos pos) {
        return !isOutsideSpawnableHeight(pos.getY()) && isInWorldBoundsHorizontal(pos);
    }

    private static boolean isInWorldBoundsHorizontal(BlockPos pos) {
        return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000;
    }

    private static boolean isOutsideSpawnableHeight(int y) {
        return y < -20000000 || y >= 20000000;
    }

    public LevelChunk getChunkAt(BlockPos pos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    @Override
    public LevelChunk getChunk(int i, int j) {
        return (LevelChunk)this.getChunk(i, j, ChunkStatus.FULL);
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        ChunkAccess chunkAccess = this.getChunkSource().getChunk(chunkX, chunkZ, leastStatus, create);
        if (chunkAccess == null && create) {
            throw new IllegalStateException("Should always be able to create a chunk!");
        } else {
            return chunkAccess;
        }
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        return this.setBlock(pos, state, flags, 512);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
        if (this.isOutsideBuildHeight(pos)) {
            return false;
        } else if (!this.isClientSide && this.isDebug()) {
            return false;
        } else {
            LevelChunk levelChunk = this.getChunkAt(pos);
            Block block = state.getBlock();
            BlockState blockState = levelChunk.setBlockState(pos, state, (flags & 64) != 0);
            if (blockState == null) {
                return false;
            } else {
                BlockState blockState2 = this.getBlockState(pos);
                if ((flags & 128) == 0 && blockState2 != blockState && (blockState2.getLightBlock(this, pos) != blockState.getLightBlock(this, pos) || blockState2.getLightEmission() != blockState.getLightEmission() || blockState2.useShapeForLightOcclusion() || blockState.useShapeForLightOcclusion())) {
                    this.getProfiler().push("queueCheckLight");
                    this.getChunkSource().getLightEngine().checkBlock(pos);
                    this.getProfiler().pop();
                }

                if (blockState2 == state) {
                    if (blockState != blockState2) {
                        this.setBlocksDirty(pos, blockState, blockState2);
                    }

                    if ((flags & 2) != 0 && (!this.isClientSide || (flags & 4) == 0) && (this.isClientSide || levelChunk.getFullStatus() != null && levelChunk.getFullStatus().isOrAfter(ChunkHolder.FullChunkStatus.TICKING))) {
                        this.sendBlockUpdated(pos, blockState, state, flags);
                    }

                    if ((flags & 1) != 0) {
                        this.blockUpdated(pos, blockState.getBlock());
                        if (!this.isClientSide && state.hasAnalogOutputSignal()) {
                            this.updateNeighbourForOutputSignal(pos, block);
                        }
                    }

                    if ((flags & 16) == 0 && maxUpdateDepth > 0) {
                        int i = flags & -34;
                        blockState.updateIndirectNeighbourShapes(this, pos, i, maxUpdateDepth - 1);
                        state.updateNeighbourShapes(this, pos, i, maxUpdateDepth - 1);
                        state.updateIndirectNeighbourShapes(this, pos, i, maxUpdateDepth - 1);
                    }

                    this.onBlockStateChange(pos, blockState, blockState2);
                }

                return true;
            }
        }
    }

    public void onBlockStateChange(BlockPos pos, BlockState oldBlock, BlockState newBlock) {
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean move) {
        FluidState fluidState = this.getFluidState(pos);
        return this.setBlock(pos, fluidState.createLegacyBlock(), 3 | (move ? 64 : 0));
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
        BlockState blockState = this.getBlockState(pos);
        if (blockState.isAir()) {
            return false;
        } else {
            FluidState fluidState = this.getFluidState(pos);
            if (!(blockState.getBlock() instanceof BaseFireBlock)) {
                this.levelEvent(2001, pos, Block.getId(blockState));
            }

            if (drop) {
                BlockEntity blockEntity = blockState.hasBlockEntity() ? this.getBlockEntity(pos) : null;
                Block.dropResources(blockState, this, pos, blockEntity, breakingEntity, ItemStack.EMPTY);
            }

            boolean bl = this.setBlock(pos, fluidState.createLegacyBlock(), 3, maxUpdateDepth);
            if (bl) {
                this.gameEvent(breakingEntity, GameEvent.BLOCK_DESTROY, pos);
            }

            return bl;
        }
    }

    public void addDestroyBlockEffect(BlockPos pos, BlockState state) {
    }

    public boolean setBlockAndUpdate(BlockPos pos, BlockState state) {
        return this.setBlock(pos, state, 3);
    }

    public abstract void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags);

    public void setBlocksDirty(BlockPos pos, BlockState old, BlockState updated) {
    }

    public void updateNeighborsAt(BlockPos pos, Block block) {
        this.neighborChanged(pos.west(), block, pos);
        this.neighborChanged(pos.east(), block, pos);
        this.neighborChanged(pos.below(), block, pos);
        this.neighborChanged(pos.above(), block, pos);
        this.neighborChanged(pos.north(), block, pos);
        this.neighborChanged(pos.south(), block, pos);
    }

    public void updateNeighborsAtExceptFromFacing(BlockPos pos, Block sourceBlock, Direction direction) {
        if (direction != Direction.WEST) {
            this.neighborChanged(pos.west(), sourceBlock, pos);
        }

        if (direction != Direction.EAST) {
            this.neighborChanged(pos.east(), sourceBlock, pos);
        }

        if (direction != Direction.DOWN) {
            this.neighborChanged(pos.below(), sourceBlock, pos);
        }

        if (direction != Direction.UP) {
            this.neighborChanged(pos.above(), sourceBlock, pos);
        }

        if (direction != Direction.NORTH) {
            this.neighborChanged(pos.north(), sourceBlock, pos);
        }

        if (direction != Direction.SOUTH) {
            this.neighborChanged(pos.south(), sourceBlock, pos);
        }

    }

    public void neighborChanged(BlockPos pos, Block sourceBlock, BlockPos neighborPos) {
        if (!this.isClientSide) {
            BlockState blockState = this.getBlockState(pos);

            try {
                blockState.neighborChanged(this, pos, sourceBlock, neighborPos, false);
            } catch (Throwable var8) {
                CrashReport crashReport = CrashReport.forThrowable(var8, "Exception while updating neighbours");
                CrashReportCategory crashReportCategory = crashReport.addCategory("Block being updated");
                crashReportCategory.setDetail("Source block type", () -> {
                    try {
                        return String.format("ID #%s (%s // %s)", Registry.BLOCK.getKey(sourceBlock), sourceBlock.getDescriptionId(), sourceBlock.getClass().getCanonicalName());
                    } catch (Throwable var2) {
                        return "ID #" + Registry.BLOCK.getKey(sourceBlock);
                    }
                });
                CrashReportCategory.populateBlockDetails(crashReportCategory, this, pos, blockState);
                throw new ReportedException(crashReport);
            }
        }
    }

    @Override
    public int getHeight(Heightmap.Types heightmap, int x, int z) {
        int j;
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000) {
            if (this.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                j = this.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)).getHeight(heightmap, x & 15, z & 15) + 1;
            } else {
                j = this.getMinBuildHeight();
            }
        } else {
            j = this.getSeaLevel() + 1;
        }

        return j;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.getChunkSource().getLightEngine();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        } else {
            LevelChunk levelChunk = this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
            return levelChunk.getBlockState(pos);
        }
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) {
            return Fluids.EMPTY.defaultFluidState();
        } else {
            LevelChunk levelChunk = this.getChunkAt(pos);
            return levelChunk.getFluidState(pos);
        }
    }

    public boolean isDay() {
        return !this.dimensionType().hasFixedTime() && this.skyDarken < 4;
    }

    public boolean isNight() {
        return !this.dimensionType().hasFixedTime() && !this.isDay();
    }

    @Override
    public void playSound(@Nullable Player player, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch) {
        this.playSound(player, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, sound, category, volume, pitch);
    }

    public abstract void playSound(@Nullable Player except, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch);

    public abstract void playSound(@Nullable Player except, Entity entity, SoundEvent sound, SoundSource category, float volume, float pitch);

    public void playLocalSound(double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean useDistance) {
    }

    @Override
    public void addParticle(ParticleOptions parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
    }

    public void addParticle(ParticleOptions parameters, boolean alwaysSpawn, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
    }

    public void addAlwaysVisibleParticle(ParticleOptions parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
    }

    public void addAlwaysVisibleParticle(ParticleOptions parameters, boolean alwaysSpawn, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
    }

    public float getSunAngle(float tickDelta) {
        float f = this.getTimeOfDay(tickDelta);
        return f * ((float)Math.PI * 2F);
    }

    public void addBlockEntityTicker(TickingBlockEntity ticker) {
        (this.tickingBlockEntities ? this.pendingBlockEntityTickers : this.blockEntityTickers).add(ticker);
    }

    protected void tickBlockEntities() {
        ProfilerFiller profilerFiller = this.getProfiler();
        profilerFiller.push("blockEntities");
        this.tickingBlockEntities = true;
        if (!this.pendingBlockEntityTickers.isEmpty()) {
            this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
            this.pendingBlockEntityTickers.clear();
        }

        Iterator<TickingBlockEntity> iterator = this.blockEntityTickers.iterator();

        while(iterator.hasNext()) {
            TickingBlockEntity tickingBlockEntity = iterator.next();
            if (tickingBlockEntity.isRemoved()) {
                iterator.remove();
            } else if (this.shouldTickBlocksAt(ChunkPos.asLong(tickingBlockEntity.getPos()))) {
                tickingBlockEntity.tick();
            }
        }

        this.tickingBlockEntities = false;
        profilerFiller.pop();
    }

    public <T extends Entity> void guardEntityTick(Consumer<T> tickConsumer, T entity) {
        try {
            tickConsumer.accept(entity);
        } catch (Throwable var6) {
            CrashReport crashReport = CrashReport.forThrowable(var6, "Ticking entity");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Entity being ticked");
            entity.fillCrashReportCategory(crashReportCategory);
            throw new ReportedException(crashReport);
        }
    }

    public boolean shouldTickDeath(Entity entity) {
        return true;
    }

    public boolean shouldTickBlocksAt(long chunkPos) {
        return true;
    }

    public Explosion explode(@Nullable Entity entity, double x, double y, double z, float power, Explosion.BlockInteraction destructionType) {
        return this.explode(entity, (DamageSource)null, (ExplosionDamageCalculator)null, x, y, z, power, false, destructionType);
    }

    public Explosion explode(@Nullable Entity entity, double x, double y, double z, float power, boolean createFire, Explosion.BlockInteraction destructionType) {
        return this.explode(entity, (DamageSource)null, (ExplosionDamageCalculator)null, x, y, z, power, createFire, destructionType);
    }

    public Explosion explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator behavior, double x, double y, double z, float power, boolean createFire, Explosion.BlockInteraction destructionType) {
        Explosion explosion = new Explosion(this, entity, damageSource, behavior, x, y, z, power, createFire, destructionType);
        explosion.explode();
        explosion.finalizeExplosion(true);
        return explosion;
    }

    public abstract String gatherChunkSourceStats();

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) {
            return null;
        } else {
            return !this.isClientSide && Thread.currentThread() != this.thread ? null : this.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
        }
    }

    public void setBlockEntity(BlockEntity blockEntity) {
        BlockPos blockPos = blockEntity.getBlockPos();
        if (!this.isOutsideBuildHeight(blockPos)) {
            this.getChunkAt(blockPos).addAndRegisterBlockEntity(blockEntity);
        }
    }

    public void removeBlockEntity(BlockPos pos) {
        if (!this.isOutsideBuildHeight(pos)) {
            this.getChunkAt(pos).removeBlockEntity(pos);
        }
    }

    public boolean isLoaded(BlockPos pos) {
        return this.isOutsideBuildHeight(pos) ? false : this.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public boolean loadedAndEntityCanStandOnFace(BlockPos pos, Entity entity, Direction direction) {
        if (this.isOutsideBuildHeight(pos)) {
            return false;
        } else {
            ChunkAccess chunkAccess = this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
            return chunkAccess == null ? false : chunkAccess.getBlockState(pos).entityCanStandOnFace(this, pos, entity, direction);
        }
    }

    public boolean loadedAndEntityCanStandOn(BlockPos pos, Entity entity) {
        return this.loadedAndEntityCanStandOnFace(pos, entity, Direction.UP);
    }

    public void updateSkyBrightness() {
        double d = 1.0D - (double)(this.getRainLevel(1.0F) * 5.0F) / 16.0D;
        double e = 1.0D - (double)(this.getThunderLevel(1.0F) * 5.0F) / 16.0D;
        double f = 0.5D + 2.0D * Mth.clamp((double)Mth.cos(this.getTimeOfDay(1.0F) * ((float)Math.PI * 2F)), -0.25D, 0.25D);
        this.skyDarken = (int)((1.0D - f * d * e) * 11.0D);
    }

    public void setSpawnSettings(boolean spawnMonsters, boolean spawnAnimals) {
        this.getChunkSource().setSpawnSettings(spawnMonsters, spawnAnimals);
    }

    protected void prepareWeather() {
        if (this.levelData.isRaining()) {
            this.rainLevel = 1.0F;
            if (this.levelData.isThundering()) {
                this.thunderLevel = 1.0F;
            }
        }

    }

    @Override
    public void close() throws IOException {
        this.getChunkSource().close();
    }

    @Nullable
    @Override
    public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity except, AABB box, Predicate<? super Entity> predicate) {
        this.getProfiler().incrementCounter("getEntities");
        List<Entity> list = Lists.newArrayList();
        this.getEntities().get(box, (entity) -> {
            if (entity != except && predicate.test(entity)) {
                list.add(entity);
            }

            if (entity instanceof EnderDragon) {
                for(EnderDragonPart enderDragonPart : ((EnderDragon)entity).getSubEntities()) {
                    if (entity != except && predicate.test(enderDragonPart)) {
                        list.add(enderDragonPart);
                    }
                }
            }

        });
        return list;
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> filter, AABB box, Predicate<? super T> predicate) {
        this.getProfiler().incrementCounter("getEntities");
        List<T> list = Lists.newArrayList();
        this.getEntities().get(filter, box, (entity) -> {
            if (predicate.test(entity)) {
                list.add(entity);
            }

            if (entity instanceof EnderDragon) {
                EnderDragon enderDragon = (EnderDragon)entity;

                for(EnderDragonPart enderDragonPart : enderDragon.getSubEntities()) {
                    T entity2 = filter.tryCast(enderDragonPart);
                    if (entity2 != null && predicate.test(entity2)) {
                        list.add(entity2);
                    }
                }
            }

        });
        return list;
    }

    @Nullable
    public abstract Entity getEntity(int id);

    public void blockEntityChanged(BlockPos pos) {
        if (this.hasChunkAt(pos)) {
            this.getChunkAt(pos).setUnsaved(true);
        }

    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    public int getDirectSignalTo(BlockPos pos) {
        int i = 0;
        i = Math.max(i, this.getDirectSignal(pos.below(), Direction.DOWN));
        if (i >= 15) {
            return i;
        } else {
            i = Math.max(i, this.getDirectSignal(pos.above(), Direction.UP));
            if (i >= 15) {
                return i;
            } else {
                i = Math.max(i, this.getDirectSignal(pos.north(), Direction.NORTH));
                if (i >= 15) {
                    return i;
                } else {
                    i = Math.max(i, this.getDirectSignal(pos.south(), Direction.SOUTH));
                    if (i >= 15) {
                        return i;
                    } else {
                        i = Math.max(i, this.getDirectSignal(pos.west(), Direction.WEST));
                        if (i >= 15) {
                            return i;
                        } else {
                            i = Math.max(i, this.getDirectSignal(pos.east(), Direction.EAST));
                            return i >= 15 ? i : i;
                        }
                    }
                }
            }
        }
    }

    public boolean hasSignal(BlockPos pos, Direction direction) {
        return this.getSignal(pos, direction) > 0;
    }

    public int getSignal(BlockPos pos, Direction direction) {
        BlockState blockState = this.getBlockState(pos);
        int i = blockState.getSignal(this, pos, direction);
        return blockState.isRedstoneConductor(this, pos) ? Math.max(i, this.getDirectSignalTo(pos)) : i;
    }

    public boolean hasNeighborSignal(BlockPos pos) {
        if (this.getSignal(pos.below(), Direction.DOWN) > 0) {
            return true;
        } else if (this.getSignal(pos.above(), Direction.UP) > 0) {
            return true;
        } else if (this.getSignal(pos.north(), Direction.NORTH) > 0) {
            return true;
        } else if (this.getSignal(pos.south(), Direction.SOUTH) > 0) {
            return true;
        } else if (this.getSignal(pos.west(), Direction.WEST) > 0) {
            return true;
        } else {
            return this.getSignal(pos.east(), Direction.EAST) > 0;
        }
    }

    public int getBestNeighborSignal(BlockPos pos) {
        int i = 0;

        for(Direction direction : DIRECTIONS) {
            int j = this.getSignal(pos.relative(direction), direction);
            if (j >= 15) {
                return 15;
            }

            if (j > i) {
                i = j;
            }
        }

        return i;
    }

    public void disconnect() {
    }

    public long getGameTime() {
        return this.levelData.getGameTime();
    }

    public long getDayTime() {
        return this.levelData.getDayTime();
    }

    public boolean mayInteract(Player player, BlockPos pos) {
        return true;
    }

    public void broadcastEntityEvent(Entity entity, byte status) {
    }

    public void blockEvent(BlockPos pos, Block block, int type, int data) {
        this.getBlockState(pos).triggerEvent(this, pos, type, data);
    }

    @Override
    public LevelData getLevelData() {
        return this.levelData;
    }

    public GameRules getGameRules() {
        return this.levelData.getGameRules();
    }

    public float getThunderLevel(float delta) {
        return Mth.lerp(delta, this.oThunderLevel, this.thunderLevel) * this.getRainLevel(delta);
    }

    public void setThunderLevel(float thunderGradient) {
        float f = Mth.clamp(thunderGradient, 0.0F, 1.0F);
        this.oThunderLevel = f;
        this.thunderLevel = f;
    }

    public float getRainLevel(float delta) {
        return Mth.lerp(delta, this.oRainLevel, this.rainLevel);
    }

    public void setRainLevel(float rainGradient) {
        float f = Mth.clamp(rainGradient, 0.0F, 1.0F);
        this.oRainLevel = f;
        this.rainLevel = f;
    }

    public boolean isThundering() {
        if (this.dimensionType().hasSkyLight() && !this.dimensionType().hasCeiling()) {
            return (double)this.getThunderLevel(1.0F) > 0.9D;
        } else {
            return false;
        }
    }

    public boolean isRaining() {
        return (double)this.getRainLevel(1.0F) > 0.2D;
    }

    public boolean isRainingAt(BlockPos pos) {
        if (!this.isRaining()) {
            return false;
        } else if (!this.canSeeSky(pos)) {
            return false;
        } else if (this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return false;
        } else {
            Biome biome = this.getBiome(pos).value();
            return biome.getPrecipitation() == Biome.Precipitation.RAIN && biome.warmEnoughToRain(pos);
        }
    }

    public boolean isHumidAt(BlockPos pos) {
        Biome biome = this.getBiome(pos).value();
        return biome.isHumid();
    }

    @Nullable
    public abstract MapItemSavedData getMapData(String id);

    public abstract void setMapData(String id, MapItemSavedData state);

    public abstract int getFreeMapId();

    public void globalLevelEvent(int eventId, BlockPos pos, int data) {
    }

    public CrashReportCategory fillReportDetails(CrashReport report) {
        CrashReportCategory crashReportCategory = report.addCategory("Affected level", 1);
        crashReportCategory.setDetail("All players", () -> {
            return this.players().size() + " total; " + this.players();
        });
        crashReportCategory.setDetail("Chunk stats", this.getChunkSource()::gatherStats);
        crashReportCategory.setDetail("Level dimension", () -> {
            return this.dimension().location().toString();
        });

        try {
            this.levelData.fillCrashReportCategory(crashReportCategory, this);
        } catch (Throwable var4) {
            crashReportCategory.setDetailError("Level Data Unobtainable", var4);
        }

        return crashReportCategory;
    }

    public abstract void destroyBlockProgress(int entityId, BlockPos pos, int progress);

    public void createFireworks(double x, double y, double z, double velocityX, double velocityY, double velocityZ, @Nullable CompoundTag nbt) {
    }

    public abstract Scoreboard getScoreboard();

    public void updateNeighbourForOutputSignal(BlockPos pos, Block block) {
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockPos = pos.relative(direction);
            if (this.hasChunkAt(blockPos)) {
                BlockState blockState = this.getBlockState(blockPos);
                if (blockState.is(Blocks.COMPARATOR)) {
                    blockState.neighborChanged(this, blockPos, block, pos, false);
                } else if (blockState.isRedstoneConductor(this, blockPos)) {
                    blockPos = blockPos.relative(direction);
                    blockState = this.getBlockState(blockPos);
                    if (blockState.is(Blocks.COMPARATOR)) {
                        blockState.neighborChanged(this, blockPos, block, pos, false);
                    }
                }
            }
        }

    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        long l = 0L;
        float f = 0.0F;
        if (this.hasChunkAt(pos)) {
            f = this.getMoonBrightness();
            l = this.getChunkAt(pos).getInhabitedTime();
        }

        return new DifficultyInstance(this.getDifficulty(), this.getDayTime(), l, f);
    }

    @Override
    public int getSkyDarken() {
        return this.skyDarken;
    }

    public void setSkyFlashTime(int lightningTicksLeft) {
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.worldBorder;
    }

    public void sendPacketToServer(Packet<?> packet) {
        throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
    }

    @Override
    public DimensionType dimensionType() {
        return this.dimensionType;
    }

    public Holder<DimensionType> dimensionTypeRegistration() {
        return this.dimensionTypeRegistration;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    @Override
    public Random getRandom() {
        return this.random;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> state) {
        return state.test(this.getBlockState(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> state) {
        return state.test(this.getFluidState(pos));
    }

    public abstract RecipeManager getRecipeManager();

    public BlockPos getBlockRandomPos(int x, int y, int z, int i) {
        this.randValue = this.randValue * 3 + 1013904223;
        int j = this.randValue >> 2;
        return new BlockPos(x + (j & 15), y + (j >> 16 & i), z + (j >> 8 & 15));
    }

    public boolean noSave() {
        return false;
    }

    public ProfilerFiller getProfiler() {
        return this.profiler.get();
    }

    public Supplier<ProfilerFiller> getProfilerSupplier() {
        return this.profiler;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    public final boolean isDebug() {
        return this.isDebug;
    }

    public abstract LevelEntityGetter<Entity> getEntities();

    protected void postGameEventInRadius(@Nullable Entity entity, GameEvent gameEvent, BlockPos pos, int range) {
        int i = SectionPos.blockToSectionCoord(pos.getX() - range);
        int j = SectionPos.blockToSectionCoord(pos.getZ() - range);
        int k = SectionPos.blockToSectionCoord(pos.getX() + range);
        int l = SectionPos.blockToSectionCoord(pos.getZ() + range);
        int m = SectionPos.blockToSectionCoord(pos.getY() - range);
        int n = SectionPos.blockToSectionCoord(pos.getY() + range);

        for(int o = i; o <= k; ++o) {
            for(int p = j; p <= l; ++p) {
                ChunkAccess chunkAccess = this.getChunkSource().getChunkNow(o, p);
                if (chunkAccess != null) {
                    for(int q = m; q <= n; ++q) {
                        chunkAccess.getEventDispatcher(q).post(gameEvent, entity, pos);
                    }
                }
            }
        }

    }

    @Override
    public long nextSubTickCount() {
        return (long)(this.subTickCount++);
    }
}
