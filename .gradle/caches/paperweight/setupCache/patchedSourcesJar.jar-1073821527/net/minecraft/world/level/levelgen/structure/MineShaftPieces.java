package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.MineshaftFeature;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.slf4j.Logger;

public class MineShaftPieces {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_SHAFT_WIDTH = 3;
    private static final int DEFAULT_SHAFT_HEIGHT = 3;
    private static final int DEFAULT_SHAFT_LENGTH = 5;
    private static final int MAX_PILLAR_HEIGHT = 20;
    private static final int MAX_CHAIN_HEIGHT = 50;
    private static final int MAX_DEPTH = 8;
    public static final int MAGIC_START_Y = 50;

    private static MineShaftPieces.MineShaftPiece createRandomShaftPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, @Nullable Direction orientation, int chainLength, MineshaftFeature.Type type) {
        int i = random.nextInt(100);
        if (i >= 80) {
            BoundingBox boundingBox = MineShaftPieces.MineShaftCrossing.findCrossing(holder, random, x, y, z, orientation);
            if (boundingBox != null) {
                return new MineShaftPieces.MineShaftCrossing(chainLength, boundingBox, orientation, type);
            }
        } else if (i >= 70) {
            BoundingBox boundingBox2 = MineShaftPieces.MineShaftStairs.findStairs(holder, random, x, y, z, orientation);
            if (boundingBox2 != null) {
                return new MineShaftPieces.MineShaftStairs(chainLength, boundingBox2, orientation, type);
            }
        } else {
            BoundingBox boundingBox3 = MineShaftPieces.MineShaftCorridor.findCorridorSize(holder, random, x, y, z, orientation);
            if (boundingBox3 != null) {
                return new MineShaftPieces.MineShaftCorridor(chainLength, random, boundingBox3, orientation, type);
            }
        }

        return null;
    }

    static MineShaftPieces.MineShaftPiece generateAndAddPiece(StructurePiece start, StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
        if (chainLength > 8) {
            return null;
        } else if (Math.abs(x - start.getBoundingBox().minX()) <= 80 && Math.abs(z - start.getBoundingBox().minZ()) <= 80) {
            MineshaftFeature.Type type = ((MineShaftPieces.MineShaftPiece)start).type;
            MineShaftPieces.MineShaftPiece mineShaftPiece = createRandomShaftPiece(holder, random, x, y, z, orientation, chainLength + 1, type);
            if (mineShaftPiece != null) {
                holder.addPiece(mineShaftPiece);
                mineShaftPiece.addChildren(start, holder, random);
            }

            return mineShaftPiece;
        } else {
            return null;
        }
    }

    public static class MineShaftCorridor extends MineShaftPieces.MineShaftPiece {
        private final boolean hasRails;
        private final boolean spiderCorridor;
        private boolean hasPlacedSpider;
        private final int numSections;

        public MineShaftCorridor(CompoundTag nbt) {
            super(StructurePieceType.MINE_SHAFT_CORRIDOR, nbt);
            this.hasRails = nbt.getBoolean("hr");
            this.spiderCorridor = nbt.getBoolean("sc");
            this.hasPlacedSpider = nbt.getBoolean("hps");
            this.numSections = nbt.getInt("Num");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("hr", this.hasRails);
            nbt.putBoolean("sc", this.spiderCorridor);
            nbt.putBoolean("hps", this.hasPlacedSpider);
            nbt.putInt("Num", this.numSections);
        }

        public MineShaftCorridor(int chainLength, Random random, BoundingBox boundingBox, Direction orientation, MineshaftFeature.Type type) {
            super(StructurePieceType.MINE_SHAFT_CORRIDOR, chainLength, type, boundingBox);
            this.setOrientation(orientation);
            this.hasRails = random.nextInt(3) == 0;
            this.spiderCorridor = !this.hasRails && random.nextInt(23) == 0;
            if (this.getOrientation().getAxis() == Direction.Axis.Z) {
                this.numSections = boundingBox.getZSpan() / 5;
            } else {
                this.numSections = boundingBox.getXSpan() / 5;
            }

        }

        @Nullable
        public static BoundingBox findCorridorSize(StructurePieceAccessor structurePieceAccessor, Random random, int x, int y, int z, Direction orientation) {
            for(int i = random.nextInt(3) + 2; i > 0; --i) {
                int j = i * 5;
                BoundingBox boundingBox;
                switch(orientation) {
                case NORTH:
                default:
                    boundingBox = new BoundingBox(0, 0, -(j - 1), 2, 2, 0);
                    break;
                case SOUTH:
                    boundingBox = new BoundingBox(0, 0, 0, 2, 2, j - 1);
                    break;
                case WEST:
                    boundingBox = new BoundingBox(-(j - 1), 0, 0, 0, 2, 2);
                    break;
                case EAST:
                    boundingBox = new BoundingBox(0, 0, 0, j - 1, 2, 2);
                }

                boundingBox.move(x, y, z);
                if (structurePieceAccessor.findCollisionPiece(boundingBox) == null) {
                    return boundingBox;
                }
            }

            return null;
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            int i = this.getGenDepth();
            int j = random.nextInt(4);
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                default:
                    if (j <= 1) {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ() - 1, direction, i);
                    } else if (j == 2) {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), Direction.WEST, i);
                    } else {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), Direction.EAST, i);
                    }
                    break;
                case SOUTH:
                    if (j <= 1) {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() + 1, direction, i);
                    } else if (j == 2) {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() - 3, Direction.WEST, i);
                    } else {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() - 3, Direction.EAST, i);
                    }
                    break;
                case WEST:
                    if (j <= 1) {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), direction, i);
                    } else if (j == 2) {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ() - 1, Direction.NORTH, i);
                    } else {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() + 1, Direction.SOUTH, i);
                    }
                    break;
                case EAST:
                    if (j <= 1) {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), direction, i);
                    } else if (j == 2) {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() - 3, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ() - 1, Direction.NORTH, i);
                    } else {
                        MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() - 3, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() + 1, Direction.SOUTH, i);
                    }
                }
            }

            if (i < 8) {
                if (direction != Direction.NORTH && direction != Direction.SOUTH) {
                    for(int m = this.boundingBox.minX() + 3; m + 3 <= this.boundingBox.maxX(); m += 5) {
                        int n = random.nextInt(5);
                        if (n == 0) {
                            MineShaftPieces.generateAndAddPiece(start, holder, random, m, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, i + 1);
                        } else if (n == 1) {
                            MineShaftPieces.generateAndAddPiece(start, holder, random, m, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, i + 1);
                        }
                    }
                } else {
                    for(int k = this.boundingBox.minZ() + 3; k + 3 <= this.boundingBox.maxZ(); k += 5) {
                        int l = random.nextInt(5);
                        if (l == 0) {
                            MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), k, Direction.WEST, i + 1);
                        } else if (l == 1) {
                            MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), k, Direction.EAST, i + 1);
                        }
                    }
                }
            }

        }

        @Override
        protected boolean createChest(WorldGenLevel world, BoundingBox boundingBox, Random random, int x, int y, int z, ResourceLocation lootTableId) {
            BlockPos blockPos = this.getWorldPos(x, y, z);
            if (boundingBox.isInside(blockPos) && world.getBlockState(blockPos).isAir() && !world.getBlockState(blockPos.below()).isAir()) {
                BlockState blockState = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, random.nextBoolean() ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST);
                this.placeBlock(world, blockState, x, y, z, boundingBox);
                MinecartChest minecartChest = new MinecartChest(world.getLevel(), (double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.5D, (double)blockPos.getZ() + 0.5D);
                minecartChest.setLootTable(lootTableId, random.nextLong());
                world.addFreshEntity(minecartChest);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            if (!this.edgesLiquid(world, chunkBox)) {
                int i = 0;
                int j = 2;
                int k = 0;
                int l = 2;
                int m = this.numSections * 5 - 1;
                BlockState blockState = this.type.getPlanksState();
                this.generateBox(world, chunkBox, 0, 0, 0, 2, 1, m, CAVE_AIR, CAVE_AIR, false);
                this.generateMaybeBox(world, chunkBox, random, 0.8F, 0, 2, 0, 2, 2, m, CAVE_AIR, CAVE_AIR, false, false);
                if (this.spiderCorridor) {
                    this.generateMaybeBox(world, chunkBox, random, 0.6F, 0, 0, 0, 2, 1, m, Blocks.COBWEB.defaultBlockState(), CAVE_AIR, false, true);
                }

                for(int n = 0; n < this.numSections; ++n) {
                    int o = 2 + n * 5;
                    this.placeSupport(world, chunkBox, 0, 0, o, 2, 2, random);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.1F, 0, 2, o - 1);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.1F, 2, 2, o - 1);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.1F, 0, 2, o + 1);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.1F, 2, 2, o + 1);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.05F, 0, 2, o - 2);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.05F, 2, 2, o - 2);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.05F, 0, 2, o + 2);
                    this.maybePlaceCobWeb(world, chunkBox, random, 0.05F, 2, 2, o + 2);
                    if (random.nextInt(100) == 0) {
                        this.createChest(world, chunkBox, random, 2, 0, o - 1, BuiltInLootTables.ABANDONED_MINESHAFT);
                    }

                    if (random.nextInt(100) == 0) {
                        this.createChest(world, chunkBox, random, 0, 0, o + 1, BuiltInLootTables.ABANDONED_MINESHAFT);
                    }

                    if (this.spiderCorridor && !this.hasPlacedSpider) {
                        int p = 1;
                        int q = o - 1 + random.nextInt(3);
                        BlockPos blockPos = this.getWorldPos(1, 0, q);
                        if (chunkBox.isInside(blockPos) && this.isInterior(world, 1, 0, q, chunkBox)) {
                            this.hasPlacedSpider = true;
                            world.setBlock(blockPos, Blocks.SPAWNER.defaultBlockState(), 2);
                            BlockEntity blockEntity = world.getBlockEntity(blockPos);
                            if (blockEntity instanceof SpawnerBlockEntity) {
                                ((SpawnerBlockEntity)blockEntity).getSpawner().setEntityId(EntityType.CAVE_SPIDER);
                            }
                        }
                    }
                }

                for(int r = 0; r <= 2; ++r) {
                    for(int s = 0; s <= m; ++s) {
                        this.setPlanksBlock(world, chunkBox, blockState, r, -1, s);
                    }
                }

                int t = 2;
                this.placeDoubleLowerOrUpperSupport(world, chunkBox, 0, -1, 2);
                if (this.numSections > 1) {
                    int u = m - 2;
                    this.placeDoubleLowerOrUpperSupport(world, chunkBox, 0, -1, u);
                }

                if (this.hasRails) {
                    BlockState blockState2 = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH);

                    for(int v = 0; v <= m; ++v) {
                        BlockState blockState3 = this.getBlock(world, 1, -1, v, chunkBox);
                        if (!blockState3.isAir() && blockState3.isSolidRender(world, this.getWorldPos(1, -1, v))) {
                            float f = this.isInterior(world, 1, 0, v, chunkBox) ? 0.7F : 0.9F;
                            this.maybeGenerateBlock(world, chunkBox, random, f, 1, 0, v, blockState2);
                        }
                    }
                }

            }
        }

        private void placeDoubleLowerOrUpperSupport(WorldGenLevel world, BoundingBox box, int x, int y, int z) {
            BlockState blockState = this.type.getWoodState();
            BlockState blockState2 = this.type.getPlanksState();
            if (this.getBlock(world, x, y, z, box).is(blockState2.getBlock())) {
                this.fillPillarDownOrChainUp(world, blockState, x, y, z, box);
            }

            if (this.getBlock(world, x + 2, y, z, box).is(blockState2.getBlock())) {
                this.fillPillarDownOrChainUp(world, blockState, x + 2, y, z, box);
            }

        }

        @Override
        protected void fillColumnDown(WorldGenLevel world, BlockState state, int x, int y, int z, BoundingBox box) {
            BlockPos.MutableBlockPos mutableBlockPos = this.getWorldPos(x, y, z);
            if (box.isInside(mutableBlockPos)) {
                int i = mutableBlockPos.getY();

                while(this.isReplaceableByStructures(world.getBlockState(mutableBlockPos)) && mutableBlockPos.getY() > world.getMinBuildHeight() + 1) {
                    mutableBlockPos.move(Direction.DOWN);
                }

                if (this.canPlaceColumnOnTopOf(world, mutableBlockPos, world.getBlockState(mutableBlockPos))) {
                    while(mutableBlockPos.getY() < i) {
                        mutableBlockPos.move(Direction.UP);
                        world.setBlock(mutableBlockPos, state, 2);
                    }

                }
            }
        }

        protected void fillPillarDownOrChainUp(WorldGenLevel world, BlockState state, int x, int y, int z, BoundingBox box) {
            BlockPos.MutableBlockPos mutableBlockPos = this.getWorldPos(x, y, z);
            if (box.isInside(mutableBlockPos)) {
                int i = mutableBlockPos.getY();
                int j = 1;
                boolean bl = true;

                for(boolean bl2 = true; bl || bl2; ++j) {
                    if (bl) {
                        mutableBlockPos.setY(i - j);
                        BlockState blockState = world.getBlockState(mutableBlockPos);
                        boolean bl3 = this.isReplaceableByStructures(blockState) && !blockState.is(Blocks.LAVA);
                        if (!bl3 && this.canPlaceColumnOnTopOf(world, mutableBlockPos, blockState)) {
                            fillColumnBetween(world, state, mutableBlockPos, i - j + 1, i);
                            return;
                        }

                        bl = j <= 20 && bl3 && mutableBlockPos.getY() > world.getMinBuildHeight() + 1;
                    }

                    if (bl2) {
                        mutableBlockPos.setY(i + j);
                        BlockState blockState2 = world.getBlockState(mutableBlockPos);
                        boolean bl4 = this.isReplaceableByStructures(blockState2);
                        if (!bl4 && this.canHangChainBelow(world, mutableBlockPos, blockState2)) {
                            world.setBlock(mutableBlockPos.setY(i + 1), this.type.getFenceState(), 2);
                            fillColumnBetween(world, Blocks.CHAIN.defaultBlockState(), mutableBlockPos, i + 2, i + j);
                            return;
                        }

                        bl2 = j <= 50 && bl4 && mutableBlockPos.getY() < world.getMaxBuildHeight() - 1;
                    }
                }

            }
        }

        private static void fillColumnBetween(WorldGenLevel world, BlockState state, BlockPos.MutableBlockPos pos, int startY, int endY) {
            for(int i = startY; i < endY; ++i) {
                world.setBlock(pos.setY(i), state, 2);
            }

        }

        private boolean canPlaceColumnOnTopOf(LevelReader levelReader, BlockPos blockPos, BlockState blockState) {
            return blockState.isFaceSturdy(levelReader, blockPos, Direction.UP);
        }

        private boolean canHangChainBelow(LevelReader world, BlockPos pos, BlockState state) {
            return Block.canSupportCenter(world, pos, Direction.DOWN) && !(state.getBlock() instanceof FallingBlock);
        }

        private void placeSupport(WorldGenLevel world, BoundingBox boundingBox, int minX, int minY, int z, int maxY, int maxX, Random random) {
            if (this.isSupportingBox(world, boundingBox, minX, maxX, maxY, z)) {
                BlockState blockState = this.type.getPlanksState();
                BlockState blockState2 = this.type.getFenceState();
                this.generateBox(world, boundingBox, minX, minY, z, minX, maxY - 1, z, blockState2.setValue(FenceBlock.WEST, Boolean.valueOf(true)), CAVE_AIR, false);
                this.generateBox(world, boundingBox, maxX, minY, z, maxX, maxY - 1, z, blockState2.setValue(FenceBlock.EAST, Boolean.valueOf(true)), CAVE_AIR, false);
                if (random.nextInt(4) == 0) {
                    this.generateBox(world, boundingBox, minX, maxY, z, minX, maxY, z, blockState, CAVE_AIR, false);
                    this.generateBox(world, boundingBox, maxX, maxY, z, maxX, maxY, z, blockState, CAVE_AIR, false);
                } else {
                    this.generateBox(world, boundingBox, minX, maxY, z, maxX, maxY, z, blockState, CAVE_AIR, false);
                    this.maybeGenerateBlock(world, boundingBox, random, 0.05F, minX + 1, maxY, z - 1, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
                    this.maybeGenerateBlock(world, boundingBox, random, 0.05F, minX + 1, maxY, z + 1, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH));
                }

            }
        }

        private void maybePlaceCobWeb(WorldGenLevel world, BoundingBox box, Random random, float threshold, int x, int y, int z) {
            if (this.isInterior(world, x, y, z, box) && random.nextFloat() < threshold && this.hasSturdyNeighbours(world, box, x, y, z, 2)) {
                this.placeBlock(world, Blocks.COBWEB.defaultBlockState(), x, y, z, box);
            }

        }

        private boolean hasSturdyNeighbours(WorldGenLevel world, BoundingBox box, int x, int y, int z, int count) {
            BlockPos.MutableBlockPos mutableBlockPos = this.getWorldPos(x, y, z);
            int i = 0;

            for(Direction direction : Direction.values()) {
                mutableBlockPos.move(direction);
                if (box.isInside(mutableBlockPos) && world.getBlockState(mutableBlockPos).isFaceSturdy(world, mutableBlockPos, direction.getOpposite())) {
                    ++i;
                    if (i >= count) {
                        return true;
                    }
                }

                mutableBlockPos.move(direction.getOpposite());
            }

            return false;
        }
    }

    public static class MineShaftCrossing extends MineShaftPieces.MineShaftPiece {
        private final Direction direction;
        private final boolean isTwoFloored;

        public MineShaftCrossing(CompoundTag nbt) {
            super(StructurePieceType.MINE_SHAFT_CROSSING, nbt);
            this.isTwoFloored = nbt.getBoolean("tf");
            this.direction = Direction.from2DDataValue(nbt.getInt("D"));
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("tf", this.isTwoFloored);
            nbt.putInt("D", this.direction.get2DDataValue());
        }

        public MineShaftCrossing(int chainLength, BoundingBox boundingBox, @Nullable Direction orientation, MineshaftFeature.Type type) {
            super(StructurePieceType.MINE_SHAFT_CROSSING, chainLength, type, boundingBox);
            this.direction = orientation;
            this.isTwoFloored = boundingBox.getYSpan() > 3;
        }

        @Nullable
        public static BoundingBox findCrossing(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation) {
            int i;
            if (random.nextInt(4) == 0) {
                i = 6;
            } else {
                i = 2;
            }

            BoundingBox boundingBox;
            switch(orientation) {
            case NORTH:
            default:
                boundingBox = new BoundingBox(-1, 0, -4, 3, i, 0);
                break;
            case SOUTH:
                boundingBox = new BoundingBox(-1, 0, 0, 3, i, 4);
                break;
            case WEST:
                boundingBox = new BoundingBox(-4, 0, -1, 0, i, 3);
                break;
            case EAST:
                boundingBox = new BoundingBox(0, 0, -1, 4, i, 3);
            }

            boundingBox.move(x, y, z);
            return holder.findCollisionPiece(boundingBox) != null ? null : boundingBox;
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            int i = this.getGenDepth();
            switch(this.direction) {
            case NORTH:
            default:
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, i);
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, i);
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, i);
                break;
            case SOUTH:
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, i);
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, i);
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, i);
                break;
            case WEST:
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, i);
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, i);
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, i);
                break;
            case EAST:
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, i);
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, i);
                MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, i);
            }

            if (this.isTwoFloored) {
                if (random.nextBoolean()) {
                    MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() - 1, Direction.NORTH, i);
                }

                if (random.nextBoolean()) {
                    MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() + 1, Direction.WEST, i);
                }

                if (random.nextBoolean()) {
                    MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() + 1, Direction.EAST, i);
                }

                if (random.nextBoolean()) {
                    MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.maxZ() + 1, Direction.SOUTH, i);
                }
            }

        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            if (!this.edgesLiquid(world, chunkBox)) {
                BlockState blockState = this.type.getPlanksState();
                if (this.isTwoFloored) {
                    this.generateBox(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), this.boundingBox.maxX() - 1, this.boundingBox.minY() + 3 - 1, this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
                    this.generateBox(world, chunkBox, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxX(), this.boundingBox.minY() + 3 - 1, this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
                    this.generateBox(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.maxY() - 2, this.boundingBox.minZ(), this.boundingBox.maxX() - 1, this.boundingBox.maxY(), this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
                    this.generateBox(world, chunkBox, this.boundingBox.minX(), this.boundingBox.maxY() - 2, this.boundingBox.minZ() + 1, this.boundingBox.maxX(), this.boundingBox.maxY(), this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
                    this.generateBox(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3, this.boundingBox.minZ() + 1, this.boundingBox.maxX() - 1, this.boundingBox.minY() + 3, this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
                } else {
                    this.generateBox(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), this.boundingBox.maxX() - 1, this.boundingBox.maxY(), this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
                    this.generateBox(world, chunkBox, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxX(), this.boundingBox.maxY(), this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
                }

                this.placeSupportPillar(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY());
                this.placeSupportPillar(world, chunkBox, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY());
                this.placeSupportPillar(world, chunkBox, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY());
                this.placeSupportPillar(world, chunkBox, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY());
                int i = this.boundingBox.minY() - 1;

                for(int j = this.boundingBox.minX(); j <= this.boundingBox.maxX(); ++j) {
                    for(int k = this.boundingBox.minZ(); k <= this.boundingBox.maxZ(); ++k) {
                        this.setPlanksBlock(world, chunkBox, blockState, j, i, k);
                    }
                }

            }
        }

        private void placeSupportPillar(WorldGenLevel world, BoundingBox boundingBox, int x, int minY, int z, int maxY) {
            if (!this.getBlock(world, x, maxY + 1, z, boundingBox).isAir()) {
                this.generateBox(world, boundingBox, x, minY, z, x, maxY, z, this.type.getPlanksState(), CAVE_AIR, false);
            }

        }
    }

    abstract static class MineShaftPiece extends StructurePiece {
        protected MineshaftFeature.Type type;

        public MineShaftPiece(StructurePieceType structurePieceType, int chainLength, MineshaftFeature.Type type, BoundingBox box) {
            super(structurePieceType, chainLength, box);
            this.type = type;
        }

        public MineShaftPiece(StructurePieceType type, CompoundTag nbt) {
            super(type, nbt);
            this.type = MineshaftFeature.Type.byId(nbt.getInt("MST"));
        }

        @Override
        protected boolean canBeReplaced(LevelReader world, int x, int y, int z, BoundingBox box) {
            BlockState blockState = this.getBlock(world, x, y, z, box);
            return !blockState.is(this.type.getPlanksState().getBlock()) && !blockState.is(this.type.getWoodState().getBlock()) && !blockState.is(this.type.getFenceState().getBlock()) && !blockState.is(Blocks.CHAIN);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            nbt.putInt("MST", this.type.ordinal());
        }

        protected boolean isSupportingBox(BlockGetter world, BoundingBox boundingBox, int minX, int maxX, int y, int z) {
            for(int i = minX; i <= maxX; ++i) {
                if (this.getBlock(world, i, y + 1, z, boundingBox).isAir()) {
                    return false;
                }
            }

            return true;
        }

        protected boolean edgesLiquid(BlockGetter world, BoundingBox box) {
            int i = Math.max(this.boundingBox.minX() - 1, box.minX());
            int j = Math.max(this.boundingBox.minY() - 1, box.minY());
            int k = Math.max(this.boundingBox.minZ() - 1, box.minZ());
            int l = Math.min(this.boundingBox.maxX() + 1, box.maxX());
            int m = Math.min(this.boundingBox.maxY() + 1, box.maxY());
            int n = Math.min(this.boundingBox.maxZ() + 1, box.maxZ());
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for(int o = i; o <= l; ++o) {
                for(int p = k; p <= n; ++p) {
                    if (world.getBlockState(mutableBlockPos.set(o, j, p)).getMaterial().isLiquid()) {
                        return true;
                    }

                    if (world.getBlockState(mutableBlockPos.set(o, m, p)).getMaterial().isLiquid()) {
                        return true;
                    }
                }
            }

            for(int q = i; q <= l; ++q) {
                for(int r = j; r <= m; ++r) {
                    if (world.getBlockState(mutableBlockPos.set(q, r, k)).getMaterial().isLiquid()) {
                        return true;
                    }

                    if (world.getBlockState(mutableBlockPos.set(q, r, n)).getMaterial().isLiquid()) {
                        return true;
                    }
                }
            }

            for(int s = k; s <= n; ++s) {
                for(int t = j; t <= m; ++t) {
                    if (world.getBlockState(mutableBlockPos.set(i, t, s)).getMaterial().isLiquid()) {
                        return true;
                    }

                    if (world.getBlockState(mutableBlockPos.set(l, t, s)).getMaterial().isLiquid()) {
                        return true;
                    }
                }
            }

            return false;
        }

        protected void setPlanksBlock(WorldGenLevel world, BoundingBox box, BlockState state, int x, int y, int z) {
            if (this.isInterior(world, x, y, z, box)) {
                BlockPos blockPos = this.getWorldPos(x, y, z);
                BlockState blockState = world.getBlockState(blockPos);
                if (!blockState.isFaceSturdy(world, blockPos, Direction.UP)) {
                    world.setBlock(blockPos, state, 2);
                }

            }
        }
    }

    public static class MineShaftRoom extends MineShaftPieces.MineShaftPiece {
        private final List<BoundingBox> childEntranceBoxes = Lists.newLinkedList();

        public MineShaftRoom(int chainLength, Random random, int x, int z, MineshaftFeature.Type type) {
            super(StructurePieceType.MINE_SHAFT_ROOM, chainLength, type, new BoundingBox(x, 50, z, x + 7 + random.nextInt(6), 54 + random.nextInt(6), z + 7 + random.nextInt(6)));
            this.type = type;
        }

        public MineShaftRoom(CompoundTag nbt) {
            super(StructurePieceType.MINE_SHAFT_ROOM, nbt);
            BoundingBox.CODEC.listOf().parse(NbtOps.INSTANCE, nbt.getList("Entrances", 11)).resultOrPartial(MineShaftPieces.LOGGER::error).ifPresent(this.childEntranceBoxes::addAll);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            int i = this.getGenDepth();
            int j = this.boundingBox.getYSpan() - 3 - 1;
            if (j <= 0) {
                j = 1;
            }

            int k;
            for(k = 0; k < this.boundingBox.getXSpan(); k += 4) {
                k += random.nextInt(this.boundingBox.getXSpan());
                if (k + 3 > this.boundingBox.getXSpan()) {
                    break;
                }

                MineShaftPieces.MineShaftPiece mineShaftPiece = MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + k, this.boundingBox.minY() + random.nextInt(j) + 1, this.boundingBox.minZ() - 1, Direction.NORTH, i);
                if (mineShaftPiece != null) {
                    BoundingBox boundingBox = mineShaftPiece.getBoundingBox();
                    this.childEntranceBoxes.add(new BoundingBox(boundingBox.minX(), boundingBox.minY(), this.boundingBox.minZ(), boundingBox.maxX(), boundingBox.maxY(), this.boundingBox.minZ() + 1));
                }
            }

            for(k = 0; k < this.boundingBox.getXSpan(); k += 4) {
                k += random.nextInt(this.boundingBox.getXSpan());
                if (k + 3 > this.boundingBox.getXSpan()) {
                    break;
                }

                MineShaftPieces.MineShaftPiece mineShaftPiece2 = MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + k, this.boundingBox.minY() + random.nextInt(j) + 1, this.boundingBox.maxZ() + 1, Direction.SOUTH, i);
                if (mineShaftPiece2 != null) {
                    BoundingBox boundingBox2 = mineShaftPiece2.getBoundingBox();
                    this.childEntranceBoxes.add(new BoundingBox(boundingBox2.minX(), boundingBox2.minY(), this.boundingBox.maxZ() - 1, boundingBox2.maxX(), boundingBox2.maxY(), this.boundingBox.maxZ()));
                }
            }

            for(k = 0; k < this.boundingBox.getZSpan(); k += 4) {
                k += random.nextInt(this.boundingBox.getZSpan());
                if (k + 3 > this.boundingBox.getZSpan()) {
                    break;
                }

                MineShaftPieces.MineShaftPiece mineShaftPiece3 = MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + random.nextInt(j) + 1, this.boundingBox.minZ() + k, Direction.WEST, i);
                if (mineShaftPiece3 != null) {
                    BoundingBox boundingBox3 = mineShaftPiece3.getBoundingBox();
                    this.childEntranceBoxes.add(new BoundingBox(this.boundingBox.minX(), boundingBox3.minY(), boundingBox3.minZ(), this.boundingBox.minX() + 1, boundingBox3.maxY(), boundingBox3.maxZ()));
                }
            }

            for(k = 0; k < this.boundingBox.getZSpan(); k += 4) {
                k += random.nextInt(this.boundingBox.getZSpan());
                if (k + 3 > this.boundingBox.getZSpan()) {
                    break;
                }

                StructurePiece structurePiece = MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + random.nextInt(j) + 1, this.boundingBox.minZ() + k, Direction.EAST, i);
                if (structurePiece != null) {
                    BoundingBox boundingBox4 = structurePiece.getBoundingBox();
                    this.childEntranceBoxes.add(new BoundingBox(this.boundingBox.maxX() - 1, boundingBox4.minY(), boundingBox4.minZ(), this.boundingBox.maxX(), boundingBox4.maxY(), boundingBox4.maxZ()));
                }
            }

        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            if (!this.edgesLiquid(world, chunkBox)) {
                this.generateBox(world, chunkBox, this.boundingBox.minX(), this.boundingBox.minY() + 1, this.boundingBox.minZ(), this.boundingBox.maxX(), Math.min(this.boundingBox.minY() + 3, this.boundingBox.maxY()), this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);

                for(BoundingBox boundingBox : this.childEntranceBoxes) {
                    this.generateBox(world, chunkBox, boundingBox.minX(), boundingBox.maxY() - 2, boundingBox.minZ(), boundingBox.maxX(), boundingBox.maxY(), boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
                }

                this.generateUpperHalfSphere(world, chunkBox, this.boundingBox.minX(), this.boundingBox.minY() + 4, this.boundingBox.minZ(), this.boundingBox.maxX(), this.boundingBox.maxY(), this.boundingBox.maxZ(), CAVE_AIR, false);
            }
        }

        @Override
        public void move(int x, int y, int z) {
            super.move(x, y, z);

            for(BoundingBox boundingBox : this.childEntranceBoxes) {
                boundingBox.move(x, y, z);
            }

        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            BoundingBox.CODEC.listOf().encodeStart(NbtOps.INSTANCE, this.childEntranceBoxes).resultOrPartial(MineShaftPieces.LOGGER::error).ifPresent((tag) -> {
                nbt.put("Entrances", tag);
            });
        }
    }

    public static class MineShaftStairs extends MineShaftPieces.MineShaftPiece {
        public MineShaftStairs(int chainLength, BoundingBox boundingBox, Direction orientation, MineshaftFeature.Type type) {
            super(StructurePieceType.MINE_SHAFT_STAIRS, chainLength, type, boundingBox);
            this.setOrientation(orientation);
        }

        public MineShaftStairs(CompoundTag nbt) {
            super(StructurePieceType.MINE_SHAFT_STAIRS, nbt);
        }

        @Nullable
        public static BoundingBox findStairs(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation) {
            BoundingBox boundingBox;
            switch(orientation) {
            case NORTH:
            default:
                boundingBox = new BoundingBox(0, -5, -8, 2, 2, 0);
                break;
            case SOUTH:
                boundingBox = new BoundingBox(0, -5, 0, 2, 2, 8);
                break;
            case WEST:
                boundingBox = new BoundingBox(-8, -5, 0, 0, 2, 2);
                break;
            case EAST:
                boundingBox = new BoundingBox(0, -5, 0, 8, 2, 2);
            }

            boundingBox.move(x, y, z);
            return holder.findCollisionPiece(boundingBox) != null ? null : boundingBox;
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            int i = this.getGenDepth();
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                default:
                    MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, i);
                    break;
                case SOUTH:
                    MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, i);
                    break;
                case WEST:
                    MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.WEST, i);
                    break;
                case EAST:
                    MineShaftPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.EAST, i);
                }
            }

        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            if (!this.edgesLiquid(world, chunkBox)) {
                this.generateBox(world, chunkBox, 0, 5, 0, 2, 7, 1, CAVE_AIR, CAVE_AIR, false);
                this.generateBox(world, chunkBox, 0, 0, 7, 2, 2, 8, CAVE_AIR, CAVE_AIR, false);

                for(int i = 0; i < 5; ++i) {
                    this.generateBox(world, chunkBox, 0, 5 - i - (i < 4 ? 1 : 0), 2 + i, 2, 7 - i, 2 + i, CAVE_AIR, CAVE_AIR, false);
                }

            }
        }
    }
}
