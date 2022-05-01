package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class ChunkStatus {
    public static final int MAX_STRUCTURE_DISTANCE = 8;
    private static final EnumSet<Heightmap.Types> PRE_FEATURES = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);
    public static final EnumSet<Heightmap.Types> POST_FEATURES = EnumSet.of(Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE, Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
    private static final ChunkStatus.LoadingTask PASSTHROUGH_LOAD_TASK = (targetStatus, world, structureManager, lightingProvider, function, chunk) -> {
        if (chunk instanceof ProtoChunk) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            if (!chunk.getStatus().isOrAfter(targetStatus)) {
                protoChunk.setStatus(targetStatus);
            }
        }

        return CompletableFuture.completedFuture(Either.left(chunk));
    };
    public static final ChunkStatus EMPTY = registerSimple("empty", (ChunkStatus)null, -1, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, world, generator, chunks, chunk) -> {
    });
    public static final ChunkStatus STRUCTURE_STARTS = register("structure_starts", EMPTY, 0, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, executor, world, generator, structureManager, lightingProvider, function, chunks, chunk, bl) -> {
        if (!chunk.getStatus().isOrAfter(targetStatus)) {
            if (world.getServer().getWorldData().worldGenSettings().generateFeatures()) {
                generator.createStructures(world.registryAccess(), world.structureFeatureManager(), chunk, structureManager, world.getSeed());
            }

            if (chunk instanceof ProtoChunk) {
                ProtoChunk protoChunk = (ProtoChunk)chunk;
                protoChunk.setStatus(targetStatus);
            }

            world.onStructureStartsAvailable(chunk);
        }

        return CompletableFuture.completedFuture(Either.left(chunk));
    }, (targetStatus, world, structureManager, lightingProvider, function, chunk) -> {
        if (!chunk.getStatus().isOrAfter(targetStatus)) {
            if (chunk instanceof ProtoChunk) {
                ProtoChunk protoChunk = (ProtoChunk)chunk;
                protoChunk.setStatus(targetStatus);
            }

            world.onStructureStartsAvailable(chunk);
        }

        return CompletableFuture.completedFuture(Either.left(chunk));
    });
    public static final ChunkStatus STRUCTURE_REFERENCES = registerSimple("structure_references", STRUCTURE_STARTS, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, world, generator, chunks, chunk) -> {
        WorldGenRegion worldGenRegion = new WorldGenRegion(world, chunks, targetStatus, -1);
        generator.createReferences(worldGenRegion, world.structureFeatureManager().forWorldGenRegion(worldGenRegion), chunk);
    });
    public static final ChunkStatus BIOMES = register("biomes", STRUCTURE_REFERENCES, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, executor, world, generator, structureManager, lightingProvider, function, chunks, chunk, bl) -> {
        if (!bl && chunk.getStatus().isOrAfter(targetStatus)) {
            return CompletableFuture.completedFuture(Either.left(chunk));
        } else {
            WorldGenRegion worldGenRegion = new WorldGenRegion(world, chunks, targetStatus, -1);
            return generator.createBiomes(world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), executor, Blender.of(worldGenRegion), world.structureFeatureManager().forWorldGenRegion(worldGenRegion), chunk).thenApply((chunkx) -> {
                if (chunkx instanceof ProtoChunk) {
                    ((ProtoChunk)chunkx).setStatus(targetStatus);
                }

                return Either.left(chunkx);
            });
        }
    });
    public static final ChunkStatus NOISE = register("noise", BIOMES, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, executor, world, generator, structureManager, lightingProvider, function, chunks, chunk, bl) -> {
        if (!bl && chunk.getStatus().isOrAfter(targetStatus)) {
            return CompletableFuture.completedFuture(Either.left(chunk));
        } else {
            WorldGenRegion worldGenRegion = new WorldGenRegion(world, chunks, targetStatus, 0);
            return generator.fillFromNoise(executor, Blender.of(worldGenRegion), world.structureFeatureManager().forWorldGenRegion(worldGenRegion), chunk).thenApply((chunkx) -> {
                if (chunkx instanceof ProtoChunk) {
                    ProtoChunk protoChunk = (ProtoChunk)chunkx;
                    BelowZeroRetrogen belowZeroRetrogen = protoChunk.getBelowZeroRetrogen();
                    if (belowZeroRetrogen != null) {
                        BelowZeroRetrogen.replaceOldBedrock(protoChunk);
                        if (belowZeroRetrogen.hasBedrockHoles()) {
                            belowZeroRetrogen.applyBedrockMask(protoChunk);
                        }
                    }

                    protoChunk.setStatus(targetStatus);
                }

                return Either.left(chunkx);
            });
        }
    });
    public static final ChunkStatus SURFACE = registerSimple("surface", NOISE, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, world, generator, chunks, chunk) -> {
        WorldGenRegion worldGenRegion = new WorldGenRegion(world, chunks, targetStatus, 0);
        generator.buildSurface(worldGenRegion, world.structureFeatureManager().forWorldGenRegion(worldGenRegion), chunk);
    });
    public static final ChunkStatus CARVERS = registerSimple("carvers", SURFACE, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, world, generator, chunks, chunk) -> {
        WorldGenRegion worldGenRegion = new WorldGenRegion(world, chunks, targetStatus, 0);
        if (chunk instanceof ProtoChunk) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            Blender.addAroundOldChunksCarvingMaskFilter(worldGenRegion, protoChunk);
        }

        generator.applyCarvers(worldGenRegion, world.getSeed(), world.getBiomeManager(), world.structureFeatureManager().forWorldGenRegion(worldGenRegion), chunk, GenerationStep.Carving.AIR);
    });
    public static final ChunkStatus LIQUID_CARVERS = registerSimple("liquid_carvers", CARVERS, 8, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, world, generator, chunks, chunk) -> {
    });
    public static final ChunkStatus FEATURES = register("features", LIQUID_CARVERS, 8, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, executor, world, generator, structureManager, lightingProvider, function, chunks, chunk, bl) -> {
        ProtoChunk protoChunk = (ProtoChunk)chunk;
        protoChunk.setLightEngine(lightingProvider);
        if (bl || !chunk.getStatus().isOrAfter(targetStatus)) {
            Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE));
            WorldGenRegion worldGenRegion = new WorldGenRegion(world, chunks, targetStatus, 1);
            generator.applyBiomeDecoration(worldGenRegion, chunk, world.structureFeatureManager().forWorldGenRegion(worldGenRegion));
            Blender.generateBorderTicks(worldGenRegion, chunk);
            protoChunk.setStatus(targetStatus);
        }

        return CompletableFuture.completedFuture(Either.left(chunk));
    });
    public static final ChunkStatus LIGHT = register("light", FEATURES, 1, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, executor, world, generator, structureManager, lightingProvider, function, chunks, chunk, bl) -> {
        return lightChunk(targetStatus, lightingProvider, chunk);
    }, (targetStatus, world, structureManager, lightingProvider, function, chunk) -> {
        return lightChunk(targetStatus, lightingProvider, chunk);
    });
    public static final ChunkStatus SPAWN = registerSimple("spawn", LIGHT, 0, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, world, generator, chunks, chunk) -> {
        if (!chunk.isUpgrading()) {
            generator.spawnOriginalMobs(new WorldGenRegion(world, chunks, targetStatus, -1));
        }

    });
    public static final ChunkStatus HEIGHTMAPS = registerSimple("heightmaps", SPAWN, 0, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (targetStatus, world, generator, chunks, chunk) -> {
    });
    public static final ChunkStatus FULL = register("full", HEIGHTMAPS, 0, POST_FEATURES, ChunkStatus.ChunkType.LEVELCHUNK, (targetStatus, executor, world, generator, structureManager, lightingProvider, function, chunks, chunk, bl) -> {
        return function.apply(chunk);
    }, (targetStatus, world, structureManager, lightingProvider, function, chunk) -> {
        return function.apply(chunk);
    });
    private static final List<ChunkStatus> STATUS_BY_RANGE = ImmutableList.of(FULL, FEATURES, LIQUID_CARVERS, BIOMES, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS);
    private static final IntList RANGE_BY_STATUS = Util.make(new IntArrayList(getStatusList().size()), (intArrayList) -> {
        int i = 0;

        for(int j = getStatusList().size() - 1; j >= 0; --j) {
            while(i + 1 < STATUS_BY_RANGE.size() && j <= STATUS_BY_RANGE.get(i + 1).getIndex()) {
                ++i;
            }

            intArrayList.add(0, i);
        }

    });
    private final String name;
    private final int index;
    private final ChunkStatus parent;
    private final ChunkStatus.GenerationTask generationTask;
    private final ChunkStatus.LoadingTask loadingTask;
    private final int range;
    private final ChunkStatus.ChunkType chunkType;
    private final EnumSet<Heightmap.Types> heightmapsAfter;

    private static CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> lightChunk(ChunkStatus status, ThreadedLevelLightEngine lightingProvider, ChunkAccess chunk) {
        boolean bl = isLighted(status, chunk);
        if (!chunk.getStatus().isOrAfter(status)) {
            ((ProtoChunk)chunk).setStatus(status);
        }

        return lightingProvider.lightChunk(chunk, bl).thenApply(Either::left);
    }

    private static ChunkStatus registerSimple(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<Heightmap.Types> heightMapTypes, ChunkStatus.ChunkType chunkType, ChunkStatus.SimpleGenerationTask task) {
        return register(id, previous, taskMargin, heightMapTypes, chunkType, task);
    }

    private static ChunkStatus register(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<Heightmap.Types> heightMapTypes, ChunkStatus.ChunkType chunkType, ChunkStatus.GenerationTask task) {
        return register(id, previous, taskMargin, heightMapTypes, chunkType, task, PASSTHROUGH_LOAD_TASK);
    }

    private static ChunkStatus register(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<Heightmap.Types> heightMapTypes, ChunkStatus.ChunkType chunkType, ChunkStatus.GenerationTask task, ChunkStatus.LoadingTask loadTask) {
        return Registry.register(Registry.CHUNK_STATUS, id, new ChunkStatus(id, previous, taskMargin, heightMapTypes, chunkType, task, loadTask));
    }

    public static List<ChunkStatus> getStatusList() {
        List<ChunkStatus> list = Lists.newArrayList();

        ChunkStatus chunkStatus;
        for(chunkStatus = FULL; chunkStatus.getParent() != chunkStatus; chunkStatus = chunkStatus.getParent()) {
            list.add(chunkStatus);
        }

        list.add(chunkStatus);
        Collections.reverse(list);
        return list;
    }

    private static boolean isLighted(ChunkStatus status, ChunkAccess chunk) {
        return chunk.getStatus().isOrAfter(status) && chunk.isLightCorrect();
    }

    public static ChunkStatus getStatusAroundFullChunk(int level) {
        if (level >= STATUS_BY_RANGE.size()) {
            return EMPTY;
        } else {
            return level < 0 ? FULL : STATUS_BY_RANGE.get(level);
        }
    }

    public static int maxDistance() {
        return STATUS_BY_RANGE.size();
    }

    public static int getDistance(ChunkStatus status) {
        return RANGE_BY_STATUS.getInt(status.getIndex());
    }

    ChunkStatus(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<Heightmap.Types> heightMapTypes, ChunkStatus.ChunkType chunkType, ChunkStatus.GenerationTask generationTask, ChunkStatus.LoadingTask loadTask) {
        this.name = id;
        this.parent = previous == null ? this : previous;
        this.generationTask = generationTask;
        this.loadingTask = loadTask;
        this.range = taskMargin;
        this.chunkType = chunkType;
        this.heightmapsAfter = heightMapTypes;
        this.index = previous == null ? 0 : previous.getIndex() + 1;
    }

    public int getIndex() {
        return this.index;
    }

    public String getName() {
        return this.name;
    }

    public ChunkStatus getParent() {
        return this.parent;
    }

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> generate(Executor executor, ServerLevel world, ChunkGenerator generator, StructureManager structureManager, ThreadedLevelLightEngine lightingProvider, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> fullChunkConverter, List<ChunkAccess> chunks, boolean bl) {
        ChunkAccess chunkAccess = chunks.get(chunks.size() / 2);
        ProfiledDuration profiledDuration = JvmProfiler.INSTANCE.onChunkGenerate(chunkAccess.getPos(), world.dimension(), this.name);
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture = this.generationTask.doWork(this, executor, world, generator, structureManager, lightingProvider, fullChunkConverter, chunks, chunkAccess, bl);
        return profiledDuration != null ? completableFuture.thenApply((either) -> {
            profiledDuration.finish();
            return either;
        }) : completableFuture;
    }

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> load(ServerLevel world, StructureManager structureManager, ThreadedLevelLightEngine lightingProvider, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> function, ChunkAccess chunk) {
        return this.loadingTask.doWork(this, world, structureManager, lightingProvider, function, chunk);
    }

    public int getRange() {
        return this.range;
    }

    public ChunkStatus.ChunkType getChunkType() {
        return this.chunkType;
    }

    public static ChunkStatus byName(String id) {
        return Registry.CHUNK_STATUS.get(ResourceLocation.tryParse(id));
    }

    public EnumSet<Heightmap.Types> heightmapsAfter() {
        return this.heightmapsAfter;
    }

    public boolean isOrAfter(ChunkStatus chunkStatus) {
        return this.getIndex() >= chunkStatus.getIndex();
    }

    @Override
    public String toString() {
        return Registry.CHUNK_STATUS.getKey(this).toString();
    }

    public static enum ChunkType {
        PROTOCHUNK,
        LEVELCHUNK;
    }

    interface GenerationTask {
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(ChunkStatus targetStatus, Executor executor, ServerLevel world, ChunkGenerator generator, StructureManager structureManager, ThreadedLevelLightEngine lightingProvider, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> fullChunkConverter, List<ChunkAccess> chunks, ChunkAccess chunk, boolean bl);
    }

    interface LoadingTask {
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(ChunkStatus targetStatus, ServerLevel world, StructureManager structureManager, ThreadedLevelLightEngine lightingProvider, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> fullChunkConverter, ChunkAccess chunk);
    }

    interface SimpleGenerationTask extends ChunkStatus.GenerationTask {
        @Override
        default CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(ChunkStatus targetStatus, Executor executor, ServerLevel world, ChunkGenerator generator, StructureManager structureManager, ThreadedLevelLightEngine lightingProvider, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> fullChunkConverter, List<ChunkAccess> chunks, ChunkAccess chunk, boolean bl) {
            if (bl || !chunk.getStatus().isOrAfter(targetStatus)) {
                this.doWork(targetStatus, world, generator, chunks, chunk);
                if (chunk instanceof ProtoChunk) {
                    ProtoChunk protoChunk = (ProtoChunk)chunk;
                    protoChunk.setStatus(targetStatus);
                }
            }

            return CompletableFuture.completedFuture(Either.left(chunk));
        }

        void doWork(ChunkStatus targetStatus, ServerLevel world, ChunkGenerator chunkGenerator, List<ChunkAccess> chunks, ChunkAccess chunk);
    }
}
