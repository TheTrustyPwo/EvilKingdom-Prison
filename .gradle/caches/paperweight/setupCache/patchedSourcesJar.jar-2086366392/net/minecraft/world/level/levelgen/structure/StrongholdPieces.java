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
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.NoiseEffect;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class StrongholdPieces {
    private static final int SMALL_DOOR_WIDTH = 3;
    private static final int SMALL_DOOR_HEIGHT = 3;
    private static final int MAX_DEPTH = 50;
    private static final int LOWEST_Y_POSITION = 10;
    private static final boolean CHECK_AIR = true;
    public static final int MAGIC_START_Y = 64;
    private static final StrongholdPieces.PieceWeight[] STRONGHOLD_PIECE_WEIGHTS = new StrongholdPieces.PieceWeight[]{new StrongholdPieces.PieceWeight(StrongholdPieces.Straight.class, 40, 0), new StrongholdPieces.PieceWeight(StrongholdPieces.PrisonHall.class, 5, 5), new StrongholdPieces.PieceWeight(StrongholdPieces.LeftTurn.class, 20, 0), new StrongholdPieces.PieceWeight(StrongholdPieces.RightTurn.class, 20, 0), new StrongholdPieces.PieceWeight(StrongholdPieces.RoomCrossing.class, 10, 6), new StrongholdPieces.PieceWeight(StrongholdPieces.StraightStairsDown.class, 5, 5), new StrongholdPieces.PieceWeight(StrongholdPieces.StairsDown.class, 5, 5), new StrongholdPieces.PieceWeight(StrongholdPieces.FiveCrossing.class, 5, 4), new StrongholdPieces.PieceWeight(StrongholdPieces.ChestCorridor.class, 5, 4), new StrongholdPieces.PieceWeight(StrongholdPieces.Library.class, 10, 2) {
        @Override
        public boolean doPlace(int chainLength) {
            return super.doPlace(chainLength) && chainLength > 4;
        }
    }, new StrongholdPieces.PieceWeight(StrongholdPieces.PortalRoom.class, 20, 1) {
        @Override
        public boolean doPlace(int chainLength) {
            return super.doPlace(chainLength) && chainLength > 5;
        }
    }};
    private static List<StrongholdPieces.PieceWeight> currentPieces;
    static Class<? extends StrongholdPieces.StrongholdPiece> imposedPiece;
    private static int totalWeight;
    static final StrongholdPieces.SmoothStoneSelector SMOOTH_STONE_SELECTOR = new StrongholdPieces.SmoothStoneSelector();

    public static void resetPieces() {
        currentPieces = Lists.newArrayList();

        for(StrongholdPieces.PieceWeight pieceWeight : STRONGHOLD_PIECE_WEIGHTS) {
            pieceWeight.placeCount = 0;
            currentPieces.add(pieceWeight);
        }

        imposedPiece = null;
    }

    private static boolean updatePieceWeight() {
        boolean bl = false;
        totalWeight = 0;

        for(StrongholdPieces.PieceWeight pieceWeight : currentPieces) {
            if (pieceWeight.maxPlaceCount > 0 && pieceWeight.placeCount < pieceWeight.maxPlaceCount) {
                bl = true;
            }

            totalWeight += pieceWeight.weight;
        }

        return bl;
    }

    private static StrongholdPieces.StrongholdPiece findAndCreatePieceFactory(Class<? extends StrongholdPieces.StrongholdPiece> pieceType, StructurePieceAccessor holder, Random random, int x, int y, int z, @Nullable Direction orientation, int chainLength) {
        StrongholdPieces.StrongholdPiece strongholdPiece = null;
        if (pieceType == StrongholdPieces.Straight.class) {
            strongholdPiece = StrongholdPieces.Straight.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (pieceType == StrongholdPieces.PrisonHall.class) {
            strongholdPiece = StrongholdPieces.PrisonHall.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (pieceType == StrongholdPieces.LeftTurn.class) {
            strongholdPiece = StrongholdPieces.LeftTurn.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (pieceType == StrongholdPieces.RightTurn.class) {
            strongholdPiece = StrongholdPieces.RightTurn.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (pieceType == StrongholdPieces.RoomCrossing.class) {
            strongholdPiece = StrongholdPieces.RoomCrossing.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (pieceType == StrongholdPieces.StraightStairsDown.class) {
            strongholdPiece = StrongholdPieces.StraightStairsDown.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (pieceType == StrongholdPieces.StairsDown.class) {
            strongholdPiece = StrongholdPieces.StairsDown.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (pieceType == StrongholdPieces.FiveCrossing.class) {
            strongholdPiece = StrongholdPieces.FiveCrossing.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (pieceType == StrongholdPieces.ChestCorridor.class) {
            strongholdPiece = StrongholdPieces.ChestCorridor.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (pieceType == StrongholdPieces.Library.class) {
            strongholdPiece = StrongholdPieces.Library.createPiece(holder, random, x, y, z, orientation, chainLength);
        } else if (pieceType == StrongholdPieces.PortalRoom.class) {
            strongholdPiece = StrongholdPieces.PortalRoom.createPiece(holder, x, y, z, orientation, chainLength);
        }

        return strongholdPiece;
    }

    private static StrongholdPieces.StrongholdPiece generatePieceFromSmallDoor(StrongholdPieces.StartPiece start, StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
        if (!updatePieceWeight()) {
            return null;
        } else {
            if (imposedPiece != null) {
                StrongholdPieces.StrongholdPiece strongholdPiece = findAndCreatePieceFactory(imposedPiece, holder, random, x, y, z, orientation, chainLength);
                imposedPiece = null;
                if (strongholdPiece != null) {
                    return strongholdPiece;
                }
            }

            int i = 0;

            while(i < 5) {
                ++i;
                int j = random.nextInt(totalWeight);

                for(StrongholdPieces.PieceWeight pieceWeight : currentPieces) {
                    j -= pieceWeight.weight;
                    if (j < 0) {
                        if (!pieceWeight.doPlace(chainLength) || pieceWeight == start.previousPiece) {
                            break;
                        }

                        StrongholdPieces.StrongholdPiece strongholdPiece2 = findAndCreatePieceFactory(pieceWeight.pieceClass, holder, random, x, y, z, orientation, chainLength);
                        if (strongholdPiece2 != null) {
                            ++pieceWeight.placeCount;
                            start.previousPiece = pieceWeight;
                            if (!pieceWeight.isValid()) {
                                currentPieces.remove(pieceWeight);
                            }

                            return strongholdPiece2;
                        }
                    }
                }
            }

            BoundingBox boundingBox = StrongholdPieces.FillerCorridor.findPieceBox(holder, random, x, y, z, orientation);
            return boundingBox != null && boundingBox.minY() > 1 ? new StrongholdPieces.FillerCorridor(chainLength, boundingBox, orientation) : null;
        }
    }

    static StructurePiece generateAndAddPiece(StrongholdPieces.StartPiece start, StructurePieceAccessor holder, Random random, int x, int y, int z, @Nullable Direction orientation, int chainLength) {
        if (chainLength > 50) {
            return null;
        } else if (Math.abs(x - start.getBoundingBox().minX()) <= 112 && Math.abs(z - start.getBoundingBox().minZ()) <= 112) {
            StructurePiece structurePiece = generatePieceFromSmallDoor(start, holder, random, x, y, z, orientation, chainLength + 1);
            if (structurePiece != null) {
                holder.addPiece(structurePiece);
                start.pendingChildren.add(structurePiece);
            }

            return structurePiece;
        } else {
            return null;
        }
    }

    public static class ChestCorridor extends StrongholdPieces.StrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 5;
        private static final int DEPTH = 7;
        private boolean hasPlacedChest;

        public ChestCorridor(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public ChestCorridor(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, nbt);
            this.hasPlacedChest = nbt.getBoolean("Chest");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("Chest", this.hasPlacedChest);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)start, holder, random, 1, 1);
        }

        public static StrongholdPieces.ChestCorridor createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainlength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, 7, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new StrongholdPieces.ChestCorridor(chainlength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 4, 6, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, chunkBox, this.entryDoor, 1, 1, 0);
            this.generateSmallDoor(world, random, chunkBox, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
            this.generateBox(world, chunkBox, 3, 1, 2, 3, 1, 4, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), false);
            this.placeBlock(world, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 1, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 5, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 2, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 4, chunkBox);

            for(int i = 2; i <= 4; ++i) {
                this.placeBlock(world, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2, 1, i, chunkBox);
            }

            if (!this.hasPlacedChest && chunkBox.isInside(this.getWorldPos(3, 2, 3))) {
                this.hasPlacedChest = true;
                this.createChest(world, chunkBox, random, 3, 2, 3, BuiltInLootTables.STRONGHOLD_CORRIDOR);
            }

        }
    }

    public static class FillerCorridor extends StrongholdPieces.StrongholdPiece {
        private final int steps;

        public FillerCorridor(int chainLength, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.steps = orientation != Direction.NORTH && orientation != Direction.SOUTH ? boundingBox.getXSpan() : boundingBox.getZSpan();
        }

        public FillerCorridor(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, nbt);
            this.steps = nbt.getInt("Steps");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putInt("Steps", this.steps);
        }

        public static BoundingBox findPieceBox(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation) {
            int i = 3;
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, 4, orientation);
            StructurePiece structurePiece = holder.findCollisionPiece(boundingBox);
            if (structurePiece == null) {
                return null;
            } else {
                if (structurePiece.getBoundingBox().minY() == boundingBox.minY()) {
                    for(int j = 2; j >= 1; --j) {
                        boundingBox = BoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, j, orientation);
                        if (!structurePiece.getBoundingBox().intersects(boundingBox)) {
                            return BoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, j + 1, orientation);
                        }
                    }
                }

                return null;
            }
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            for(int i = 0; i < this.steps; ++i) {
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 0, 0, i, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 1, 0, i, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 2, 0, i, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 3, 0, i, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 4, 0, i, chunkBox);

                for(int j = 1; j <= 3; ++j) {
                    this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 0, j, i, chunkBox);
                    this.placeBlock(world, Blocks.CAVE_AIR.defaultBlockState(), 1, j, i, chunkBox);
                    this.placeBlock(world, Blocks.CAVE_AIR.defaultBlockState(), 2, j, i, chunkBox);
                    this.placeBlock(world, Blocks.CAVE_AIR.defaultBlockState(), 3, j, i, chunkBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 4, j, i, chunkBox);
                }

                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 0, 4, i, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, i, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, i, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 3, 4, i, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 4, 4, i, chunkBox);
            }

        }
    }

    public static class FiveCrossing extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 10;
        protected static final int HEIGHT = 9;
        protected static final int DEPTH = 11;
        private final boolean leftLow;
        private final boolean leftHigh;
        private final boolean rightLow;
        private final boolean rightHigh;

        public FiveCrossing(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
            this.leftLow = random.nextBoolean();
            this.leftHigh = random.nextBoolean();
            this.rightLow = random.nextBoolean();
            this.rightHigh = random.nextInt(3) > 0;
        }

        public FiveCrossing(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, nbt);
            this.leftLow = nbt.getBoolean("leftLow");
            this.leftHigh = nbt.getBoolean("leftHigh");
            this.rightLow = nbt.getBoolean("rightLow");
            this.rightHigh = nbt.getBoolean("rightHigh");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("leftLow", this.leftLow);
            nbt.putBoolean("leftHigh", this.leftHigh);
            nbt.putBoolean("rightLow", this.rightLow);
            nbt.putBoolean("rightHigh", this.rightHigh);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            int i = 3;
            int j = 5;
            Direction direction = this.getOrientation();
            if (direction == Direction.WEST || direction == Direction.NORTH) {
                i = 8 - i;
                j = 8 - j;
            }

            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)start, holder, random, 5, 1);
            if (this.leftLow) {
                this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)start, holder, random, i, 1);
            }

            if (this.leftHigh) {
                this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)start, holder, random, j, 7);
            }

            if (this.rightLow) {
                this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)start, holder, random, i, 1);
            }

            if (this.rightHigh) {
                this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)start, holder, random, j, 7);
            }

        }

        public static StrongholdPieces.FiveCrossing createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -4, -3, 0, 10, 9, 11, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new StrongholdPieces.FiveCrossing(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 9, 8, 10, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, chunkBox, this.entryDoor, 4, 3, 0);
            if (this.leftLow) {
                this.generateBox(world, chunkBox, 0, 3, 1, 0, 5, 3, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.rightLow) {
                this.generateBox(world, chunkBox, 9, 3, 1, 9, 5, 3, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.leftHigh) {
                this.generateBox(world, chunkBox, 0, 5, 7, 0, 7, 9, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.rightHigh) {
                this.generateBox(world, chunkBox, 9, 5, 7, 9, 7, 9, CAVE_AIR, CAVE_AIR, false);
            }

            this.generateBox(world, chunkBox, 5, 1, 10, 7, 3, 10, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(world, chunkBox, 1, 2, 1, 8, 2, 6, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 4, 1, 5, 4, 4, 9, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 8, 1, 5, 8, 4, 9, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 1, 4, 7, 3, 4, 9, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 1, 3, 5, 3, 3, 6, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 1, 3, 4, 3, 3, 4, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 1, 4, 6, 3, 4, 6, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 1, 7, 7, 1, 8, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 5, 1, 9, 7, 1, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 2, 7, 7, 2, 7, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 4, 5, 7, 4, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 8, 5, 7, 8, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 5, 5, 7, 7, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), false);
            this.placeBlock(world, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 6, 5, 6, chunkBox);
        }
    }

    public static class LeftTurn extends StrongholdPieces.Turn {
        public LeftTurn(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_LEFT_TURN, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public LeftTurn(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_LEFT_TURN, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            Direction direction = this.getOrientation();
            if (direction != Direction.NORTH && direction != Direction.EAST) {
                this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)start, holder, random, 1, 1);
            } else {
                this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)start, holder, random, 1, 1);
            }

        }

        public static StrongholdPieces.LeftTurn createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, 5, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new StrongholdPieces.LeftTurn(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 4, 4, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, chunkBox, this.entryDoor, 1, 1, 0);
            Direction direction = this.getOrientation();
            if (direction != Direction.NORTH && direction != Direction.EAST) {
                this.generateBox(world, chunkBox, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
            } else {
                this.generateBox(world, chunkBox, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
            }

        }
    }

    public static class Library extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 14;
        protected static final int HEIGHT = 6;
        protected static final int TALL_HEIGHT = 11;
        protected static final int DEPTH = 15;
        private final boolean isTall;

        public Library(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_LIBRARY, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
            this.isTall = boundingBox.getYSpan() > 6;
        }

        public Library(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_LIBRARY, nbt);
            this.isTall = nbt.getBoolean("Tall");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("Tall", this.isTall);
        }

        public static StrongholdPieces.Library createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -4, -1, 0, 14, 11, 15, orientation);
            if (!isOkBox(boundingBox) || holder.findCollisionPiece(boundingBox) != null) {
                boundingBox = BoundingBox.orientBox(x, y, z, -4, -1, 0, 14, 6, 15, orientation);
                if (!isOkBox(boundingBox) || holder.findCollisionPiece(boundingBox) != null) {
                    return null;
                }
            }

            return new StrongholdPieces.Library(chainLength, random, boundingBox, orientation);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            int i = 11;
            if (!this.isTall) {
                i = 6;
            }

            this.generateBox(world, chunkBox, 0, 0, 0, 13, i - 1, 14, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, chunkBox, this.entryDoor, 4, 1, 0);
            this.generateMaybeBox(world, chunkBox, random, 0.07F, 2, 1, 1, 11, 4, 13, Blocks.COBWEB.defaultBlockState(), Blocks.COBWEB.defaultBlockState(), false, false);
            int j = 1;
            int k = 12;

            for(int l = 1; l <= 13; ++l) {
                if ((l - 1) % 4 == 0) {
                    this.generateBox(world, chunkBox, 1, 1, l, 1, 4, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                    this.generateBox(world, chunkBox, 12, 1, l, 12, 4, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                    this.placeBlock(world, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 2, 3, l, chunkBox);
                    this.placeBlock(world, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 11, 3, l, chunkBox);
                    if (this.isTall) {
                        this.generateBox(world, chunkBox, 1, 6, l, 1, 9, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                        this.generateBox(world, chunkBox, 12, 6, l, 12, 9, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                    }
                } else {
                    this.generateBox(world, chunkBox, 1, 1, l, 1, 4, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                    this.generateBox(world, chunkBox, 12, 1, l, 12, 4, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                    if (this.isTall) {
                        this.generateBox(world, chunkBox, 1, 6, l, 1, 9, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                        this.generateBox(world, chunkBox, 12, 6, l, 12, 9, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                    }
                }
            }

            for(int m = 3; m < 12; m += 2) {
                this.generateBox(world, chunkBox, 3, 1, m, 4, 3, m, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                this.generateBox(world, chunkBox, 6, 1, m, 7, 3, m, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                this.generateBox(world, chunkBox, 9, 1, m, 10, 3, m, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
            }

            if (this.isTall) {
                this.generateBox(world, chunkBox, 1, 5, 1, 3, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.generateBox(world, chunkBox, 10, 5, 1, 12, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.generateBox(world, chunkBox, 4, 5, 1, 9, 5, 2, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.generateBox(world, chunkBox, 4, 5, 12, 9, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                this.placeBlock(world, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 11, chunkBox);
                this.placeBlock(world, Blocks.OAK_PLANKS.defaultBlockState(), 8, 5, 11, chunkBox);
                this.placeBlock(world, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 10, chunkBox);
                BlockState blockState = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true));
                BlockState blockState2 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
                this.generateBox(world, chunkBox, 3, 6, 3, 3, 6, 11, blockState2, blockState2, false);
                this.generateBox(world, chunkBox, 10, 6, 3, 10, 6, 9, blockState2, blockState2, false);
                this.generateBox(world, chunkBox, 4, 6, 2, 9, 6, 2, blockState, blockState, false);
                this.generateBox(world, chunkBox, 4, 6, 12, 7, 6, 12, blockState, blockState, false);
                this.placeBlock(world, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)), 3, 6, 2, chunkBox);
                this.placeBlock(world, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)), 3, 6, 12, chunkBox);
                this.placeBlock(world, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)), 10, 6, 2, chunkBox);

                for(int n = 0; n <= 2; ++n) {
                    this.placeBlock(world, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)), 8 + n, 6, 12 - n, chunkBox);
                    if (n != 2) {
                        this.placeBlock(world, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)), 8 + n, 6, 11 - n, chunkBox);
                    }
                }

                BlockState blockState3 = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
                this.placeBlock(world, blockState3, 10, 1, 13, chunkBox);
                this.placeBlock(world, blockState3, 10, 2, 13, chunkBox);
                this.placeBlock(world, blockState3, 10, 3, 13, chunkBox);
                this.placeBlock(world, blockState3, 10, 4, 13, chunkBox);
                this.placeBlock(world, blockState3, 10, 5, 13, chunkBox);
                this.placeBlock(world, blockState3, 10, 6, 13, chunkBox);
                this.placeBlock(world, blockState3, 10, 7, 13, chunkBox);
                int o = 7;
                int p = 7;
                BlockState blockState4 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true));
                this.placeBlock(world, blockState4, 6, 9, 7, chunkBox);
                BlockState blockState5 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true));
                this.placeBlock(world, blockState5, 7, 9, 7, chunkBox);
                this.placeBlock(world, blockState4, 6, 8, 7, chunkBox);
                this.placeBlock(world, blockState5, 7, 8, 7, chunkBox);
                BlockState blockState6 = blockState2.setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true));
                this.placeBlock(world, blockState6, 6, 7, 7, chunkBox);
                this.placeBlock(world, blockState6, 7, 7, 7, chunkBox);
                this.placeBlock(world, blockState4, 5, 7, 7, chunkBox);
                this.placeBlock(world, blockState5, 8, 7, 7, chunkBox);
                this.placeBlock(world, blockState4.setValue(FenceBlock.NORTH, Boolean.valueOf(true)), 6, 7, 6, chunkBox);
                this.placeBlock(world, blockState4.setValue(FenceBlock.SOUTH, Boolean.valueOf(true)), 6, 7, 8, chunkBox);
                this.placeBlock(world, blockState5.setValue(FenceBlock.NORTH, Boolean.valueOf(true)), 7, 7, 6, chunkBox);
                this.placeBlock(world, blockState5.setValue(FenceBlock.SOUTH, Boolean.valueOf(true)), 7, 7, 8, chunkBox);
                BlockState blockState7 = Blocks.TORCH.defaultBlockState();
                this.placeBlock(world, blockState7, 5, 8, 7, chunkBox);
                this.placeBlock(world, blockState7, 8, 8, 7, chunkBox);
                this.placeBlock(world, blockState7, 6, 8, 6, chunkBox);
                this.placeBlock(world, blockState7, 6, 8, 8, chunkBox);
                this.placeBlock(world, blockState7, 7, 8, 6, chunkBox);
                this.placeBlock(world, blockState7, 7, 8, 8, chunkBox);
            }

            this.createChest(world, chunkBox, random, 3, 3, 5, BuiltInLootTables.STRONGHOLD_LIBRARY);
            if (this.isTall) {
                this.placeBlock(world, CAVE_AIR, 12, 9, 1, chunkBox);
                this.createChest(world, chunkBox, random, 12, 8, 1, BuiltInLootTables.STRONGHOLD_LIBRARY);
            }

        }
    }

    static class PieceWeight {
        public final Class<? extends StrongholdPieces.StrongholdPiece> pieceClass;
        public final int weight;
        public int placeCount;
        public final int maxPlaceCount;

        public PieceWeight(Class<? extends StrongholdPieces.StrongholdPiece> pieceType, int weight, int limit) {
            this.pieceClass = pieceType;
            this.weight = weight;
            this.maxPlaceCount = limit;
        }

        public boolean doPlace(int chainLength) {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }

        public boolean isValid() {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }
    }

    public static class PortalRoom extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 11;
        protected static final int HEIGHT = 8;
        protected static final int DEPTH = 16;
        private boolean hasPlacedSpawner;

        public PortalRoom(int chainLength, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, chainLength, boundingBox);
            this.setOrientation(orientation);
        }

        public PortalRoom(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, nbt);
            this.hasPlacedSpawner = nbt.getBoolean("Mob");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("Mob", this.hasPlacedSpawner);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            if (start != null) {
                ((StrongholdPieces.StartPiece)start).portalRoomPiece = this;
            }

        }

        public static StrongholdPieces.PortalRoom createPiece(StructurePieceAccessor holder, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -4, -1, 0, 11, 8, 16, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new StrongholdPieces.PortalRoom(chainLength, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 10, 7, 15, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, chunkBox, StrongholdPieces.StrongholdPiece.SmallDoorType.GRATES, 4, 1, 0);
            int i = 6;
            this.generateBox(world, chunkBox, 1, i, 1, 1, i, 14, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 9, i, 1, 9, i, 14, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 2, i, 1, 8, i, 2, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 2, i, 14, 8, i, 14, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 1, 1, 1, 2, 1, 4, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 8, 1, 1, 9, 1, 4, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 1, 1, 1, 1, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 9, 1, 1, 9, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
            this.generateBox(world, chunkBox, 3, 1, 8, 7, 1, 12, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 4, 1, 9, 6, 1, 11, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
            BlockState blockState = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, Boolean.valueOf(true)).setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true));
            BlockState blockState2 = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, Boolean.valueOf(true)).setValue(IronBarsBlock.EAST, Boolean.valueOf(true));

            for(int j = 3; j < 14; j += 2) {
                this.generateBox(world, chunkBox, 0, 3, j, 0, 4, j, blockState, blockState, false);
                this.generateBox(world, chunkBox, 10, 3, j, 10, 4, j, blockState, blockState, false);
            }

            for(int k = 2; k < 9; k += 2) {
                this.generateBox(world, chunkBox, k, 3, 15, k, 4, 15, blockState2, blockState2, false);
            }

            BlockState blockState3 = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            this.generateBox(world, chunkBox, 4, 1, 5, 6, 1, 7, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 4, 2, 6, 6, 2, 7, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 4, 3, 7, 6, 3, 7, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);

            for(int l = 4; l <= 6; ++l) {
                this.placeBlock(world, blockState3, l, 1, 4, chunkBox);
                this.placeBlock(world, blockState3, l, 2, 5, chunkBox);
                this.placeBlock(world, blockState3, l, 3, 6, chunkBox);
            }

            BlockState blockState4 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.NORTH);
            BlockState blockState5 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.SOUTH);
            BlockState blockState6 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.EAST);
            BlockState blockState7 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.WEST);
            boolean bl = true;
            boolean[] bls = new boolean[12];

            for(int m = 0; m < bls.length; ++m) {
                bls[m] = random.nextFloat() > 0.9F;
                bl &= bls[m];
            }

            this.placeBlock(world, blockState4.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[0])), 4, 3, 8, chunkBox);
            this.placeBlock(world, blockState4.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[1])), 5, 3, 8, chunkBox);
            this.placeBlock(world, blockState4.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[2])), 6, 3, 8, chunkBox);
            this.placeBlock(world, blockState5.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[3])), 4, 3, 12, chunkBox);
            this.placeBlock(world, blockState5.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[4])), 5, 3, 12, chunkBox);
            this.placeBlock(world, blockState5.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[5])), 6, 3, 12, chunkBox);
            this.placeBlock(world, blockState6.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[6])), 3, 3, 9, chunkBox);
            this.placeBlock(world, blockState6.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[7])), 3, 3, 10, chunkBox);
            this.placeBlock(world, blockState6.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[8])), 3, 3, 11, chunkBox);
            this.placeBlock(world, blockState7.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[9])), 7, 3, 9, chunkBox);
            this.placeBlock(world, blockState7.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[10])), 7, 3, 10, chunkBox);
            this.placeBlock(world, blockState7.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(bls[11])), 7, 3, 11, chunkBox);
            if (bl) {
                BlockState blockState8 = Blocks.END_PORTAL.defaultBlockState();
                this.placeBlock(world, blockState8, 4, 3, 9, chunkBox);
                this.placeBlock(world, blockState8, 5, 3, 9, chunkBox);
                this.placeBlock(world, blockState8, 6, 3, 9, chunkBox);
                this.placeBlock(world, blockState8, 4, 3, 10, chunkBox);
                this.placeBlock(world, blockState8, 5, 3, 10, chunkBox);
                this.placeBlock(world, blockState8, 6, 3, 10, chunkBox);
                this.placeBlock(world, blockState8, 4, 3, 11, chunkBox);
                this.placeBlock(world, blockState8, 5, 3, 11, chunkBox);
                this.placeBlock(world, blockState8, 6, 3, 11, chunkBox);
            }

            if (!this.hasPlacedSpawner) {
                BlockPos blockPos = this.getWorldPos(5, 3, 6);
                if (chunkBox.isInside(blockPos)) {
                    this.hasPlacedSpawner = true;
                    world.setBlock(blockPos, Blocks.SPAWNER.defaultBlockState(), 2);
                    BlockEntity blockEntity = world.getBlockEntity(blockPos);
                    if (blockEntity instanceof SpawnerBlockEntity) {
                        ((SpawnerBlockEntity)blockEntity).getSpawner().setEntityId(EntityType.SILVERFISH);
                    }
                }
            }

        }
    }

    public static class PrisonHall extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 9;
        protected static final int HEIGHT = 5;
        protected static final int DEPTH = 11;

        public PrisonHall(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_PRISON_HALL, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public PrisonHall(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_PRISON_HALL, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)start, holder, random, 1, 1);
        }

        public static StrongholdPieces.PrisonHall createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, -1, 0, 9, 5, 11, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new StrongholdPieces.PrisonHall(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 8, 4, 10, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, chunkBox, this.entryDoor, 1, 1, 0);
            this.generateBox(world, chunkBox, 1, 1, 10, 3, 3, 10, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(world, chunkBox, 4, 1, 1, 4, 3, 1, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 4, 1, 3, 4, 3, 3, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 4, 1, 7, 4, 3, 7, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateBox(world, chunkBox, 4, 1, 9, 4, 3, 9, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);

            for(int i = 1; i <= 3; ++i) {
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, Boolean.valueOf(true)).setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true)), 4, i, 4, chunkBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, Boolean.valueOf(true)).setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true)).setValue(IronBarsBlock.EAST, Boolean.valueOf(true)), 4, i, 5, chunkBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, Boolean.valueOf(true)).setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true)), 4, i, 6, chunkBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, Boolean.valueOf(true)).setValue(IronBarsBlock.EAST, Boolean.valueOf(true)), 5, i, 5, chunkBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, Boolean.valueOf(true)).setValue(IronBarsBlock.EAST, Boolean.valueOf(true)), 6, i, 5, chunkBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, Boolean.valueOf(true)).setValue(IronBarsBlock.EAST, Boolean.valueOf(true)), 7, i, 5, chunkBox);
            }

            this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, Boolean.valueOf(true)).setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true)), 4, 3, 2, chunkBox);
            this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, Boolean.valueOf(true)).setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true)), 4, 3, 8, chunkBox);
            BlockState blockState = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST);
            BlockState blockState2 = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            this.placeBlock(world, blockState, 4, 1, 2, chunkBox);
            this.placeBlock(world, blockState2, 4, 2, 2, chunkBox);
            this.placeBlock(world, blockState, 4, 1, 8, chunkBox);
            this.placeBlock(world, blockState2, 4, 2, 8, chunkBox);
        }
    }

    public static class RightTurn extends StrongholdPieces.Turn {
        public RightTurn(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_RIGHT_TURN, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public RightTurn(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_RIGHT_TURN, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            Direction direction = this.getOrientation();
            if (direction != Direction.NORTH && direction != Direction.EAST) {
                this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)start, holder, random, 1, 1);
            } else {
                this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)start, holder, random, 1, 1);
            }

        }

        public static StrongholdPieces.RightTurn createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, 5, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new StrongholdPieces.RightTurn(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 4, 4, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, chunkBox, this.entryDoor, 1, 1, 0);
            Direction direction = this.getOrientation();
            if (direction != Direction.NORTH && direction != Direction.EAST) {
                this.generateBox(world, chunkBox, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
            } else {
                this.generateBox(world, chunkBox, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
            }

        }
    }

    public static class RoomCrossing extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 11;
        protected static final int HEIGHT = 7;
        protected static final int DEPTH = 11;
        protected final int type;

        public RoomCrossing(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
            this.type = random.nextInt(5);
        }

        public RoomCrossing(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, nbt);
            this.type = nbt.getInt("Type");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putInt("Type", this.type);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)start, holder, random, 4, 1);
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)start, holder, random, 1, 4);
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)start, holder, random, 1, 4);
        }

        public static StrongholdPieces.RoomCrossing createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -4, -1, 0, 11, 7, 11, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new StrongholdPieces.RoomCrossing(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 10, 6, 10, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, chunkBox, this.entryDoor, 4, 1, 0);
            this.generateBox(world, chunkBox, 4, 1, 10, 6, 3, 10, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(world, chunkBox, 0, 1, 4, 0, 3, 6, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(world, chunkBox, 10, 1, 4, 10, 3, 6, CAVE_AIR, CAVE_AIR, false);
            switch(this.type) {
            case 0:
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, chunkBox);
                this.placeBlock(world, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 4, 3, 5, chunkBox);
                this.placeBlock(world, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 6, 3, 5, chunkBox);
                this.placeBlock(world, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 5, 3, 4, chunkBox);
                this.placeBlock(world, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH), 5, 3, 6, chunkBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 4, chunkBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 5, chunkBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 6, chunkBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 4, chunkBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 5, chunkBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 6, chunkBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 4, chunkBox);
                this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 6, chunkBox);
                break;
            case 1:
                for(int i = 0; i < 5; ++i) {
                    this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 3, 1, 3 + i, chunkBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 7, 1, 3 + i, chunkBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 3 + i, 1, 3, chunkBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 3 + i, 1, 7, chunkBox);
                }

                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, chunkBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, chunkBox);
                this.placeBlock(world, Blocks.WATER.defaultBlockState(), 5, 4, 5, chunkBox);
                break;
            case 2:
                for(int j = 1; j <= 9; ++j) {
                    this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 1, 3, j, chunkBox);
                    this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 9, 3, j, chunkBox);
                }

                for(int k = 1; k <= 9; ++k) {
                    this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), k, 3, 1, chunkBox);
                    this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), k, 3, 9, chunkBox);
                }

                this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 4, chunkBox);
                this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 6, chunkBox);
                this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 4, chunkBox);
                this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 6, chunkBox);
                this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 4, 1, 5, chunkBox);
                this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 6, 1, 5, chunkBox);
                this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 4, 3, 5, chunkBox);
                this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 6, 3, 5, chunkBox);

                for(int l = 1; l <= 3; ++l) {
                    this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 4, l, 4, chunkBox);
                    this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 6, l, 4, chunkBox);
                    this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 4, l, 6, chunkBox);
                    this.placeBlock(world, Blocks.COBBLESTONE.defaultBlockState(), 6, l, 6, chunkBox);
                }

                this.placeBlock(world, Blocks.TORCH.defaultBlockState(), 5, 3, 5, chunkBox);

                for(int m = 2; m <= 8; ++m) {
                    this.placeBlock(world, Blocks.OAK_PLANKS.defaultBlockState(), 2, 3, m, chunkBox);
                    this.placeBlock(world, Blocks.OAK_PLANKS.defaultBlockState(), 3, 3, m, chunkBox);
                    if (m <= 3 || m >= 7) {
                        this.placeBlock(world, Blocks.OAK_PLANKS.defaultBlockState(), 4, 3, m, chunkBox);
                        this.placeBlock(world, Blocks.OAK_PLANKS.defaultBlockState(), 5, 3, m, chunkBox);
                        this.placeBlock(world, Blocks.OAK_PLANKS.defaultBlockState(), 6, 3, m, chunkBox);
                    }

                    this.placeBlock(world, Blocks.OAK_PLANKS.defaultBlockState(), 7, 3, m, chunkBox);
                    this.placeBlock(world, Blocks.OAK_PLANKS.defaultBlockState(), 8, 3, m, chunkBox);
                }

                BlockState blockState = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
                this.placeBlock(world, blockState, 9, 1, 3, chunkBox);
                this.placeBlock(world, blockState, 9, 2, 3, chunkBox);
                this.placeBlock(world, blockState, 9, 3, 3, chunkBox);
                this.createChest(world, chunkBox, random, 3, 4, 8, BuiltInLootTables.STRONGHOLD_CROSSING);
            }

        }
    }

    static class SmoothStoneSelector extends StructurePiece.BlockSelector {
        @Override
        public void next(Random random, int x, int y, int z, boolean placeBlock) {
            if (placeBlock) {
                float f = random.nextFloat();
                if (f < 0.2F) {
                    this.next = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                } else if (f < 0.5F) {
                    this.next = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                } else if (f < 0.55F) {
                    this.next = Blocks.INFESTED_STONE_BRICKS.defaultBlockState();
                } else {
                    this.next = Blocks.STONE_BRICKS.defaultBlockState();
                }
            } else {
                this.next = Blocks.CAVE_AIR.defaultBlockState();
            }

        }
    }

    public static class StairsDown extends StrongholdPieces.StrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 5;
        private final boolean isSource;

        public StairsDown(StructurePieceType structurePieceType, int chainLength, int x, int z, Direction orientation) {
            super(structurePieceType, chainLength, makeBoundingBox(x, 64, z, orientation, 5, 11, 5));
            this.isSource = true;
            this.setOrientation(orientation);
            this.entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;
        }

        public StairsDown(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_STAIRS_DOWN, chainLength, boundingBox);
            this.isSource = false;
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public StairsDown(StructurePieceType type, CompoundTag nbt) {
            super(type, nbt);
            this.isSource = nbt.getBoolean("Source");
        }

        public StairsDown(CompoundTag nbt) {
            this(StructurePieceType.STRONGHOLD_STAIRS_DOWN, nbt);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("Source", this.isSource);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            if (this.isSource) {
                StrongholdPieces.imposedPiece = StrongholdPieces.FiveCrossing.class;
            }

            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)start, holder, random, 1, 1);
        }

        public static StrongholdPieces.StairsDown createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, -7, 0, 5, 11, 5, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new StrongholdPieces.StairsDown(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 10, 4, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, chunkBox, this.entryDoor, 1, 7, 0);
            this.generateSmallDoor(world, random, chunkBox, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 4);
            this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 2, 6, 1, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 1, chunkBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 6, 1, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 2, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, 3, chunkBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 5, 3, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, 3, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 3, chunkBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 4, 3, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 2, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 3, 2, 1, chunkBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 3, 1, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 2, 2, 1, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 1, chunkBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 2, 1, chunkBox);
            this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 2, chunkBox);
            this.placeBlock(world, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 1, 3, chunkBox);
        }
    }

    public static class StartPiece extends StrongholdPieces.StairsDown {
        public StrongholdPieces.PieceWeight previousPiece;
        @Nullable
        public StrongholdPieces.PortalRoom portalRoomPiece;
        public final List<StructurePiece> pendingChildren = Lists.newArrayList();

        public StartPiece(Random random, int i, int j) {
            super(StructurePieceType.STRONGHOLD_START, 0, i, j, getRandomHorizontalDirection(random));
        }

        public StartPiece(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_START, nbt);
        }

        @Override
        public BlockPos getLocatorPosition() {
            return this.portalRoomPiece != null ? this.portalRoomPiece.getLocatorPosition() : super.getLocatorPosition();
        }
    }

    public static class Straight extends StrongholdPieces.StrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 5;
        private static final int DEPTH = 7;
        private final boolean leftChild;
        private final boolean rightChild;

        public Straight(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_STRAIGHT, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
            this.leftChild = random.nextInt(2) == 0;
            this.rightChild = random.nextInt(2) == 0;
        }

        public Straight(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_STRAIGHT, nbt);
            this.leftChild = nbt.getBoolean("Left");
            this.rightChild = nbt.getBoolean("Right");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("Left", this.leftChild);
            nbt.putBoolean("Right", this.rightChild);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)start, holder, random, 1, 1);
            if (this.leftChild) {
                this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)start, holder, random, 1, 2);
            }

            if (this.rightChild) {
                this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)start, holder, random, 1, 2);
            }

        }

        public static StrongholdPieces.Straight createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, -1, 0, 5, 5, 7, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new StrongholdPieces.Straight(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 4, 6, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, chunkBox, this.entryDoor, 1, 1, 0);
            this.generateSmallDoor(world, random, chunkBox, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
            BlockState blockState = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST);
            BlockState blockState2 = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST);
            this.maybeGenerateBlock(world, chunkBox, random, 0.1F, 1, 2, 1, blockState);
            this.maybeGenerateBlock(world, chunkBox, random, 0.1F, 3, 2, 1, blockState2);
            this.maybeGenerateBlock(world, chunkBox, random, 0.1F, 1, 2, 5, blockState);
            this.maybeGenerateBlock(world, chunkBox, random, 0.1F, 3, 2, 5, blockState2);
            if (this.leftChild) {
                this.generateBox(world, chunkBox, 0, 1, 2, 0, 3, 4, CAVE_AIR, CAVE_AIR, false);
            }

            if (this.rightChild) {
                this.generateBox(world, chunkBox, 4, 1, 2, 4, 3, 4, CAVE_AIR, CAVE_AIR, false);
            }

        }
    }

    public static class StraightStairsDown extends StrongholdPieces.StrongholdPiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 8;

        public StraightStairsDown(int chainLength, Random random, BoundingBox boundingBox, Direction orientation) {
            super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, chainLength, boundingBox);
            this.setOrientation(orientation);
            this.entryDoor = this.randomSmallDoor(random);
        }

        public StraightStairsDown(CompoundTag nbt) {
            super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, nbt);
        }

        @Override
        public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
            this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)start, holder, random, 1, 1);
        }

        public static StrongholdPieces.StraightStairsDown createPiece(StructurePieceAccessor holder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
            BoundingBox boundingBox = BoundingBox.orientBox(x, y, z, -1, -7, 0, 5, 11, 8, orientation);
            return isOkBox(boundingBox) && holder.findCollisionPiece(boundingBox) == null ? new StrongholdPieces.StraightStairsDown(chainLength, random, boundingBox, orientation) : null;
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
            this.generateBox(world, chunkBox, 0, 0, 0, 4, 10, 7, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
            this.generateSmallDoor(world, random, chunkBox, this.entryDoor, 1, 7, 0);
            this.generateSmallDoor(world, random, chunkBox, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 7);
            BlockState blockState = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);

            for(int i = 0; i < 6; ++i) {
                this.placeBlock(world, blockState, 1, 6 - i, 1 + i, chunkBox);
                this.placeBlock(world, blockState, 2, 6 - i, 1 + i, chunkBox);
                this.placeBlock(world, blockState, 3, 6 - i, 1 + i, chunkBox);
                if (i < 5) {
                    this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5 - i, 1 + i, chunkBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 2, 5 - i, 1 + i, chunkBox);
                    this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), 3, 5 - i, 1 + i, chunkBox);
                }
            }

        }
    }

    abstract static class StrongholdPiece extends StructurePiece {
        protected StrongholdPieces.StrongholdPiece.SmallDoorType entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;

        protected StrongholdPiece(StructurePieceType type, int length, BoundingBox boundingBox) {
            super(type, length, boundingBox);
        }

        public StrongholdPiece(StructurePieceType type, CompoundTag nbt) {
            super(type, nbt);
            this.entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.valueOf(nbt.getString("EntryDoor"));
        }

        @Override
        public NoiseEffect getNoiseEffect() {
            return NoiseEffect.BURY;
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            nbt.putString("EntryDoor", this.entryDoor.name());
        }

        protected void generateSmallDoor(WorldGenLevel world, Random random, BoundingBox boundingBox, StrongholdPieces.StrongholdPiece.SmallDoorType type, int x, int y, int z) {
            switch(type) {
            case OPENING:
                this.generateBox(world, boundingBox, x, y, z, x + 3 - 1, y + 3 - 1, z, CAVE_AIR, CAVE_AIR, false);
                break;
            case WOOD_DOOR:
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x, y, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x + 1, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x + 2, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x + 2, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x + 2, y, z, boundingBox);
                this.placeBlock(world, Blocks.OAK_DOOR.defaultBlockState(), x + 1, y, z, boundingBox);
                this.placeBlock(world, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), x + 1, y + 1, z, boundingBox);
                break;
            case GRATES:
                this.placeBlock(world, Blocks.CAVE_AIR.defaultBlockState(), x + 1, y, z, boundingBox);
                this.placeBlock(world, Blocks.CAVE_AIR.defaultBlockState(), x + 1, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, Boolean.valueOf(true)), x, y, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, Boolean.valueOf(true)), x, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, Boolean.valueOf(true)).setValue(IronBarsBlock.WEST, Boolean.valueOf(true)), x, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, Boolean.valueOf(true)).setValue(IronBarsBlock.WEST, Boolean.valueOf(true)), x + 1, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, Boolean.valueOf(true)).setValue(IronBarsBlock.WEST, Boolean.valueOf(true)), x + 2, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, Boolean.valueOf(true)), x + 2, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, Boolean.valueOf(true)), x + 2, y, z, boundingBox);
                break;
            case IRON_DOOR:
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x, y, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x + 1, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x + 2, y + 2, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x + 2, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BRICKS.defaultBlockState(), x + 2, y, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_DOOR.defaultBlockState(), x + 1, y, z, boundingBox);
                this.placeBlock(world, Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), x + 1, y + 1, z, boundingBox);
                this.placeBlock(world, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.NORTH), x + 2, y + 1, z + 1, boundingBox);
                this.placeBlock(world, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.SOUTH), x + 2, y + 1, z - 1, boundingBox);
            }

        }

        protected StrongholdPieces.StrongholdPiece.SmallDoorType randomSmallDoor(Random random) {
            int i = random.nextInt(5);
            switch(i) {
            case 0:
            case 1:
            default:
                return StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;
            case 2:
                return StrongholdPieces.StrongholdPiece.SmallDoorType.WOOD_DOOR;
            case 3:
                return StrongholdPieces.StrongholdPiece.SmallDoorType.GRATES;
            case 4:
                return StrongholdPieces.StrongholdPiece.SmallDoorType.IRON_DOOR;
            }
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildForward(StrongholdPieces.StartPiece start, StructurePieceAccessor holder, Random random, int leftRightOffset, int heightOffset) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, direction, this.getGenDepth());
                case SOUTH:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, direction, this.getGenDepth());
                case WEST:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, direction, this.getGenDepth());
                case EAST:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, direction, this.getGenDepth());
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildLeft(StrongholdPieces.StartPiece start, StructurePieceAccessor holder, Random random, int heightOffset, int leftRightOffset) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, Direction.WEST, this.getGenDepth());
                case SOUTH:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, Direction.WEST, this.getGenDepth());
                case WEST:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, Direction.NORTH, this.getGenDepth());
                case EAST:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() - 1, Direction.NORTH, this.getGenDepth());
                }
            }

            return null;
        }

        @Nullable
        protected StructurePiece generateSmallDoorChildRight(StrongholdPieces.StartPiece start, StructurePieceAccessor holder, Random random, int heightOffset, int leftRightOffset) {
            Direction direction = this.getOrientation();
            if (direction != null) {
                switch(direction) {
                case NORTH:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, Direction.EAST, this.getGenDepth());
                case SOUTH:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + heightOffset, this.boundingBox.minZ() + leftRightOffset, Direction.EAST, this.getGenDepth());
                case WEST:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, Direction.SOUTH, this.getGenDepth());
                case EAST:
                    return StrongholdPieces.generateAndAddPiece(start, holder, random, this.boundingBox.minX() + leftRightOffset, this.boundingBox.minY() + heightOffset, this.boundingBox.maxZ() + 1, Direction.SOUTH, this.getGenDepth());
                }
            }

            return null;
        }

        protected static boolean isOkBox(BoundingBox boundingBox) {
            return boundingBox != null && boundingBox.minY() > 10;
        }

        protected static enum SmallDoorType {
            OPENING,
            WOOD_DOOR,
            GRATES,
            IRON_DOOR;
        }
    }

    public abstract static class Turn extends StrongholdPieces.StrongholdPiece {
        protected static final int WIDTH = 5;
        protected static final int HEIGHT = 5;
        protected static final int DEPTH = 5;

        protected Turn(StructurePieceType type, int length, BoundingBox boundingBox) {
            super(type, length, boundingBox);
        }

        public Turn(StructurePieceType type, CompoundTag nbt) {
            super(type, nbt);
        }
    }
}
