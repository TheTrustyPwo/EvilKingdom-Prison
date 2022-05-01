package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class NetherBridgePieces {
    private static final int MAX_DEPTH = 30;
    private static final int LOWEST_Y_POSITION = 10;
    public static final int MAGIC_START_Y = 64;
    static final NetherBridgePieces.PieceWeight[] BRIDGE_PIECE_WEIGHTS = new NetherBridgePieces.PieceWeight[]{new NetherBridgePieces.PieceWeight(NetherBridgePieces.BridgeStraight.class, 30, 0, true), new NetherBridgePieces.PieceWeight(NetherBridgePieces.BridgeCrossing.class, 10, 4), new NetherBridgePieces.PieceWeight(NetherBridgePieces.RoomCrossing.class, 10, 4), new NetherBridgePieces.PieceWeight(NetherBridgePieces.StairsRoom.class, 10, 3), new NetherBridgePieces.PieceWeight(NetherBridgePieces.MonsterThrone.class, 5, 2), new NetherBridgePieces.PieceWeight(NetherBridgePieces.CastleEntrance.class, 5, 1)};
    static final NetherBridgePieces.PieceWeight[] CASTLE_PIECE_WEIGHTS = new NetherBridgePieces.PieceWeight[]{new NetherBridgePieces.PieceWeight(NetherBridgePieces.CastleSmallCorridorPiece.class, 25, 0, true), new NetherBridgePieces.PieceWeight(NetherBridgePieces.CastleSmallCorridorCrossingPiece.class, 15, 5), new NetherBridgePieces.PieceWeight(NetherBridgePieces.CastleSmallCorridorRightTurnPiece.class, 5, 10), new NetherBridgePieces.PieceWeight(NetherBridgePieces.CastleSmallCorridorLeftTurnPiece.class, 5, 10), new NetherBridgePieces.PieceWeight(NetherBridgePieces.CastleCorridorStairsPiece.class, 10, 3, true), new NetherBridgePieces.PieceWeight(NetherBridgePieces.CastleCorridorTBalconyPiece.class, 7, 2), new NetherBridgePieces.PieceWeight(NetherBridgePieces.CastleStalkRoom.class, 5, 2)};

    static NetherBridgePieces.NetherBridgePiece findAndCreateBridgePieceFactory(NetherBridgePieces.PieceWeight pieceData, StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
        Class<? extends NetherBridgePieces.NetherBridgePiece> class_ = pieceData.pieceClass;
        NetherBridgePieces.NetherBridgePiece netherBridgePiece = null;
        if (class_ == NetherBridgePieces.BridgeStraight.class) {
            netherBridgePiece = NetherBridgePieces.BridgeStraight.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (class_ == NetherBridgePieces.BridgeCrossing.class) {
            netherBridgePiece = NetherBridgePieces.BridgeCrossing.createPiece(holder, x, y, z, orientation, chainLength);
        } else if (class_ == NetherBridgePieces.RoomCrossing.class) {
            netherBridgePiece = NetherBridgePieces.RoomCrossing.createPiece(holder, x, y, z, orientation, chainLength);
        } else if (class_ == NetherBridgePieces.StairsRoom.class) {
            netherBridgePiece = NetherBridgePieces.StairsRoom.createPiece(holder, x, y, z, chainLength, orientation);
        } else if (class_ == NetherBridgePieces.MonsterThrone.class) {
            netherBridgePiece = NetherBridgePieces.MonsterThrone.createPiece(holder, x, y, z, chainLength, orientation);
        } else if (class_ == NetherBridgePieces.CastleEntrance.class) {
            netherBridgePiece = NetherBridgePieces.CastleEntrance.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (class_ == NetherBridgePieces.CastleSmallCorridorPiece.class) {
            netherBridgePiece = NetherBridgePieces.CastleSmallCorridorPiece.createPiece(holder, x, y, z, orientation, chainLength);
        } else if (class_ == NetherBridgePieces.CastleSmallCorridorRightTurnPiece.class) {
            netherBridgePiece = NetherBridgePieces.CastleSmallCorridorRightTurnPiece.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (class_ == NetherBridgePieces.CastleSmallCorridorLeftTurnPiece.class) {
            netherBridgePiece = NetherBridgePieces.CastleSmallCorridorLeftTurnPiece.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (class_ == NetherBridgePieces.CastleCorridorStairsPiece.class) {
            netherBridgePiece = NetherBridgePieces.CastleCorridorStairsPiece.createPiece(holder, x, y, z, orientation, chainLength);
        } else if (class_ == NetherBridgePieces.CastleCorridorTBalconyPiece.class) {
            netherBridgePiece = NetherBridgePieces.CastleCorridorTBalconyPiece.createPiece(holder, x, y, z, orientation, chainLength);
        } else if (class_ == NetherBridgePieces.CastleSmallCorridorCrossingPiece.class) {
            netherBridgePiece = NetherBridgePieces.CastleSmallCorridorCrossingPiece.createPiece(holder, x, y, z, orientation, chainLength);
        } else if (class_ == NetherBridgePieces.CastleStalkRoom.class) {
            netherBridgePiece = NetherBridgePieces.CastleStalkRoom.createPiece(holder, x, y, z, orientation, chainLength);
        }

        return netherBridgePiece;
    }

    public static class BridgeCrossing extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 19;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 19;

        public BridgeCrossing(int chainLength, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        protected BridgeCrossing(int x, int z, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, 0, StructurePiece.makeBoundingBox(x, 64, z, orientation, 19, 10, 19));
            this.setOrientation(orientation);
        }

        protected BridgeCrossing(StructurePieceType type, CompoundTag nbt) {
            super(type, nbt);
        }

        public BridgeCrossing(CompoundTag nbt) {
            this(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateChildForward((NetherBridgePieces.StartPiece)start, holder, random, 8, 3, false);
            this.generateChildLeft((NetherBridgePieces.StartPiece)start, holder, random, 3, 8, false);
            this.generateChildRight((NetherBridgePieces.StartPiece)start, holder, random, 3, 8, false);
        }

        public static NetherBridgePieces.BridgeCrossing createPiece(StructurePieceAccessor holder, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -8, -3, 0, 19, 10, 19, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.BridgeCrossing(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 7, 3, 0, 11, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 3, 7, 18, 4, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, 5, 0, 10, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 5, 8, 18, 7, 10, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 7, 5, 0, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 7, 5, 11, 7, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 11, 5, 0, 11, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 11, 5, 11, 11, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 5, 7, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 11, 5, 7, 18, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 5, 11, 7, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 11, 5, 11, 18, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 7, 2, 0, 11, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 7, 2, 13, 11, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 7, 0, 0, 11, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 7, 0, 15, 11, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for(int i = 7; i <= 11; ++i) {
                for(int j = 0; j <= 2; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, chunkBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, 18 - j, chunkBox);
                }
            }

            this.generateBox(world, chunkBox, 0, 2, 7, 5, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 13, 2, 7, 18, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 0, 7, 3, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 15, 0, 7, 18, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for(int k = 0; k <= 2; ++k) {
                for(int l = 7; l <= 11; ++l) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), k, -1, l, chunkBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), 18 - k, -1, l, chunkBox);
                }
            }

        }
    }

    public static class BridgeEndFiller extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 8;
        private final int selfSeed;

        public BridgeEndFiller(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.selfSeed = random.nextInt();
        }

        public BridgeEndFiller(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, nbt);
            this.selfSeed = nbt.getInt("Seed");
        }

        public static NetherBridgePieces.BridgeEndFiller createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, -3, 0, 5, 10, 8, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.BridgeEndFiller(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putInt("Seed", this.selfSeed);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            Random random2 = new Random((long)this.selfSeed);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 3; j <= 4; ++j) {
                    int k = random2.nextInt(8);
                    this.generateBox(world, chunkBox, i, j, 0, i, j, k, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                }
            }

            int l = random2.nextInt(8);
            this.generateBox(world, chunkBox, 0, 5, 0, 0, 5, l, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            l = random2.nextInt(8);
            this.generateBox(world, chunkBox, 4, 5, 0, 4, 5, l, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for(int n = 0; n <= 4; ++n) {
                int o = random2.nextInt(5);
                this.generateBox(world, chunkBox, n, 2, 0, n, 2, o, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            }

            for(int p = 0; p <= 4; ++p) {
                for(int q = 0; q <= 1; ++q) {
                    int r = random2.nextInt(3);
                    this.generateBox(world, chunkBox, p, q, 0, p, q, r, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                }
            }

        }
    }

    public static class BridgeStraight extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 19;

        public BridgeStraight(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public BridgeStraight(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateChildForward((NetherBridgePieces.StartPiece)start, holder, random, 1, 3, false);
        }

        public static NetherBridgePieces.BridgeStraight createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, -3, 0, 5, 10, 19, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.BridgeStraight(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 3, 0, 4, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 5, 0, 3, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 5, 0, 0, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 5, 0, 4, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 4, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 13, 4, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 0, 15, 4, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 0; j <= 2; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, chunkBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, 18 - j, chunkBox);
                }
            }

            BlockState blockState = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
            BlockState blockState2 = blockState.setValue(FenceBlock.EAST, Boolean.valueOf(true));
            BlockState blockState3 = blockState.setValue(FenceBlock.WEST, Boolean.valueOf(true));
            this.generateBox(world, chunkBox, 0, 1, 1, 0, 4, 1, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 0, 3, 4, 0, 4, 4, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 0, 3, 14, 0, 4, 14, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 0, 1, 17, 0, 4, 17, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 4, 1, 1, 4, 4, 1, blockState3, blockState3, false);
            this.generateBox(world, chunkBox, 4, 3, 4, 4, 4, 4, blockState3, blockState3, false);
            this.generateBox(world, chunkBox, 4, 3, 14, 4, 4, 14, blockState3, blockState3, false);
            this.generateBox(world, chunkBox, 4, 1, 17, 4, 4, 17, blockState3, blockState3, false);
        }
    }

    public static class CastleCorridorStairsPiece extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 10;

        public CastleCorridorStairsPiece(int chainLength, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public CastleCorridorStairsPiece(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateChildForward((NetherBridgePieces.StartPiece)start, holder, random, 1, 0, true);
        }

        public static NetherBridgePieces.CastleCorridorStairsPiece createPiece(StructurePieceAccessor holder, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, -7, 0, 5, 14, 10, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.CastleCorridorStairsPiece(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            BlockState blockState = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            BlockState blockState2 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));

            for(int i = 0; i <= 9; ++i) {
                int j = Math.max(1, 7 - i);
                int k = Math.min(Math.max(j + 5, 14 - i), 13);
                int l = i;
                this.generateBox(world, chunkBox, 0, 0, i, 4, j, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                this.generateBox(world, chunkBox, 1, j + 1, i, 3, k - 1, i, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
                if (i <= 6) {
                    this.placeBlock(world, blockState, 1, j + 1, i, chunkBox);
                    this.placeBlock(world, blockState, 2, j + 1, i, chunkBox);
                    this.placeBlock(world, blockState, 3, j + 1, i, chunkBox);
                }

                this.generateBox(world, chunkBox, 0, k, i, 4, k, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                this.generateBox(world, chunkBox, 0, j + 1, i, 0, k - 1, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                this.generateBox(world, chunkBox, 4, j + 1, i, 4, k - 1, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                if ((i & 1) == 0) {
                    this.generateBox(world, chunkBox, 0, j + 2, i, 0, j + 3, i, blockState2, blockState2, false);
                    this.generateBox(world, chunkBox, 4, j + 2, i, 4, j + 3, i, blockState2, blockState2, false);
                }

                for(int m = 0; m <= 4; ++m) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), m, -1, l, chunkBox);
                }
            }

        }
    }

    public static class CastleCorridorTBalconyPiece extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 9;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 9;

        public CastleCorridorTBalconyPiece(int chainLength, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public CastleCorridorTBalconyPiece(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            int i = 1;
            Direction direction = this.getOrientation();
            if (direction == Direction.WEST || direction == Direction.NORTH) {
                i = 5;
            }

            this.generateChildLeft((NetherBridgePieces.StartPiece)start, holder, random, 0, i, random.nextInt(8) > 0);
            this.generateChildRight((NetherBridgePieces.StartPiece)start, holder, random, 0, i, random.nextInt(8) > 0);
        }

        public static NetherBridgePieces.CastleCorridorTBalconyPiece createPiece(StructurePieceAccessor holder, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -3, 0, 0, 9, 7, 9, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.CastleCorridorTBalconyPiece(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            BlockState blockState = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
            BlockState blockState2 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true));
            this.generateBox(world, chunkBox, 0, 0, 0, 8, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 8, 5, 8, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 6, 0, 8, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 2, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 6, 2, 0, 8, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 3, 0, 1, 4, 0, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 7, 3, 0, 7, 4, 0, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 0, 2, 4, 8, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 1, 4, 2, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 6, 1, 4, 7, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 3, 8, 7, 3, 8, blockState2, blockState2, false);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)), 0, 3, 8, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)), 8, 3, 8, chunkBox);
            this.generateBox(world, chunkBox, 0, 3, 6, 0, 3, 7, blockState, blockState, false);
            this.generateBox(world, chunkBox, 8, 3, 6, 8, 3, 7, blockState, blockState, false);
            this.generateBox(world, chunkBox, 0, 3, 4, 0, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, 3, 4, 8, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 3, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 6, 3, 5, 7, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 4, 5, 1, 5, 5, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 7, 4, 5, 7, 5, 5, blockState2, blockState2, false);

            for(int i = 0; i <= 5; ++i) {
                for(int j = 0; j <= 8; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), j, -1, i, chunkBox);
                }
            }

        }
    }

    public static class CastleEntrance extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 13;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 13;

        public CastleEntrance(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public CastleEntrance(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateChildForward((NetherBridgePieces.StartPiece)start, holder, random, 5, 3, true);
        }

        public static NetherBridgePieces.CastleEntrance createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -5, -3, 0, 13, 14, 13, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.CastleEntrance(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 8, 0, 7, 8, 0, Blocks.NETHER_BRICK_FENCE.defaultBlockState(), Blocks.NETHER_BRICK_FENCE.defaultBlockState(), false);
            BlockState blockState = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true));
            BlockState blockState2 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));

            for(int i = 1; i <= 11; i += 2) {
                this.generateBox(world, chunkBox, i, 10, 0, i, 11, 0, blockState, blockState, false);
                this.generateBox(world, chunkBox, i, 10, 12, i, 11, 12, blockState, blockState, false);
                this.generateBox(world, chunkBox, 0, 10, i, 0, 11, i, blockState2, blockState2, false);
                this.generateBox(world, chunkBox, 12, 10, i, 12, 11, i, blockState2, blockState2, false);
                this.placeBlock(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, chunkBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, chunkBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, chunkBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, chunkBox);
                if (i != 11) {
                    this.placeBlock(world, blockState, i + 1, 13, 0, chunkBox);
                    this.placeBlock(world, blockState, i + 1, 13, 12, chunkBox);
                    this.placeBlock(world, blockState2, 0, 13, i + 1, chunkBox);
                    this.placeBlock(world, blockState2, 12, 13, i + 1, chunkBox);
                }
            }

            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)), 0, 13, 0, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)), 0, 13, 12, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)), 12, 13, 12, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)), 12, 13, 0, chunkBox);

            for(int j = 3; j <= 9; j += 2) {
                this.generateBox(world, chunkBox, 1, 7, j, 1, 8, j, blockState2.setValue(FenceBlock.WEST, Boolean.valueOf(true)), blockState2.setValue(FenceBlock.WEST, Boolean.valueOf(true)), false);
                this.generateBox(world, chunkBox, 11, 7, j, 11, 8, j, blockState2.setValue(FenceBlock.EAST, Boolean.valueOf(true)), blockState2.setValue(FenceBlock.EAST, Boolean.valueOf(true)), false);
            }

            this.generateBox(world, chunkBox, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for(int k = 4; k <= 8; ++k) {
                for(int l = 0; l <= 2; ++l) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), k, -1, l, chunkBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), k, -1, 12 - l, chunkBox);
                }
            }

            for(int m = 0; m <= 2; ++m) {
                for(int n = 4; n <= 8; ++n) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), m, -1, n, chunkBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - m, -1, n, chunkBox);
                }
            }

            this.generateBox(world, chunkBox, 5, 5, 5, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 6, 1, 6, 6, 4, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.placeBlock(world, Blocks.NETHER_BRICKS.defaultBlockState(), 6, 0, 6, chunkBox);
            this.placeBlock(world, Blocks.LAVA.defaultBlockState(), 6, 5, 6, chunkBox);
            BlockPos blockPos = this.getWorldPos(6, 5, 6);
            if (chunkBox.isInside(blockPos)) {
                world.scheduleTick(blockPos, Fluids.LAVA, 0);
            }

        }
    }

    public static class CastleSmallCorridorCrossingPiece extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;

        public CastleSmallCorridorCrossingPiece(int chainLength, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public CastleSmallCorridorCrossingPiece(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateChildForward((NetherBridgePieces.StartPiece)start, holder, random, 1, 0, true);
            this.generateChildLeft((NetherBridgePieces.StartPiece)start, holder, random, 0, 1, true);
            this.generateChildRight((NetherBridgePieces.StartPiece)start, holder, random, 0, 1, true);
        }

        public static NetherBridgePieces.CastleSmallCorridorCrossingPiece createPiece(StructurePieceAccessor holder, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, 0, 0, 5, 7, 5, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.CastleSmallCorridorCrossingPiece(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 4, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 0; j <= 4; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, chunkBox);
                }
            }

        }
    }

    public static class CastleSmallCorridorLeftTurnPiece extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;
        private boolean isNeedingChest;

        public CastleSmallCorridorLeftTurnPiece(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.isNeedingChest = random.nextInt(3) == 0;
        }

        public CastleSmallCorridorLeftTurnPiece(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, nbt);
            this.isNeedingChest = nbt.getBoolean("Chest");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("Chest", this.isNeedingChest);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateChildLeft((NetherBridgePieces.StartPiece)start, holder, random, 0, 1, true);
        }

        public static NetherBridgePieces.CastleSmallCorridorLeftTurnPiece createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, 0, 0, 5, 7, 5, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.CastleSmallCorridorLeftTurnPiece(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            BlockState blockState = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true));
            BlockState blockState2 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
            this.generateBox(world, chunkBox, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 3, 1, 4, 4, 1, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 4, 3, 3, 4, 4, 3, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 4, 3, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 3, 4, 1, 4, 4, blockState, blockState, false);
            this.generateBox(world, chunkBox, 3, 3, 4, 3, 4, 4, blockState, blockState, false);
            if (this.isNeedingChest && chunkBox.isInside(this.getWorldPos(3, 2, 3))) {
                this.isNeedingChest = false;
                this.createChest(world, chunkBox, random, 3, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
            }

            this.generateBox(world, chunkBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 0; j <= 4; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, chunkBox);
                }
            }

        }
    }

    public static class CastleSmallCorridorPiece extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;

        public CastleSmallCorridorPiece(int chainLength, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public CastleSmallCorridorPiece(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateChildForward((NetherBridgePieces.StartPiece)start, holder, random, 1, 0, true);
        }

        public static NetherBridgePieces.CastleSmallCorridorPiece createPiece(StructurePieceAccessor holder, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, 0, 0, 5, 7, 5, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.CastleSmallCorridorPiece(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            BlockState blockState = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
            this.generateBox(world, chunkBox, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 3, 1, 0, 4, 1, blockState, blockState, false);
            this.generateBox(world, chunkBox, 0, 3, 3, 0, 4, 3, blockState, blockState, false);
            this.generateBox(world, chunkBox, 4, 3, 1, 4, 4, 1, blockState, blockState, false);
            this.generateBox(world, chunkBox, 4, 3, 3, 4, 4, 3, blockState, blockState, false);
            this.generateBox(world, chunkBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 0; j <= 4; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, chunkBox);
                }
            }

        }
    }

    public static class CastleSmallCorridorRightTurnPiece extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;
        private boolean isNeedingChest;

        public CastleSmallCorridorRightTurnPiece(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.isNeedingChest = random.nextInt(3) == 0;
        }

        public CastleSmallCorridorRightTurnPiece(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, nbt);
            this.isNeedingChest = nbt.getBoolean("Chest");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("Chest", this.isNeedingChest);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateChildRight((NetherBridgePieces.StartPiece)start, holder, random, 0, 1, true);
        }

        public static NetherBridgePieces.CastleSmallCorridorRightTurnPiece createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, 0, 0, 5, 7, 5, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.CastleSmallCorridorRightTurnPiece(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            BlockState blockState = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true));
            BlockState blockState2 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
            this.generateBox(world, chunkBox, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 3, 1, 0, 4, 1, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 0, 3, 3, 0, 4, 3, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 3, 4, 1, 4, 4, blockState, blockState, false);
            this.generateBox(world, chunkBox, 3, 3, 4, 3, 4, 4, blockState, blockState, false);
            if (this.isNeedingChest && chunkBox.isInside(this.getWorldPos(1, 2, 3))) {
                this.isNeedingChest = false;
                this.createChest(world, chunkBox, random, 1, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
            }

            this.generateBox(world, chunkBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for(int i = 0; i <= 4; ++i) {
                for(int j = 0; j <= 4; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, chunkBox);
                }
            }

        }
    }

    public static class CastleStalkRoom extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 13;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 13;

        public CastleStalkRoom(int chainLength, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public CastleStalkRoom(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateChildForward((NetherBridgePieces.StartPiece)start, holder, random, 5, 3, true);
            this.generateChildForward((NetherBridgePieces.StartPiece)start, holder, random, 5, 11, true);
        }

        public static NetherBridgePieces.CastleStalkRoom createPiece(StructurePieceAccessor holder, int x, int y, int z, Direction orientation, int chainlength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -5, -3, 0, 13, 14, 13, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.CastleStalkRoom(chainlength, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState blockState = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true));
            BlockState blockState2 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
            BlockState blockState3 = blockState2.setValue(FenceBlock.WEST, Boolean.valueOf(true));
            BlockState blockState4 = blockState2.setValue(FenceBlock.EAST, Boolean.valueOf(true));

            for(int i = 1; i <= 11; i += 2) {
                this.generateBox(world, chunkBox, i, 10, 0, i, 11, 0, blockState, blockState, false);
                this.generateBox(world, chunkBox, i, 10, 12, i, 11, 12, blockState, blockState, false);
                this.generateBox(world, chunkBox, 0, 10, i, 0, 11, i, blockState2, blockState2, false);
                this.generateBox(world, chunkBox, 12, 10, i, 12, 11, i, blockState2, blockState2, false);
                this.placeBlock(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, chunkBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, chunkBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, chunkBox);
                this.placeBlock(world, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, chunkBox);
                if (i != 11) {
                    this.placeBlock(world, blockState, i + 1, 13, 0, chunkBox);
                    this.placeBlock(world, blockState, i + 1, 13, 12, chunkBox);
                    this.placeBlock(world, blockState2, 0, 13, i + 1, chunkBox);
                    this.placeBlock(world, blockState2, 12, 13, i + 1, chunkBox);
                }
            }

            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)), 0, 13, 0, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)), 0, 13, 12, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)), 12, 13, 12, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)), 12, 13, 0, chunkBox);

            for(int j = 3; j <= 9; j += 2) {
                this.generateBox(world, chunkBox, 1, 7, j, 1, 8, j, blockState3, blockState3, false);
                this.generateBox(world, chunkBox, 11, 7, j, 11, 8, j, blockState4, blockState4, false);
            }

            BlockState blockState5 = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);

            for(int k = 0; k <= 6; ++k) {
                int l = k + 4;

                for(int m = 5; m <= 7; ++m) {
                    this.placeBlock(world, blockState5, m, 5 + k, l, chunkBox);
                }

                if (l >= 5 && l <= 8) {
                    this.generateBox(world, chunkBox, 5, 5, l, 7, k + 4, l, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                } else if (l >= 9 && l <= 10) {
                    this.generateBox(world, chunkBox, 5, 8, l, 7, k + 4, l, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                }

                if (k >= 1) {
                    this.generateBox(world, chunkBox, 5, 6 + k, l, 7, 9 + k, l, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
                }
            }

            for(int n = 5; n <= 7; ++n) {
                this.placeBlock(world, blockState5, n, 12, 11, chunkBox);
            }

            this.generateBox(world, chunkBox, 5, 6, 7, 5, 7, 7, blockState4, blockState4, false);
            this.generateBox(world, chunkBox, 7, 6, 7, 7, 7, 7, blockState3, blockState3, false);
            this.generateBox(world, chunkBox, 5, 13, 12, 7, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 5, 2, 3, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 5, 9, 3, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 5, 4, 2, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 9, 5, 2, 10, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 9, 5, 9, 10, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 10, 5, 4, 10, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState blockState6 = blockState5.setValue(StairBlock.FACING, Direction.EAST);
            BlockState blockState7 = blockState5.setValue(StairBlock.FACING, Direction.WEST);
            this.placeBlock(world, blockState7, 4, 5, 2, chunkBox);
            this.placeBlock(world, blockState7, 4, 5, 3, chunkBox);
            this.placeBlock(world, blockState7, 4, 5, 9, chunkBox);
            this.placeBlock(world, blockState7, 4, 5, 10, chunkBox);
            this.placeBlock(world, blockState6, 8, 5, 2, chunkBox);
            this.placeBlock(world, blockState6, 8, 5, 3, chunkBox);
            this.placeBlock(world, blockState6, 8, 5, 9, chunkBox);
            this.placeBlock(world, blockState6, 8, 5, 10, chunkBox);
            this.generateBox(world, chunkBox, 3, 4, 4, 4, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, 4, 4, 9, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 3, 5, 4, 4, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, 5, 4, 9, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

            for(int o = 4; o <= 8; ++o) {
                for(int p = 0; p <= 2; ++p) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), o, -1, p, chunkBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), o, -1, 12 - p, chunkBox);
                }
            }

            for(int q = 0; q <= 2; ++q) {
                for(int r = 4; r <= 8; ++r) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), q, -1, r, chunkBox);
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - q, -1, r, chunkBox);
                }
            }

        }
    }

    public static class MonsterThrone extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 8;
        private static final int DEPTH = 9;
        private boolean hasPlacedSpawner;

        public MonsterThrone(int chainLength, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public MonsterThrone(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, nbt);
            this.hasPlacedSpawner = nbt.getBoolean("Mob");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("Mob", this.hasPlacedSpawner);
        }

        public static NetherBridgePieces.MonsterThrone createPiece(StructurePieceAccessor holder, int x, int y, int z, int chainLength, Direction orientation) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -2, 0, 0, 7, 8, 9, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.MonsterThrone(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 2, 0, 6, 7, 7, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 0, 0, 5, 1, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 2, 1, 5, 2, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 3, 2, 5, 3, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 4, 3, 5, 4, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 2, 0, 1, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 2, 0, 5, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 5, 2, 1, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 5, 2, 5, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 5, 3, 0, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 6, 5, 3, 6, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 5, 8, 5, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState blockState = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true));
            BlockState blockState2 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)), 1, 6, 3, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)), 5, 6, 3, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)).setValue(FenceBlock.NORTH, Boolean.valueOf(true)), 0, 6, 3, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.NORTH, Boolean.valueOf(true)), 6, 6, 3, chunkBox);
            this.generateBox(world, chunkBox, 0, 6, 4, 0, 6, 7, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 6, 6, 4, 6, 6, 7, blockState2, blockState2, false);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)), 0, 6, 8, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)), 6, 6, 8, chunkBox);
            this.generateBox(world, chunkBox, 1, 6, 8, 5, 6, 8, blockState, blockState, false);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)), 1, 7, 8, chunkBox);
            this.generateBox(world, chunkBox, 2, 7, 8, 4, 7, 8, blockState, blockState, false);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)), 5, 7, 8, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)), 2, 8, 8, chunkBox);
            this.placeBlock(world, blockState, 3, 8, 8, chunkBox);
            this.placeBlock(world, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)), 4, 8, 8, chunkBox);
            if (!this.hasPlacedSpawner) {
                BlockPos blockPos = this.getWorldPos(3, 5, 5);
                if (chunkBox.isInside(blockPos)) {
                    this.hasPlacedSpawner = true;
                    world.setBlock(blockPos, Blocks.SPAWNER.defaultBlockState(), 2);
                    BlockEntity blockEntity = world.getBlockEntity(blockPos);
                    if (blockEntity instanceof SpawnerBlockEntity) {
                        ((SpawnerBlockEntity)blockEntity).getSpawner().setEntityId(EntityType.BLAZE);
                    }
                }
            }

            for(int i = 0; i <= 6; ++i) {
                for(int j = 0; j <= 6; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, chunkBox);
                }
            }

        }
    }

    abstract static class NetherBridgePiece extends StructurePiece {
        protected NetherBridgePiece(StructurePieceType type, int length, BoundingBox boundingBox) {
            super(type, length, boundingBox);
        }

        public NetherBridgePiece(StructurePieceType type, CompoundTag nbt) {
            super(type, nbt);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        }

        private int updatePieceWeight(List<NetherBridgePieces.PieceWeight> possiblePieces) {
            boolean bl = false;
            int i = 0;

            for(NetherBridgePieces.PieceWeight pieceWeight : possiblePieces) {
                if (pieceWeight.maxPlaceCount > 0 && pieceWeight.placeCount < pieceWeight.maxPlaceCount) {
                    bl = true;
                }

                i += pieceWeight.weight;
            }

            return bl ? i : -1;
        }

        private NetherBridgePieces.NetherBridgePiece generatePiece(NetherBridgePieces.StartPiece start, List<NetherBridgePieces.PieceWeight> possiblePieces, StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            int i = this.updatePieceWeight(possiblePieces);
            boolean bl = i > 0 && chainLength <= 30;
            int j = 0;

            while(j < 5 && bl) {
                ++j;
                int k = random.nextInt(i);

                for(NetherBridgePieces.PieceWeight pieceWeight : possiblePieces) {
                    k -= pieceWeight.weight;
                    if (k < 0) {
                        if (!pieceWeight.doPlace(chainLength) || pieceWeight == start.previousPiece && !pieceWeight.allowInRow) {
                            break;
                        }

                        NetherBridgePieces.NetherBridgePiece netherBridgePiece = NetherBridgePieces.findAndCreateBridgePieceFactory(pieceWeight, holder, random, x, y, z, orientation, chainLength);
                        if (netherBridgePiece != null) {
                            ++pieceWeight.placeCount;
                            start.previousPiece = pieceWeight;
                            if (!pieceWeight.isValid()) {
                                possiblePieces.remove(pieceWeight);
                            }

                            return netherBridgePiece;
                        }
                    }
                }
            }

            return NetherBridgePieces.BridgeEndFiller.createPiece(holder, random, x, y, z, orientation, chainLength);
        }

        private StructurePiece generateAndAddPiece(NetherBridgePieces.StartPiece start, StructurePieceAccessor holder, Random random, int x, int y, int z, @Nullable Direction orientation, int chainLength, boolean inside) {
            if (Math.abs(x - start.getBoundingBox().minX()) <= 112 && Math.abs(z - start.getBoundingBox().minZ()) <= 112) {
                List<NetherBridgePieces.PieceWeight> list = start.availableBridgePieces;
                if (inside) {
                    list = start.availableCastlePieces;
                }

                StructurePiece structurePiece = this.generatePiece(start, list, holder, random, x, y, z, orientation, chainLength + 1);
                if (structurePiece != null) {
                    holder.addPiece(structurePiece);
                    start.pendingChildren.add(structurePiece);
                }

                return structurePiece;
            } else {
                return NetherBridgePieces.BridgeEndFiller.createPiece(holder, random, x, y, z, orientation, chainLength);
            }
        }

        @Nullable
        protected StructurePiece generateChildForward(NetherBridgePieces.StartPiece start, StructurePieceAccessor holder, Random random, int leftRightOffset, int heightOffset, boolean inside) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, direction, this.getGenDepth(), inside);
                case SOUTH:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, direction, this.getGenDepth(), inside);
                case WEST:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, direction, this.getGenDepth(), inside);
                case EAST:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, direction, this.getGenDepth(), inside);
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateChildLeft(NetherBridgePieces.StartPiece start, StructurePieceAccessor holder, Random random, int heightOffset, int leftRightOffset, boolean inside) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, Direction.WEST, this.getGenDepth(), inside);
                case SOUTH:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, Direction.WEST, this.getGenDepth(), inside);
                case WEST:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, Direction.NORTH, this.getGenDepth(), inside);
                case EAST:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, Direction.NORTH, this.getGenDepth(), inside);
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateChildRight(NetherBridgePieces.StartPiece start, StructurePieceAccessor holder, Random random, int heightOffset, int leftRightOffset, boolean inside) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, Direction.EAST, this.getGenDepth(), inside);
                case SOUTH:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, Direction.EAST, this.getGenDepth(), inside);
                case WEST:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, Direction.SOUTH, this.getGenDepth(), inside);
                case EAST:
                    return this.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, Direction.SOUTH, this.getGenDepth(), inside);
                }
            }

            return null;
        }

        protected static boolean isOkBox(BoundingBox boundingBox) {
            return boundingBox != null && boundingBox.minY() > 10;
        }
    }

    static class PieceWeight {
        public final Class<? extends NetherBridgePieces.NetherBridgePiece> pieceClass;
        public final int weight;
        public int placeCount;
        public final int maxPlaceCount;
        public final boolean allowInRow;

        public PieceWeight(Class<? extends NetherBridgePieces.NetherBridgePiece> pieceType, int weight, int limit, boolean repeatable) {
            this.pieceClass = pieceType;
            this.weight = weight;
            this.maxPlaceCount = limit;
            this.allowInRow = repeatable;
        }

        public PieceWeight(Class<? extends NetherBridgePieces.NetherBridgePiece> pieceType, int weight, int limit) {
            this(pieceType, weight, limit, false);
        }

        public boolean doPlace(int chainLength) {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }

        public boolean isValid() {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }
    }

    public static class RoomCrossing extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 9;
        private static final int DEPTH = 7;

        public RoomCrossing(int chainLength, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public RoomCrossing(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateChildForward((NetherBridgePieces.StartPiece)start, holder, random, 2, 0, false);
            this.generateChildLeft((NetherBridgePieces.StartPiece)start, holder, random, 0, 2, false);
            this.generateChildRight((NetherBridgePieces.StartPiece)start, holder, random, 0, 2, false);
        }

        public static NetherBridgePieces.RoomCrossing createPiece(StructurePieceAccessor holder, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -2, 0, 0, 7, 9, 7, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.RoomCrossing(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 6, 7, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 1, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 6, 1, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 2, 0, 6, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 2, 6, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 0, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 5, 0, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 6, 2, 0, 6, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 6, 2, 5, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState blockState = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true));
            BlockState blockState2 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
            this.generateBox(world, chunkBox, 2, 6, 0, 4, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 5, 0, 4, 5, 0, blockState, blockState, false);
            this.generateBox(world, chunkBox, 2, 6, 6, 4, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 5, 6, 4, 5, 6, blockState, blockState, false);
            this.generateBox(world, chunkBox, 0, 6, 2, 0, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 5, 2, 0, 5, 4, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 6, 6, 2, 6, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 6, 5, 2, 6, 5, 4, blockState2, blockState2, false);

            for(int i = 0; i <= 6; ++i) {
                for(int j = 0; j <= 6; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, chunkBox);
                }
            }

        }
    }

    public static class StairsRoom extends NetherBridgePieces.NetherBridgePiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 7;

        public StairsRoom(int chainLength, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public StairsRoom(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateChildRight((NetherBridgePieces.StartPiece)start, holder, random, 6, 2, false);
        }

        public static NetherBridgePieces.StairsRoom createPiece(StructurePieceAccessor holder, int x, int y, int z, int chainlength, Direction orientation) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -2, 0, 0, 7, 11, 7, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new NetherBridgePieces.StairsRoom(chainlength, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 6, 10, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 0, 1, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 2, 0, 6, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 0, 2, 1, 0, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 6, 2, 1, 6, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 2, 6, 5, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState blockState = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true));
            BlockState blockState2 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
            this.generateBox(world, chunkBox, 0, 3, 2, 0, 5, 4, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 6, 3, 2, 6, 5, 2, blockState2, blockState2, false);
            this.generateBox(world, chunkBox, 6, 3, 4, 6, 5, 4, blockState2, blockState2, false);
            this.placeBlock(world, Blocks.NETHER_BRICKS.defaultBlockState(), 5, 2, 5, chunkBox);
            this.generateBox(world, chunkBox, 4, 2, 5, 4, 3, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 3, 2, 5, 3, 4, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 2, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 2, 5, 1, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 7, 1, 5, 7, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 6, 8, 2, 6, 8, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 6, 0, 4, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 2, 5, 0, 4, 5, 0, blockState, blockState, false);

            for(int i = 0; i <= 6; ++i) {
                for(int j = 0; j <= 6; ++j) {
                    this.fillColumnDown(world, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, chunkBox);
                }
            }

        }
    }

    public static class StartPiece extends NetherBridgePieces.BridgeCrossing {
        public NetherBridgePieces.PieceWeight previousPiece;
        public List<NetherBridgePieces.PieceWeight> availableBridgePieces;
        public List<NetherBridgePieces.PieceWeight> availableCastlePieces;
        public final List<StructurePiece> pendingChildren = Lists.newArrayList();

        public StartPiece(Random random, int x, int z) {
            super(x, z, getRandomHorizontalDirection(random));
            this.availableBridgePieces = Lists.newArrayList();

            for(NetherBridgePieces.PieceWeight pieceWeight : NetherBridgePieces.BRIDGE_PIECE_WEIGHTS) {
                pieceWeight.placeCount = 0;
                this.availableBridgePieces.add(pieceWeight);
            }

            this.availableCastlePieces = Lists.newArrayList();

            for(NetherBridgePieces.PieceWeight pieceWeight2 : NetherBridgePieces.CASTLE_PIECE_WEIGHTS) {
                pieceWeight2.placeCount = 0;
                this.availableCastlePieces.add(pieceWeight2);
            }

        }

        public StartPiece(CompoundTag nbt) {
            super(StructurePieceType.NETHER_FORTRESS_START, nbt);
        }
    }
}
