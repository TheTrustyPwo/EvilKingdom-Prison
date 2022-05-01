package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.WorldGenTickAccess;
import org.slf4j.Logger;

public class WorldGenRegion implements WorldGenLevel {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<ChunkAccess> cache;
    private final ChunkAccess center;
    private final int size;
    private final ServerLevel level;
    private final long seed;
    private final LevelData levelData;
    private final Random random;
    private final DimensionType dimensionType;
    private final WorldGenTickAccess<Block> blockTicks = new WorldGenTickAccess<>((pos) -> {
        return this.getChunk(pos).getBlockTicks();
    });
    private final WorldGenTickAccess<Fluid> fluidTicks = new WorldGenTickAccess<>((pos) -> {
        return this.getChunk(pos).getFluidTicks();
    });
    private final BiomeManager biomeManager;
    private final ChunkPos firstPos;
    private final ChunkPos lastPos;
    private final StructureFeatureManager structureFeatureManager;
    private final ChunkStatus generatingStatus;
    private final int writeRadiusCutoff;
    @Nullable
    private Supplier<String> currentlyGenerating;
    private final AtomicLong subTickCount = new AtomicLong();

    public WorldGenRegion(ServerLevel world, List<ChunkAccess> chunks, ChunkStatus status, int placementRadius) {
        this.generatingStatus = status;
        this.writeRadiusCutoff = placementRadius;
        int i = Mth.floor(Math.sqrt((double)chunks.size()));
        if (i * i != chunks.size()) {
            throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Cache size is not a square."));
        } else {
            this.cache = chunks;
            this.center = chunks.get(chunks.size() / 2);
            this.size = i;
            this.level = world;
            this.seed = world.getSeed();
            this.levelData = world.getLevelData();
            this.random = world.getRandom();
            this.dimensionType = world.dimensionType();
            this.biomeManager = new BiomeManager(this, BiomeManager.obfuscateSeed(this.seed));
            this.firstPos = chunks.get(0).getPos();
            this.lastPos = chunks.get(chunks.size() - 1).getPos();
            this.structureFeatureManager = world.structureFeatureManager().forWorldGenRegion(this);
        }
    }

    public ChunkPos getCenter() {
        return this.center.getPos();
    }

    @Override
    public void setCurrentlyGenerating(@Nullable Supplier<String> structureName) {
        this.currentlyGenerating = structureName;
    }

    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY);
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        ChunkAccess chunkAccess;
        if (this.hasChunk(chunkX, chunkZ)) {
            int i = chunkX - this.firstPos.x;
            int j = chunkZ - this.firstPos.z;
            chunkAccess = this.cache.get(i + j * this.size);
            if (chunkAccess.getStatus().isOrAfter(leastStatus)) {
                return chunkAccess;
            }
        } else {
            chunkAccess = null;
        }

        if (!create) {
            return null;
        } else {
            LOGGER.error("Requested chunk : {} {}", chunkX, chunkZ);
            LOGGER.error("Region bounds : {} {} | {} {}", this.firstPos.x, this.firstPos.z, this.lastPos.x, this.lastPos.z);
            if (chunkAccess != null) {
                throw (RuntimeException)Util.pauseInIde(new RuntimeException(String.format("Chunk is not of correct status. Expecting %s, got %s | %s %s", leastStatus, chunkAccess.getStatus(), chunkX, chunkZ)));
            } else {
                throw (RuntimeException)Util.pauseInIde(new RuntimeException(String.format("We are asking a region for a chunk out of bound | %s %s", chunkX, chunkZ)));
            }
        }
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return chunkX >= this.firstPos.x && chunkX <= this.lastPos.x && chunkZ >= this.firstPos.z && chunkZ <= this.lastPos.z;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())).getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getChunk(pos).getFluidState(pos);
    }

    @Nullable
    @Override
    public Player getNearestPlayer(double x, double y, double z, double maxDistance, Predicate<Entity> targetPredicate) {
        return null;
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return this.level.getUncachedNoiseBiome(biomeX, biomeY, biomeZ);
    }

    @Override
    public float getShade(Direction direction, boolean shaded) {
        return 1.0F;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
        BlockState blockState = this.getBlockState(pos);
        if (blockState.isAir()) {
            return false;
        } else {
            if (drop) {
                BlockEntity blockEntity = blockState.hasBlockEntity() ? this.getBlockEntity(pos) : null;
                Block.dropResources(blockState, this.level, pos, blockEntity, breakingEntity, ItemStack.EMPTY);
            }

            return this.setBlock(pos, Blocks.AIR.defaultBlockState(), 3, maxUpdateDepth);
        }
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        ChunkAccess chunkAccess = this.getChunk(pos);
        BlockEntity blockEntity = chunkAccess.getBlockEntity(pos);
        if (blockEntity != null) {
            return blockEntity;
        } else {
            CompoundTag compoundTag = chunkAccess.getBlockEntityNbt(pos);
            BlockState blockState = chunkAccess.getBlockState(pos);
            if (compoundTag != null) {
                if ("DUMMY".equals(compoundTag.getString("id"))) {
                    if (!blockState.hasBlockEntity()) {
                        return null;
                    }

                    blockEntity = ((EntityBlock)blockState.getBlock()).newBlockEntity(pos, blockState);
                } else {
                    blockEntity = BlockEntity.loadStatic(pos, blockState, compoundTag);
                }

                if (blockEntity != null) {
                    chunkAccess.setBlockEntity(blockEntity);
                    return blockEntity;
                }
            }

            if (blockState.hasBlockEntity()) {
                LOGGER.warn("Tried to access a block entity before it was created. {}", (Object)pos);
            }

            return null;
        }
    }

    @Override
    public boolean ensureCanWrite(BlockPos pos) {
        int i = SectionPos.blockToSectionCoord(pos.getX());
        int j = SectionPos.blockToSectionCoord(pos.getZ());
        ChunkPos chunkPos = this.getCenter();
        int k = Math.abs(chunkPos.x - i);
        int l = Math.abs(chunkPos.z - j);
        if (k <= this.writeRadiusCutoff && l <= this.writeRadiusCutoff) {
            if (this.center.isUpgrading()) {
                LevelHeightAccessor levelHeightAccessor = this.center.getHeightAccessorForGeneration();
                if (pos.getY() < levelHeightAccessor.getMinBuildHeight() || pos.getY() >= levelHeightAccessor.getMaxBuildHeight()) {
                    return false;
                }
            }

            return true;
        } else {
            Util.logAndPauseIfInIde("Detected setBlock in a far chunk [" + i + ", " + j + "], pos: " + pos + ", status: " + this.generatingStatus + (this.currentlyGenerating == null ? "" : ", currently generating: " + (String)this.currentlyGenerating.get()));
            return false;
        }
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
        if (!this.ensureCanWrite(pos)) {
            return false;
        } else {
            ChunkAccess chunkAccess = this.getChunk(pos);
            BlockState blockState = chunkAccess.setBlockState(pos, state, false);
            if (blockState != null) {
                this.level.onBlockStateChange(pos, blockState, state);
            }

            if (state.hasBlockEntity()) {
                if (chunkAccess.getStatus().getChunkType() == ChunkStatus.ChunkType.LEVELCHUNK) {
                    BlockEntity blockEntity = ((EntityBlock)state.getBlock()).newBlockEntity(pos, state);
                    if (blockEntity != null) {
                        chunkAccess.setBlockEntity(blockEntity);
                    } else {
                        chunkAccess.removeBlockEntity(pos);
                    }
                } else {
                    CompoundTag compoundTag = new CompoundTag();
                    compoundTag.putInt("x", pos.getX());
                    compoundTag.putInt("y", pos.getY());
                    compoundTag.putInt("z", pos.getZ());
                    compoundTag.putString("id", "DUMMY");
                    chunkAccess.setBlockEntityNbt(compoundTag);
                }
            } else if (blockState != null && blockState.hasBlockEntity()) {
                chunkAccess.removeBlockEntity(pos);
            }

            if (state.hasPostProcess(this, pos)) {
                this.markPosForPostprocessing(pos);
            }

            return true;
        }
    }

    private void markPosForPostprocessing(BlockPos pos) {
        this.getChunk(pos).markPosForPostprocessing(pos);
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        int i = SectionPos.blockToSectionCoord(entity.getBlockX());
        int j = SectionPos.blockToSectionCoord(entity.getBlockZ());
        this.getChunk(i, j).addEntity(entity);
        return true;
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean move) {
        return this.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.level.getWorldBorder();
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    /** @deprecated */
    @Deprecated
    @Override
    public ServerLevel getLevel() {
        return this.level;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }

    @Override
    public LevelData getLevelData() {
        return this.levelData;
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        if (!this.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
            throw new RuntimeException("We are asking a region for a chunk out of bound");
        } else {
            return new DifficultyInstance(this.level.getDifficulty(), this.level.getDayTime(), 0L, this.level.getMoonBrightness());
        }
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return this.level.getServer();
    }

    @Override
    public ChunkSource getChunkSource() {
        return this.level.getChunkSource();
    }

    @Override
    public long getSeed() {
        return this.seed;
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public int getSeaLevel() {
        return this.level.getSeaLevel();
    }

    @Override
    public Random getRandom() {
        return this.random;
    }

    @Override
    public int getHeight(Heightmap.Types heightmap, int x, int z) {
        return this.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)).getHeight(heightmap, x & 15, z & 15) + 1;
    }

    @Override
    public void playSound(@Nullable Player player, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch) {
    }

    @Override
    public void addParticle(ParticleOptions parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
    }

    @Override
    public void levelEvent(@Nullable Player player, int eventId, BlockPos pos, int data) {
    }

    @Override
    public void gameEvent(@Nullable Entity entity, GameEvent event, BlockPos pos) {
    }

    @Override
    public DimensionType dimensionType() {
        return this.dimensionType;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> state) {
        return state.test(this.getBlockState(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> state) {
        return state.test(this.getFluidState(pos));
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> filter, AABB box, Predicate<? super T> predicate) {
        return Collections.emptyList();
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity except, AABB box, @Nullable Predicate<? super Entity> predicate) {
        return Collections.emptyList();
    }

    @Override
    public List<Player> players() {
        return Collections.emptyList();
    }

    @Override
    public int getMinBuildHeight() {
        return this.level.getMinBuildHeight();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    @Override
    public long nextSubTickCount() {
        return this.subTickCount.getAndIncrement();
    }
}
