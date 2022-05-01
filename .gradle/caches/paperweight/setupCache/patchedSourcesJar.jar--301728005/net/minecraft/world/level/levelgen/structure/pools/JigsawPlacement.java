package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class JigsawPlacement {
    static final Logger LOGGER = LogUtils.getLogger();

    public static Optional<PieceGenerator<JigsawConfiguration>> addPieces(PieceGeneratorSupplier.Context<JigsawConfiguration> context, JigsawPlacement.PieceFactory pieceFactory, BlockPos pos, boolean bl, boolean bl2) {
        WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(0L));
        worldgenRandom.setLargeFeatureSeed(context.seed(), context.chunkPos().x, context.chunkPos().z);
        RegistryAccess registryAccess = context.registryAccess();
        JigsawConfiguration jigsawConfiguration = context.config();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        StructureManager structureManager = context.structureManager();
        LevelHeightAccessor levelHeightAccessor = context.heightAccessor();
        Predicate<Holder<Biome>> predicate = context.validBiome();
        StructureFeature.bootstrap();
        Registry<StructureTemplatePool> registry = registryAccess.registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY);
        Rotation rotation = Rotation.getRandom(worldgenRandom);
        StructureTemplatePool structureTemplatePool = jigsawConfiguration.startPool().value();
        StructurePoolElement structurePoolElement = structureTemplatePool.getRandomTemplate(worldgenRandom);
        if (structurePoolElement == EmptyPoolElement.INSTANCE) {
            return Optional.empty();
        } else {
            PoolElementStructurePiece poolElementStructurePiece = pieceFactory.create(structureManager, structurePoolElement, pos, structurePoolElement.getGroundLevelDelta(), rotation, structurePoolElement.getBoundingBox(structureManager, pos, rotation));
            BoundingBox boundingBox = poolElementStructurePiece.getBoundingBox();
            int i = (boundingBox.maxX() + boundingBox.minX()) / 2;
            int j = (boundingBox.maxZ() + boundingBox.minZ()) / 2;
            int k;
            if (bl2) {
                k = pos.getY() + chunkGenerator.getFirstFreeHeight(i, j, Heightmap.Types.WORLD_SURFACE_WG, levelHeightAccessor);
            } else {
                k = pos.getY();
            }

            if (!predicate.test(chunkGenerator.getNoiseBiome(QuartPos.fromBlock(i), QuartPos.fromBlock(k), QuartPos.fromBlock(j)))) {
                return Optional.empty();
            } else {
                int m = boundingBox.minY() + poolElementStructurePiece.getGroundLevelDelta();
                poolElementStructurePiece.move(0, k - m, 0);
                return Optional.of((structurePiecesBuilder, contextx) -> {
                    List<PoolElementStructurePiece> list = Lists.newArrayList();
                    list.add(poolElementStructurePiece);
                    if (jigsawConfiguration.maxDepth() > 0) {
                        int l = 80;
                        AABB aABB = new AABB((double)(i - 80), (double)(k - 80), (double)(j - 80), (double)(i + 80 + 1), (double)(k + 80 + 1), (double)(j + 80 + 1));
                        JigsawPlacement.Placer placer = new JigsawPlacement.Placer(registry, jigsawConfiguration.maxDepth(), pieceFactory, chunkGenerator, structureManager, list, worldgenRandom);
                        placer.placing.addLast(new JigsawPlacement.PieceState(poolElementStructurePiece, new MutableObject<>(Shapes.join(Shapes.create(aABB), Shapes.create(AABB.of(boundingBox)), BooleanOp.ONLY_FIRST)), 0));

                        while(!placer.placing.isEmpty()) {
                            JigsawPlacement.PieceState pieceState = placer.placing.removeFirst();
                            placer.tryPlacingChildren(pieceState.piece, pieceState.free, pieceState.depth, bl, levelHeightAccessor);
                        }

                        list.forEach(structurePiecesBuilder::addPiece);
                    }
                });
            }
        }
    }

    public static void addPieces(RegistryAccess registryManager, PoolElementStructurePiece piece, int maxDepth, JigsawPlacement.PieceFactory pieceFactory, ChunkGenerator chunkGenerator, StructureManager structureManager, List<? super PoolElementStructurePiece> results, Random random, LevelHeightAccessor world) {
        Registry<StructureTemplatePool> registry = registryManager.registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY);
        JigsawPlacement.Placer placer = new JigsawPlacement.Placer(registry, maxDepth, pieceFactory, chunkGenerator, structureManager, results, random);
        placer.placing.addLast(new JigsawPlacement.PieceState(piece, new MutableObject<>(Shapes.INFINITY), 0));

        while(!placer.placing.isEmpty()) {
            JigsawPlacement.PieceState pieceState = placer.placing.removeFirst();
            placer.tryPlacingChildren(pieceState.piece, pieceState.free, pieceState.depth, false, world);
        }

    }

    public interface PieceFactory {
        PoolElementStructurePiece create(StructureManager structureManager, StructurePoolElement poolElement, BlockPos pos, int groundLevelDelta, Rotation rotation, BoundingBox elementBounds);
    }

    static final class PieceState {
        final PoolElementStructurePiece piece;
        final MutableObject<VoxelShape> free;
        final int depth;

        PieceState(PoolElementStructurePiece piece, MutableObject<VoxelShape> pieceShape, int currentSize) {
            this.piece = piece;
            this.free = pieceShape;
            this.depth = currentSize;
        }
    }

    static final class Placer {
        private final Registry<StructureTemplatePool> pools;
        private final int maxDepth;
        private final JigsawPlacement.PieceFactory factory;
        private final ChunkGenerator chunkGenerator;
        private final StructureManager structureManager;
        private final List<? super PoolElementStructurePiece> pieces;
        private final Random random;
        final Deque<JigsawPlacement.PieceState> placing = Queues.newArrayDeque();

        Placer(Registry<StructureTemplatePool> registry, int maxSize, JigsawPlacement.PieceFactory pieceFactory, ChunkGenerator chunkGenerator, StructureManager structureManager, List<? super PoolElementStructurePiece> children, Random random) {
            this.pools = registry;
            this.maxDepth = maxSize;
            this.factory = pieceFactory;
            this.chunkGenerator = chunkGenerator;
            this.structureManager = structureManager;
            this.pieces = children;
            this.random = random;
        }

        void tryPlacingChildren(PoolElementStructurePiece piece, MutableObject<VoxelShape> pieceShape, int minY, boolean modifyBoundingBox, LevelHeightAccessor world) {
            StructurePoolElement structurePoolElement = piece.getElement();
            BlockPos blockPos = piece.getPosition();
            Rotation rotation = piece.getRotation();
            StructureTemplatePool.Projection projection = structurePoolElement.getProjection();
            boolean bl = projection == StructureTemplatePool.Projection.RIGID;
            MutableObject<VoxelShape> mutableObject = new MutableObject<>();
            BoundingBox boundingBox = piece.getBoundingBox();
            int i = boundingBox.minY();

            label139:
            for(StructureTemplate.StructureBlockInfo structureBlockInfo : structurePoolElement.getShuffledJigsawBlocks(this.structureManager, blockPos, rotation, this.random)) {
                Direction direction = JigsawBlock.getFrontFacing(structureBlockInfo.state);
                BlockPos blockPos2 = structureBlockInfo.pos;
                BlockPos blockPos3 = blockPos2.relative(direction);
                int j = blockPos2.getY() - i;
                int k = -1;
                ResourceLocation resourceLocation = new ResourceLocation(structureBlockInfo.nbt.getString("pool"));
                Optional<StructureTemplatePool> optional = this.pools.getOptional(resourceLocation);
                if (optional.isPresent() && (optional.get().size() != 0 || Objects.equals(resourceLocation, Pools.EMPTY.location()))) {
                    ResourceLocation resourceLocation2 = optional.get().getFallback();
                    Optional<StructureTemplatePool> optional2 = this.pools.getOptional(resourceLocation2);
                    if (optional2.isPresent() && (optional2.get().size() != 0 || Objects.equals(resourceLocation2, Pools.EMPTY.location()))) {
                        boolean bl2 = boundingBox.isInside(blockPos3);
                        MutableObject<VoxelShape> mutableObject2;
                        if (bl2) {
                            mutableObject2 = mutableObject;
                            if (mutableObject.getValue() == null) {
                                mutableObject.setValue(Shapes.create(AABB.of(boundingBox)));
                            }
                        } else {
                            mutableObject2 = pieceShape;
                        }

                        List<StructurePoolElement> list = Lists.newArrayList();
                        if (minY != this.maxDepth) {
                            list.addAll(optional.get().getShuffledTemplates(this.random));
                        }

                        list.addAll(optional2.get().getShuffledTemplates(this.random));

                        for(StructurePoolElement structurePoolElement2 : list) {
                            if (structurePoolElement2 == EmptyPoolElement.INSTANCE) {
                                break;
                            }

                            for(Rotation rotation2 : Rotation.getShuffled(this.random)) {
                                List<StructureTemplate.StructureBlockInfo> list2 = structurePoolElement2.getShuffledJigsawBlocks(this.structureManager, BlockPos.ZERO, rotation2, this.random);
                                BoundingBox boundingBox2 = structurePoolElement2.getBoundingBox(this.structureManager, BlockPos.ZERO, rotation2);
                                int m;
                                if (modifyBoundingBox && boundingBox2.getYSpan() <= 16) {
                                    m = list2.stream().mapToInt((structureBlockInfox) -> {
                                        if (!boundingBox2.isInside(structureBlockInfox.pos.relative(JigsawBlock.getFrontFacing(structureBlockInfox.state)))) {
                                            return 0;
                                        } else {
                                            ResourceLocation resourceLocation = new ResourceLocation(structureBlockInfox.nbt.getString("pool"));
                                            Optional<StructureTemplatePool> optional = this.pools.getOptional(resourceLocation);
                                            Optional<StructureTemplatePool> optional2 = optional.flatMap((pool) -> {
                                                return this.pools.getOptional(pool.getFallback());
                                            });
                                            int i = optional.map((pool) -> {
                                                return pool.getMaxSize(this.structureManager);
                                            }).orElse(0);
                                            int j = optional2.map((pool) -> {
                                                return pool.getMaxSize(this.structureManager);
                                            }).orElse(0);
                                            return Math.max(i, j);
                                        }
                                    }).max().orElse(0);
                                } else {
                                    m = 0;
                                }

                                for(StructureTemplate.StructureBlockInfo structureBlockInfo2 : list2) {
                                    if (JigsawBlock.canAttach(structureBlockInfo, structureBlockInfo2)) {
                                        BlockPos blockPos4 = structureBlockInfo2.pos;
                                        BlockPos blockPos5 = blockPos3.subtract(blockPos4);
                                        BoundingBox boundingBox3 = structurePoolElement2.getBoundingBox(this.structureManager, blockPos5, rotation2);
                                        int n = boundingBox3.minY();
                                        StructureTemplatePool.Projection projection2 = structurePoolElement2.getProjection();
                                        boolean bl3 = projection2 == StructureTemplatePool.Projection.RIGID;
                                        int o = blockPos4.getY();
                                        int p = j - o + JigsawBlock.getFrontFacing(structureBlockInfo.state).getStepY();
                                        int q;
                                        if (bl && bl3) {
                                            q = i + p;
                                        } else {
                                            if (k == -1) {
                                                k = this.chunkGenerator.getFirstFreeHeight(blockPos2.getX(), blockPos2.getZ(), Heightmap.Types.WORLD_SURFACE_WG, world);
                                            }

                                            q = k - o;
                                        }

                                        int s = q - n;
                                        BoundingBox boundingBox4 = boundingBox3.moved(0, s, 0);
                                        BlockPos blockPos6 = blockPos5.offset(0, s, 0);
                                        if (m > 0) {
                                            int t = Math.max(m + 1, boundingBox4.maxY() - boundingBox4.minY());
                                            boundingBox4.encapsulate(new BlockPos(boundingBox4.minX(), boundingBox4.minY() + t, boundingBox4.minZ()));
                                        }

                                        if (!Shapes.joinIsNotEmpty(mutableObject2.getValue(), Shapes.create(AABB.of(boundingBox4).deflate(0.25D)), BooleanOp.ONLY_SECOND)) {
                                            mutableObject2.setValue(Shapes.joinUnoptimized(mutableObject2.getValue(), Shapes.create(AABB.of(boundingBox4)), BooleanOp.ONLY_FIRST));
                                            int u = piece.getGroundLevelDelta();
                                            int v;
                                            if (bl3) {
                                                v = u - p;
                                            } else {
                                                v = structurePoolElement2.getGroundLevelDelta();
                                            }

                                            PoolElementStructurePiece poolElementStructurePiece = this.factory.create(this.structureManager, structurePoolElement2, blockPos6, v, rotation2, boundingBox4);
                                            int x;
                                            if (bl) {
                                                x = i + j;
                                            } else if (bl3) {
                                                x = q + o;
                                            } else {
                                                if (k == -1) {
                                                    k = this.chunkGenerator.getFirstFreeHeight(blockPos2.getX(), blockPos2.getZ(), Heightmap.Types.WORLD_SURFACE_WG, world);
                                                }

                                                x = k + p / 2;
                                            }

                                            piece.addJunction(new JigsawJunction(blockPos3.getX(), x - j + u, blockPos3.getZ(), p, projection2));
                                            poolElementStructurePiece.addJunction(new JigsawJunction(blockPos2.getX(), x - o + v, blockPos2.getZ(), -p, projection));
                                            this.pieces.add(poolElementStructurePiece);
                                            if (minY + 1 <= this.maxDepth) {
                                                this.placing.addLast(new JigsawPlacement.PieceState(poolElementStructurePiece, mutableObject2, minY + 1));
                                            }
                                            continue label139;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        JigsawPlacement.LOGGER.warn("Empty or non-existent fallback pool: {}", (Object)resourceLocation2);
                    }
                } else {
                    JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", (Object)resourceLocation);
                }
            }

        }
    }
}
