package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.NoiseEffect;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;

public abstract class StructurePiece {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
    protected BoundingBox boundingBox;
    @Nullable
    private Direction orientation;
    private Mirror mirror;
    private Rotation rotation;
    protected int genDepth;
    private final StructurePieceType type;
    private static final Set<Block> SHAPE_CHECK_BLOCKS = ImmutableSet.<Block>builder().add(Blocks.NETHER_BRICK_FENCE).add(Blocks.TORCH).add(Blocks.WALL_TORCH).add(Blocks.OAK_FENCE).add(Blocks.SPRUCE_FENCE).add(Blocks.DARK_OAK_FENCE).add(Blocks.ACACIA_FENCE).add(Blocks.BIRCH_FENCE).add(Blocks.JUNGLE_FENCE).add(Blocks.LADDER).add(Blocks.IRON_BARS).build();

    protected StructurePiece(StructurePieceType type, int length, BoundingBox boundingBox) {
        this.type = type;
        this.genDepth = length;
        this.boundingBox = boundingBox;
    }

    public StructurePiece(StructurePieceType type, CompoundTag nbt) {
        this(type, nbt.getInt("GD"), BoundingBox.CODEC.parse(NbtOps.INSTANCE, nbt.get("BB")).resultOrPartial(LOGGER::error).orElseThrow(() -> {
            return new IllegalArgumentException("Invalid boundingbox");
        }));
        int i = nbt.getInt("O");
        this.setOrientation(i == -1 ? null : Direction.from2DDataValue(i));
    }

    protected static BoundingBox makeBoundingBox(int x, int y, int z, Direction orientation, int width, int height, int depth) {
        return orientation.getAxis() == Direction.Axis.Z ? new BoundingBox(x, y, z, x + width - 1, y + height - 1, z + depth - 1) : new BoundingBox(x, y, z, x + depth - 1, y + height - 1, z + width - 1);
    }

    protected static Direction getRandomHorizontalDirection(Random random) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(random);
    }

    public final CompoundTag createTag(StructurePieceSerializationContext context) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("id", Registry.STRUCTURE_PIECE.getKey(this.getType()).toString());
        BoundingBox.CODEC.encodeStart(NbtOps.INSTANCE, this.boundingBox).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
            compoundTag.put("BB", tag);
        });
        Direction direction = this.getOrientation();
        compoundTag.putInt("O", direction == null ? -1 : direction.get2DDataValue());
        compoundTag.putInt("GD", this.genDepth);
        this.addAdditionalSaveData(context, compoundTag);
        return compoundTag;
    }

    protected abstract void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt);

    public NoiseEffect getNoiseEffect() {
        return NoiseEffect.BEARD;
    }

    public void addChildren(StructurePiece start, StructurePieceAccessor holder, Random random) {
    }

    public abstract void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos);

    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public int getGenDepth() {
        return this.genDepth;
    }

    public boolean isCloseToChunk(ChunkPos pos, int offset) {
        int i = pos.getMinBlockX();
        int j = pos.getMinBlockZ();
        return this.boundingBox.intersects(i - offset, j - offset, i + 15 + offset, j + 15 + offset);
    }

    public BlockPos getLocatorPosition() {
        return new BlockPos(this.boundingBox.getCenter());
    }

    protected BlockPos.MutableBlockPos getWorldPos(int x, int y, int z) {
        return new BlockPos.MutableBlockPos(this.getWorldX(x, z), this.getWorldY(y), this.getWorldZ(x, z));
    }

    protected int getWorldX(int x, int z) {
        Direction direction = this.getOrientation();
        if (direction == null) {
            return x;
        } else {
            switch(direction) {
            case NORTH:
            case SOUTH:
                return this.boundingBox.minX() + x;
            case WEST:
                return this.boundingBox.maxX() - z;
            case EAST:
                return this.boundingBox.minX() + z;
            default:
                return x;
            }
        }
    }

    protected int getWorldY(int y) {
        return this.getOrientation() == null ? y : y + this.boundingBox.minY();
    }

    protected int getWorldZ(int x, int z) {
        Direction direction = this.getOrientation();
        if (direction == null) {
            return z;
        } else {
            switch(direction) {
            case NORTH:
                return this.boundingBox.maxZ() - z;
            case SOUTH:
                return this.boundingBox.minZ() + z;
            case WEST:
            case EAST:
                return this.boundingBox.minZ() + x;
            default:
                return z;
            }
        }
    }

    protected void placeBlock(WorldGenLevel world, BlockState block, int x, int y, int z, BoundingBox box) {
        BlockPos blockPos = this.getWorldPos(x, y, z);
        if (box.isInside(blockPos)) {
            if (this.canBeReplaced(world, x, y, z, box)) {
                if (this.mirror != Mirror.NONE) {
                    block = block.mirror(this.mirror);
                }

                if (this.rotation != Rotation.NONE) {
                    block = block.rotate(this.rotation);
                }

                world.setBlock(blockPos, block, 2);
                FluidState fluidState = world.getFluidState(blockPos);
                if (!fluidState.isEmpty()) {
                    world.scheduleTick(blockPos, fluidState.getType(), 0);
                }

                if (SHAPE_CHECK_BLOCKS.contains(block.getBlock())) {
                    world.getChunk(blockPos).markPosForPostprocessing(blockPos);
                }

            }
        }
    }

    protected boolean canBeReplaced(LevelReader world, int x, int y, int z, BoundingBox box) {
        return true;
    }

    protected BlockState getBlock(BlockGetter world, int x, int y, int z, BoundingBox box) {
        BlockPos blockPos = this.getWorldPos(x, y, z);
        return !box.isInside(blockPos) ? Blocks.AIR.defaultBlockState() : world.getBlockState(blockPos);
    }

    protected boolean isInterior(LevelReader world, int x, int z, int y, BoundingBox box) {
        BlockPos blockPos = this.getWorldPos(x, z + 1, y);
        if (!box.isInside(blockPos)) {
            return false;
        } else {
            return blockPos.getY() < world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, blockPos.getX(), blockPos.getZ());
        }
    }

    protected void generateAirBox(WorldGenLevel world, BoundingBox bounds, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for(int i = minY; i <= maxY; ++i) {
            for(int j = minX; j <= maxX; ++j) {
                for(int k = minZ; k <= maxZ; ++k) {
                    this.placeBlock(world, Blocks.AIR.defaultBlockState(), j, i, k, bounds);
                }
            }
        }

    }

    protected void generateBox(WorldGenLevel world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState outline, BlockState inside, boolean cantReplaceAir) {
        for(int i = minY; i <= maxY; ++i) {
            for(int j = minX; j <= maxX; ++j) {
                for(int k = minZ; k <= maxZ; ++k) {
                    if (!cantReplaceAir || !this.getBlock(world, j, i, k, box).isAir()) {
                        if (i != minY && i != maxY && j != minX && j != maxX && k != minZ && k != maxZ) {
                            this.placeBlock(world, inside, j, i, k, box);
                        } else {
                            this.placeBlock(world, outline, j, i, k, box);
                        }
                    }
                }
            }
        }

    }

    protected void generateBox(WorldGenLevel world, BoundingBox box, BoundingBox fillBox, BlockState outline, BlockState inside, boolean cantReplaceAir) {
        this.generateBox(world, box, fillBox.minX(), fillBox.minY(), fillBox.minZ(), fillBox.maxX(), fillBox.maxY(), fillBox.maxZ(), outline, inside, cantReplaceAir);
    }

    protected void generateBox(WorldGenLevel world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean cantReplaceAir, Random random, StructurePiece.BlockSelector randomizer) {
        for(int i = minY; i <= maxY; ++i) {
            for(int j = minX; j <= maxX; ++j) {
                for(int k = minZ; k <= maxZ; ++k) {
                    if (!cantReplaceAir || !this.getBlock(world, j, i, k, box).isAir()) {
                        randomizer.next(random, j, i, k, i == minY || i == maxY || j == minX || j == maxX || k == minZ || k == maxZ);
                        this.placeBlock(world, randomizer.getNext(), j, i, k, box);
                    }
                }
            }
        }

    }

    protected void generateBox(WorldGenLevel world, BoundingBox box, BoundingBox fillBox, boolean cantReplaceAir, Random random, StructurePiece.BlockSelector randomizer) {
        this.generateBox(world, box, fillBox.minX(), fillBox.minY(), fillBox.minZ(), fillBox.maxX(), fillBox.maxY(), fillBox.maxZ(), cantReplaceAir, random, randomizer);
    }

    protected void generateMaybeBox(WorldGenLevel world, BoundingBox box, Random random, float blockChance, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState outline, BlockState inside, boolean cantReplaceAir, boolean stayBelowSeaLevel) {
        for(int i = minY; i <= maxY; ++i) {
            for(int j = minX; j <= maxX; ++j) {
                for(int k = minZ; k <= maxZ; ++k) {
                    if (!(random.nextFloat() > blockChance) && (!cantReplaceAir || !this.getBlock(world, j, i, k, box).isAir()) && (!stayBelowSeaLevel || this.isInterior(world, j, i, k, box))) {
                        if (i != minY && i != maxY && j != minX && j != maxX && k != minZ && k != maxZ) {
                            this.placeBlock(world, inside, j, i, k, box);
                        } else {
                            this.placeBlock(world, outline, j, i, k, box);
                        }
                    }
                }
            }
        }

    }

    protected void maybeGenerateBlock(WorldGenLevel world, BoundingBox bounds, Random random, float threshold, int x, int y, int z, BlockState state) {
        if (random.nextFloat() < threshold) {
            this.placeBlock(world, state, x, y, z, bounds);
        }

    }

    protected void generateUpperHalfSphere(WorldGenLevel world, BoundingBox bounds, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState block, boolean cantReplaceAir) {
        float f = (float)(maxX - minX + 1);
        float g = (float)(maxY - minY + 1);
        float h = (float)(maxZ - minZ + 1);
        float i = (float)minX + f / 2.0F;
        float j = (float)minZ + h / 2.0F;

        for(int k = minY; k <= maxY; ++k) {
            float l = (float)(k - minY) / g;

            for(int m = minX; m <= maxX; ++m) {
                float n = ((float)m - i) / (f * 0.5F);

                for(int o = minZ; o <= maxZ; ++o) {
                    float p = ((float)o - j) / (h * 0.5F);
                    if (!cantReplaceAir || !this.getBlock(world, m, k, o, bounds).isAir()) {
                        float q = n * n + l * l + p * p;
                        if (q <= 1.05F) {
                            this.placeBlock(world, block, m, k, o, bounds);
                        }
                    }
                }
            }
        }

    }

    protected void fillColumnDown(WorldGenLevel world, BlockState state, int x, int y, int z, BoundingBox box) {
        BlockPos.MutableBlockPos mutableBlockPos = this.getWorldPos(x, y, z);
        if (box.isInside(mutableBlockPos)) {
            while(this.isReplaceableByStructures(world.getBlockState(mutableBlockPos)) && mutableBlockPos.getY() > world.getMinBuildHeight() + 1) {
                world.setBlock(mutableBlockPos, state, 2);
                mutableBlockPos.move(Direction.DOWN);
            }

        }
    }

    protected boolean isReplaceableByStructures(BlockState state) {
        return state.isAir() || state.getMaterial().isLiquid() || state.is(Blocks.GLOW_LICHEN) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS);
    }

    protected boolean createChest(WorldGenLevel world, BoundingBox boundingBox, Random random, int x, int y, int z, ResourceLocation lootTableId) {
        return this.createChest(world, boundingBox, random, this.getWorldPos(x, y, z), lootTableId, (BlockState)null);
    }

    public static BlockState reorient(BlockGetter world, BlockPos pos, BlockState state) {
        Direction direction = null;

        for(Direction direction2 : Direction.Plane.HORIZONTAL) {
            BlockPos blockPos = pos.relative(direction2);
            BlockState blockState = world.getBlockState(blockPos);
            if (blockState.is(Blocks.CHEST)) {
                return state;
            }

            if (blockState.isSolidRender(world, blockPos)) {
                if (direction != null) {
                    direction = null;
                    break;
                }

                direction = direction2;
            }
        }

        if (direction != null) {
            return state.setValue(HorizontalDirectionalBlock.FACING, direction.getOpposite());
        } else {
            Direction direction3 = state.getValue(HorizontalDirectionalBlock.FACING);
            BlockPos blockPos2 = pos.relative(direction3);
            if (world.getBlockState(blockPos2).isSolidRender(world, blockPos2)) {
                direction3 = direction3.getOpposite();
                blockPos2 = pos.relative(direction3);
            }

            if (world.getBlockState(blockPos2).isSolidRender(world, blockPos2)) {
                direction3 = direction3.getClockWise();
                blockPos2 = pos.relative(direction3);
            }

            if (world.getBlockState(blockPos2).isSolidRender(world, blockPos2)) {
                direction3 = direction3.getOpposite();
                pos.relative(direction3);
            }

            return state.setValue(HorizontalDirectionalBlock.FACING, direction3);
        }
    }

    protected boolean createChest(ServerLevelAccessor world, BoundingBox boundingBox, Random random, BlockPos pos, ResourceLocation lootTableId, @Nullable BlockState block) {
        if (boundingBox.isInside(pos) && !world.getBlockState(pos).is(Blocks.CHEST)) {
            if (block == null) {
                block = reorient(world, pos, Blocks.CHEST.defaultBlockState());
            }

            world.setBlock(pos, block, 2);
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ChestBlockEntity) {
                ((ChestBlockEntity)blockEntity).setLootTable(lootTableId, random.nextLong());
            }

            return true;
        } else {
            return false;
        }
    }

    protected boolean createDispenser(WorldGenLevel world, BoundingBox boundingBox, Random random, int x, int y, int z, Direction facing, ResourceLocation lootTableId) {
        BlockPos blockPos = this.getWorldPos(x, y, z);
        if (boundingBox.isInside(blockPos) && !world.getBlockState(blockPos).is(Blocks.DISPENSER)) {
            this.placeBlock(world, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, facing), x, y, z, boundingBox);
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (blockEntity instanceof DispenserBlockEntity) {
                ((DispenserBlockEntity)blockEntity).setLootTable(lootTableId, random.nextLong());
            }

            return true;
        } else {
            return false;
        }
    }

    public void move(int x, int y, int z) {
        this.boundingBox.move(x, y, z);
    }

    public static BoundingBox createBoundingBox(Stream<StructurePiece> pieces) {
        return BoundingBox.encapsulatingBoxes(pieces.map(StructurePiece::getBoundingBox)::iterator).orElseThrow(() -> {
            return new IllegalStateException("Unable to calculate boundingbox without pieces");
        });
    }

    @Nullable
    public static StructurePiece findCollisionPiece(List<StructurePiece> pieces, BoundingBox box) {
        for(StructurePiece structurePiece : pieces) {
            if (structurePiece.getBoundingBox().intersects(box)) {
                return structurePiece;
            }
        }

        return null;
    }

    @Nullable
    public Direction getOrientation() {
        return this.orientation;
    }

    public void setOrientation(@Nullable Direction orientation) {
        this.orientation = orientation;
        if (orientation == null) {
            this.rotation = Rotation.NONE;
            this.mirror = Mirror.NONE;
        } else {
            switch(orientation) {
            case SOUTH:
                this.mirror = Mirror.LEFT_RIGHT;
                this.rotation = Rotation.NONE;
                break;
            case WEST:
                this.mirror = Mirror.LEFT_RIGHT;
                this.rotation = Rotation.CLOCKWISE_90;
                break;
            case EAST:
                this.mirror = Mirror.NONE;
                this.rotation = Rotation.CLOCKWISE_90;
                break;
            default:
                this.mirror = Mirror.NONE;
                this.rotation = Rotation.NONE;
            }
        }

    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public Mirror getMirror() {
        return this.mirror;
    }

    public StructurePieceType getType() {
        return this.type;
    }

    protected abstract static class BlockSelector {
        protected BlockState next = Blocks.AIR.defaultBlockState();

        public abstract void next(Random random, int x, int y, int z, boolean placeBlock);

        public BlockState getNext() {
            return this.next;
        }
    }
}
