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
    private static final ChunkStatus.LoadingTask PASSTHROUGH_LOAD_TASK = (chunkstatus, worldserver, definedstructuremanager, lightenginethreaded, function, ichunkaccess) -> {
        if (ichunkaccess instanceof ProtoChunk) {
            ProtoChunk protochunk = (ProtoChunk) ichunkaccess;

            if (!ichunkaccess.getStatus().isOrAfter(chunkstatus)) {
                protochunk.setStatus(chunkstatus);
            }
        }

        return CompletableFuture.completedFuture(Either.left(ichunkaccess));
    };
    public static final ChunkStatus EMPTY = ChunkStatus.registerSimple("empty", (ChunkStatus) null, -1, ChunkStatus.PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, worldserver, chunkgenerator, list, ichunkaccess) -> {
    });
    public static final ChunkStatus STRUCTURE_STARTS = ChunkStatus.register("structure_starts", ChunkStatus.EMPTY, 0, ChunkStatus.PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, executor, worldserver, chunkgenerator, definedstructuremanager, lightenginethreaded, function, list, ichunkaccess, flag) -> {
        if (!ichunkaccess.getStatus().isOrAfter(chunkstatus)) {
            if (worldserver.serverLevelData.worldGenSettings().generateFeatures()) { // CraftBukkit
                chunkgenerator.createStructures(worldserver.registryAccess(), worldserver.structureFeatureManager(), ichunkaccess, definedstructuremanager, worldserver.getSeed());
            }

            if (ichunkaccess instanceof ProtoChunk) {
                ProtoChunk protochunk = (ProtoChunk) ichunkaccess;

                protochunk.setStatus(chunkstatus);
            }

            worldserver.onStructureStartsAvailable(ichunkaccess);
        }

        return CompletableFuture.completedFuture(Either.left(ichunkaccess));
    }, (chunkstatus, worldserver, definedstructuremanager, lightenginethreaded, function, ichunkaccess) -> {
        if (!ichunkaccess.getStatus().isOrAfter(chunkstatus)) {
            if (ichunkaccess instanceof ProtoChunk) {
                ProtoChunk protochunk = (ProtoChunk) ichunkaccess;

                protochunk.setStatus(chunkstatus);
            }

            worldserver.onStructureStartsAvailable(ichunkaccess);
        }

        return CompletableFuture.completedFuture(Either.left(ichunkaccess));
    });
    public static final ChunkStatus STRUCTURE_REFERENCES = ChunkStatus.registerSimple("structure_references", ChunkStatus.STRUCTURE_STARTS, 8, ChunkStatus.PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, worldserver, chunkgenerator, list, ichunkaccess) -> {
        WorldGenRegion regionlimitedworldaccess = new WorldGenRegion(worldserver, list, chunkstatus, -1);

        chunkgenerator.createReferences(regionlimitedworldaccess, worldserver.structureFeatureManager().forWorldGenRegion(regionlimitedworldaccess), ichunkaccess);
    });
    public static final ChunkStatus BIOMES = ChunkStatus.register("biomes", ChunkStatus.STRUCTURE_REFERENCES, 8, ChunkStatus.PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, executor, worldserver, chunkgenerator, definedstructuremanager, lightenginethreaded, function, list, ichunkaccess, flag) -> {
        if (!flag && ichunkaccess.getStatus().isOrAfter(chunkstatus)) {
            return CompletableFuture.completedFuture(Either.left(ichunkaccess));
        } else {
            WorldGenRegion regionlimitedworldaccess = new WorldGenRegion(worldserver, list, chunkstatus, -1);

            return chunkgenerator.createBiomes(worldserver.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), executor, Blender.of(regionlimitedworldaccess), worldserver.structureFeatureManager().forWorldGenRegion(regionlimitedworldaccess), ichunkaccess).thenApply((ichunkaccess1) -> {
                if (ichunkaccess1 instanceof ProtoChunk) {
                    ((ProtoChunk) ichunkaccess1).setStatus(chunkstatus);
                }

                return Either.left(ichunkaccess1);
            });
        }
    });
    public static final ChunkStatus NOISE = ChunkStatus.register("noise", ChunkStatus.BIOMES, 8, ChunkStatus.PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, executor, worldserver, chunkgenerator, definedstructuremanager, lightenginethreaded, function, list, ichunkaccess, flag) -> {
        if (!flag && ichunkaccess.getStatus().isOrAfter(chunkstatus)) {
            return CompletableFuture.completedFuture(Either.left(ichunkaccess));
        } else {
            WorldGenRegion regionlimitedworldaccess = new WorldGenRegion(worldserver, list, chunkstatus, 0);

            return chunkgenerator.fillFromNoise(executor, Blender.of(regionlimitedworldaccess), worldserver.structureFeatureManager().forWorldGenRegion(regionlimitedworldaccess), ichunkaccess).thenApply((ichunkaccess1) -> {
                if (ichunkaccess1 instanceof ProtoChunk) {
                    ProtoChunk protochunk = (ProtoChunk) ichunkaccess1;
                    BelowZeroRetrogen belowzeroretrogen = protochunk.getBelowZeroRetrogen();

                    if (belowzeroretrogen != null) {
                        BelowZeroRetrogen.replaceOldBedrock(protochunk);
                        if (belowzeroretrogen.hasBedrockHoles()) {
                            belowzeroretrogen.applyBedrockMask(protochunk);
                        }
                    }

                    protochunk.setStatus(chunkstatus);
                }

                return Either.left(ichunkaccess1);
            });
        }
    });
    public static final ChunkStatus SURFACE = ChunkStatus.registerSimple("surface", ChunkStatus.NOISE, 8, ChunkStatus.PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, worldserver, chunkgenerator, list, ichunkaccess) -> {
        WorldGenRegion regionlimitedworldaccess = new WorldGenRegion(worldserver, list, chunkstatus, 0);

        chunkgenerator.buildSurface(regionlimitedworldaccess, worldserver.structureFeatureManager().forWorldGenRegion(regionlimitedworldaccess), ichunkaccess);
    });
    public static final ChunkStatus CARVERS = ChunkStatus.registerSimple("carvers", ChunkStatus.SURFACE, 8, ChunkStatus.PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, worldserver, chunkgenerator, list, ichunkaccess) -> {
        WorldGenRegion regionlimitedworldaccess = new WorldGenRegion(worldserver, list, chunkstatus, 0);

        if (ichunkaccess instanceof ProtoChunk) {
            ProtoChunk protochunk = (ProtoChunk) ichunkaccess;

            Blender.addAroundOldChunksCarvingMaskFilter(regionlimitedworldaccess, protochunk);
        }

        chunkgenerator.applyCarvers(regionlimitedworldaccess, worldserver.getSeed(), worldserver.getBiomeManager(), worldserver.structureFeatureManager().forWorldGenRegion(regionlimitedworldaccess), ichunkaccess, GenerationStep.Carving.AIR);
    });
    public static final ChunkStatus LIQUID_CARVERS = ChunkStatus.registerSimple("liquid_carvers", ChunkStatus.CARVERS, 8, ChunkStatus.POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, worldserver, chunkgenerator, list, ichunkaccess) -> {
    });
    public static final ChunkStatus FEATURES = ChunkStatus.register("features", ChunkStatus.LIQUID_CARVERS, 8, ChunkStatus.POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, executor, worldserver, chunkgenerator, definedstructuremanager, lightenginethreaded, function, list, ichunkaccess, flag) -> {
        ProtoChunk protochunk = (ProtoChunk) ichunkaccess;

        protochunk.setLightEngine(lightenginethreaded);
        if (flag || !ichunkaccess.getStatus().isOrAfter(chunkstatus)) {
            Heightmap.primeHeightmaps(ichunkaccess, EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE));
            WorldGenRegion regionlimitedworldaccess = new WorldGenRegion(worldserver, list, chunkstatus, 1);

            chunkgenerator.applyBiomeDecoration(regionlimitedworldaccess, ichunkaccess, worldserver.structureFeatureManager().forWorldGenRegion(regionlimitedworldaccess));
            Blender.generateBorderTicks(regionlimitedworldaccess, ichunkaccess);
            protochunk.setStatus(chunkstatus);
        }

        return CompletableFuture.completedFuture(Either.left(ichunkaccess));
    });
    public static final ChunkStatus LIGHT = ChunkStatus.register("light", ChunkStatus.FEATURES, 1, ChunkStatus.POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, executor, worldserver, chunkgenerator, definedstructuremanager, lightenginethreaded, function, list, ichunkaccess, flag) -> {
        return ChunkStatus.lightChunk(chunkstatus, lightenginethreaded, ichunkaccess);
    }, (chunkstatus, worldserver, definedstructuremanager, lightenginethreaded, function, ichunkaccess) -> {
        return ChunkStatus.lightChunk(chunkstatus, lightenginethreaded, ichunkaccess);
    });
    public static final ChunkStatus SPAWN = ChunkStatus.registerSimple("spawn", ChunkStatus.LIGHT, 0, ChunkStatus.POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, worldserver, chunkgenerator, list, ichunkaccess) -> {
        if (!ichunkaccess.isUpgrading()) {
            chunkgenerator.spawnOriginalMobs(new WorldGenRegion(worldserver, list, chunkstatus, -1));
        }

    });
    public static final ChunkStatus HEIGHTMAPS = ChunkStatus.registerSimple("heightmaps", ChunkStatus.SPAWN, 0, ChunkStatus.POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkstatus, worldserver, chunkgenerator, list, ichunkaccess) -> {
    });
    public static final ChunkStatus FULL = ChunkStatus.register("full", ChunkStatus.HEIGHTMAPS, 0, ChunkStatus.POST_FEATURES, ChunkStatus.ChunkType.LEVELCHUNK, (chunkstatus, executor, worldserver, chunkgenerator, definedstructuremanager, lightenginethreaded, function, list, ichunkaccess, flag) -> {
        return (CompletableFuture) function.apply(ichunkaccess);
    }, (chunkstatus, worldserver, definedstructuremanager, lightenginethreaded, function, ichunkaccess) -> {
        return (CompletableFuture) function.apply(ichunkaccess);
    });
    private static final List<ChunkStatus> STATUS_BY_RANGE = ImmutableList.of(ChunkStatus.FULL, ChunkStatus.FEATURES, ChunkStatus.LIQUID_CARVERS, ChunkStatus.BIOMES, ChunkStatus.STRUCTURE_STARTS, ChunkStatus.STRUCTURE_STARTS, ChunkStatus.STRUCTURE_STARTS, ChunkStatus.STRUCTURE_STARTS, ChunkStatus.STRUCTURE_STARTS, ChunkStatus.STRUCTURE_STARTS, ChunkStatus.STRUCTURE_STARTS, ChunkStatus.STRUCTURE_STARTS, new ChunkStatus[0]);
    private static final IntList RANGE_BY_STATUS = (IntList) Util.make(new IntArrayList(ChunkStatus.getStatusList().size()), (intarraylist) -> {
        int i = 0;

        for (int j = ChunkStatus.getStatusList().size() - 1; j >= 0; --j) {
            while (i + 1 < ChunkStatus.STATUS_BY_RANGE.size() && j <= ((ChunkStatus) ChunkStatus.STATUS_BY_RANGE.get(i + 1)).getIndex()) {
                ++i;
            }

            intarraylist.add(0, i);
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
        boolean flag = ChunkStatus.isLighted(status, chunk);

        if (!chunk.getStatus().isOrAfter(status)) {
            ((ProtoChunk) chunk).setStatus(status);
        }

        return lightingProvider.lightChunk(chunk, flag).thenApply(Either::left);
    }

    private static ChunkStatus registerSimple(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<Heightmap.Types> heightMapTypes, ChunkStatus.ChunkType chunkType, ChunkStatus.SimpleGenerationTask task) {
        return ChunkStatus.register(id, previous, taskMargin, heightMapTypes, chunkType, task);
    }

    private static ChunkStatus register(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<Heightmap.Types> heightMapTypes, ChunkStatus.ChunkType chunkType, ChunkStatus.GenerationTask task) {
        return ChunkStatus.register(id, previous, taskMargin, heightMapTypes, chunkType, task, ChunkStatus.PASSTHROUGH_LOAD_TASK);
    }

    private static ChunkStatus register(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<Heightmap.Types> heightMapTypes, ChunkStatus.ChunkType chunkType, ChunkStatus.GenerationTask task, ChunkStatus.LoadingTask loadTask) {
        return (ChunkStatus) Registry.register(Registry.CHUNK_STATUS, id, new ChunkStatus(id, previous, taskMargin, heightMapTypes, chunkType, task, loadTask));
    }

    public static List<ChunkStatus> getStatusList() {
        List<ChunkStatus> list = Lists.newArrayList();

        ChunkStatus chunkstatus;

        for (chunkstatus = ChunkStatus.FULL; chunkstatus.getParent() != chunkstatus; chunkstatus = chunkstatus.getParent()) {
            list.add(chunkstatus);
        }

        list.add(chunkstatus);
        Collections.reverse(list);
        return list;
    }

    private static boolean isLighted(ChunkStatus status, ChunkAccess chunk) {
        return chunk.getStatus().isOrAfter(status) && chunk.isLightCorrect();
    }

    public static ChunkStatus getStatusAroundFullChunk(int level) {
        return level >= ChunkStatus.STATUS_BY_RANGE.size() ? ChunkStatus.EMPTY : (level < 0 ? ChunkStatus.FULL : (ChunkStatus) ChunkStatus.STATUS_BY_RANGE.get(level));
    }

    public static int maxDistance() {
        return ChunkStatus.STATUS_BY_RANGE.size();
    }

    public static int getDistance(ChunkStatus status) {
        return ChunkStatus.RANGE_BY_STATUS.getInt(status.getIndex());
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

    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> generate(Executor executor, ServerLevel world, ChunkGenerator generator, StructureManager structureManager, ThreadedLevelLightEngine lightingProvider, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> fullChunkConverter, List<ChunkAccess> chunks, boolean flag) {
        ChunkAccess ichunkaccess = (ChunkAccess) chunks.get(chunks.size() / 2);
        ProfiledDuration profiledduration = JvmProfiler.INSTANCE.onChunkGenerate(ichunkaccess.getPos(), world.dimension(), this.name);
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.generationTask.doWork(this, executor, world, generator, structureManager, lightingProvider, fullChunkConverter, chunks, ichunkaccess, flag);

        return profiledduration != null ? completablefuture.thenApply((either) -> {
            profiledduration.finish();
            return either;
        }) : completablefuture;
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

    // Paper start
    public static ChunkStatus getStatus(String name) {
        try {
            // We need this otherwise we return EMPTY for invalid names
            ResourceLocation key = new ResourceLocation(name);
            return Registry.CHUNK_STATUS.getOptional(key).orElse(null);
        } catch (Exception ex) {
            return null; // invalid name
        }
    }
    // Paper end
    public static ChunkStatus byName(String id) {
        return (ChunkStatus) Registry.CHUNK_STATUS.get(ResourceLocation.tryParse(id));
    }

    public EnumSet<Heightmap.Types> heightmapsAfter() {
        return this.heightmapsAfter;
    }

    public boolean isOrAfter(ChunkStatus chunkStatus) {
        return this.getIndex() >= chunkStatus.getIndex();
    }

    public String toString() {
        return Registry.CHUNK_STATUS.getKey(this).toString();
    }

    public static enum ChunkType {

        PROTOCHUNK, LEVELCHUNK;

        private ChunkType() {}
    }

    private interface GenerationTask {

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(ChunkStatus targetStatus, Executor executor, ServerLevel world, ChunkGenerator generator, StructureManager structureManager, ThreadedLevelLightEngine lightingProvider, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> fullChunkConverter, List<ChunkAccess> chunks, ChunkAccess chunk, boolean flag);
    }

    private interface LoadingTask {

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(ChunkStatus targetStatus, ServerLevel world, StructureManager structureManager, ThreadedLevelLightEngine lightingProvider, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> fullChunkConverter, ChunkAccess chunk);
    }

    private interface SimpleGenerationTask extends ChunkStatus.GenerationTask {

        @Override
        default CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(ChunkStatus targetStatus, Executor executor, ServerLevel world, ChunkGenerator generator, StructureManager structureManager, ThreadedLevelLightEngine lightingProvider, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> fullChunkConverter, List<ChunkAccess> chunks, ChunkAccess chunk, boolean flag) {
            if (flag || !chunk.getStatus().isOrAfter(targetStatus)) {
                this.doWork(targetStatus, world, generator, chunks, chunk);
                if (chunk instanceof ProtoChunk) {
                    ProtoChunk protochunk = (ProtoChunk) chunk;

                    protochunk.setStatus(targetStatus);
                }
            }

            return CompletableFuture.completedFuture(Either.left(chunk));
        }

        void doWork(ChunkStatus targetStatus, ServerLevel world, ChunkGenerator chunkGenerator, List<ChunkAccess> chunks, ChunkAccess chunk);
    }
}
