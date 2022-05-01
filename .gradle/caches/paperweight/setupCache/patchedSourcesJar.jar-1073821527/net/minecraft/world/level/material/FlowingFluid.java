package net.minecraft.world.level.material;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class FlowingFluid extends Fluid {
    public static final BooleanProperty FALLING = BlockStateProperties.FALLING;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_FLOWING;
    private static final int CACHE_SIZE = 200;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
        Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2ByteLinkedOpenHashMap = new Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>(200) {
            protected void rehash(int i) {
            }
        };
        object2ByteLinkedOpenHashMap.defaultReturnValue((byte)127);
        return object2ByteLinkedOpenHashMap;
    });
    private final Map<FluidState, VoxelShape> shapes = Maps.newIdentityHashMap();

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
        builder.add(FALLING);
    }

    @Override
    public Vec3 getFlow(BlockGetter world, BlockPos pos, FluidState state) {
        double d = 0.0D;
        double e = 0.0D;
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            mutableBlockPos.setWithOffset(pos, direction);
            FluidState fluidState = world.getFluidState(mutableBlockPos);
            if (this.affectsFlow(fluidState)) {
                float f = fluidState.getOwnHeight();
                float g = 0.0F;
                if (f == 0.0F) {
                    if (!world.getBlockState(mutableBlockPos).getMaterial().blocksMotion()) {
                        BlockPos blockPos = mutableBlockPos.below();
                        FluidState fluidState2 = world.getFluidState(blockPos);
                        if (this.affectsFlow(fluidState2)) {
                            f = fluidState2.getOwnHeight();
                            if (f > 0.0F) {
                                g = state.getOwnHeight() - (f - 0.8888889F);
                            }
                        }
                    }
                } else if (f > 0.0F) {
                    g = state.getOwnHeight() - f;
                }

                if (g != 0.0F) {
                    d += (double)((float)direction.getStepX() * g);
                    e += (double)((float)direction.getStepZ() * g);
                }
            }
        }

        Vec3 vec3 = new Vec3(d, 0.0D, e);
        if (state.getValue(FALLING)) {
            for(Direction direction2 : Direction.Plane.HORIZONTAL) {
                mutableBlockPos.setWithOffset(pos, direction2);
                if (this.isSolidFace(world, mutableBlockPos, direction2) || this.isSolidFace(world, mutableBlockPos.above(), direction2)) {
                    vec3 = vec3.normalize().add(0.0D, -6.0D, 0.0D);
                    break;
                }
            }
        }

        return vec3.normalize();
    }

    private boolean affectsFlow(FluidState state) {
        return state.isEmpty() || state.getType().isSame(this);
    }

    protected boolean isSolidFace(BlockGetter world, BlockPos pos, Direction direction) {
        BlockState blockState = world.getBlockState(pos);
        FluidState fluidState = world.getFluidState(pos);
        if (fluidState.getType().isSame(this)) {
            return false;
        } else if (direction == Direction.UP) {
            return true;
        } else {
            return blockState.getMaterial() == Material.ICE ? false : blockState.isFaceSturdy(world, pos, direction);
        }
    }

    protected void spread(LevelAccessor world, BlockPos fluidPos, FluidState state) {
        if (!state.isEmpty()) {
            BlockState blockState = world.getBlockState(fluidPos);
            BlockPos blockPos = fluidPos.below();
            BlockState blockState2 = world.getBlockState(blockPos);
            FluidState fluidState = this.getNewLiquid(world, blockPos, blockState2);
            if (this.canSpreadTo(world, fluidPos, blockState, Direction.DOWN, blockPos, blockState2, world.getFluidState(blockPos), fluidState.getType())) {
                this.spreadTo(world, blockPos, blockState2, Direction.DOWN, fluidState);
                if (this.sourceNeighborCount(world, fluidPos) >= 3) {
                    this.spreadToSides(world, fluidPos, state, blockState);
                }
            } else if (state.isSource() || !this.isWaterHole(world, fluidState.getType(), fluidPos, blockState, blockPos, blockState2)) {
                this.spreadToSides(world, fluidPos, state, blockState);
            }

        }
    }

    private void spreadToSides(LevelAccessor world, BlockPos pos, FluidState fluidState, BlockState blockState) {
        int i = fluidState.getAmount() - this.getDropOff(world);
        if (fluidState.getValue(FALLING)) {
            i = 7;
        }

        if (i > 0) {
            Map<Direction, FluidState> map = this.getSpread(world, pos, blockState);

            for(Entry<Direction, FluidState> entry : map.entrySet()) {
                Direction direction = entry.getKey();
                FluidState fluidState2 = entry.getValue();
                BlockPos blockPos = pos.relative(direction);
                BlockState blockState2 = world.getBlockState(blockPos);
                if (this.canSpreadTo(world, pos, blockState, direction, blockPos, blockState2, world.getFluidState(blockPos), fluidState2.getType())) {
                    this.spreadTo(world, blockPos, blockState2, direction, fluidState2);
                }
            }

        }
    }

    protected FluidState getNewLiquid(LevelReader world, BlockPos pos, BlockState state) {
        int i = 0;
        int j = 0;

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockPos = pos.relative(direction);
            BlockState blockState = world.getBlockState(blockPos);
            FluidState fluidState = blockState.getFluidState();
            if (fluidState.getType().isSame(this) && this.canPassThroughWall(direction, world, pos, state, blockPos, blockState)) {
                if (fluidState.isSource()) {
                    ++j;
                }

                i = Math.max(i, fluidState.getAmount());
            }
        }

        if (this.canConvertToSource() && j >= 2) {
            BlockState blockState2 = world.getBlockState(pos.below());
            FluidState fluidState2 = blockState2.getFluidState();
            if (blockState2.getMaterial().isSolid() || this.isSourceBlockOfThisType(fluidState2)) {
                return this.getSource(false);
            }
        }

        BlockPos blockPos2 = pos.above();
        BlockState blockState3 = world.getBlockState(blockPos2);
        FluidState fluidState3 = blockState3.getFluidState();
        if (!fluidState3.isEmpty() && fluidState3.getType().isSame(this) && this.canPassThroughWall(Direction.UP, world, pos, state, blockPos2, blockState3)) {
            return this.getFlowing(8, true);
        } else {
            int k = i - this.getDropOff(world);
            return k <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(k, false);
        }
    }

    private boolean canPassThroughWall(Direction face, BlockGetter world, BlockPos pos, BlockState state, BlockPos fromPos, BlockState fromState) {
        Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2ByteLinkedOpenHashMap2;
        if (!state.getBlock().hasDynamicShape() && !fromState.getBlock().hasDynamicShape()) {
            object2ByteLinkedOpenHashMap2 = OCCLUSION_CACHE.get();
        } else {
            object2ByteLinkedOpenHashMap2 = null;
        }

        Block.BlockStatePairKey blockStatePairKey;
        if (object2ByteLinkedOpenHashMap2 != null) {
            blockStatePairKey = new Block.BlockStatePairKey(state, fromState, face);
            byte b = object2ByteLinkedOpenHashMap2.getAndMoveToFirst(blockStatePairKey);
            if (b != 127) {
                return b != 0;
            }
        } else {
            blockStatePairKey = null;
        }

        VoxelShape voxelShape = state.getCollisionShape(world, pos);
        VoxelShape voxelShape2 = fromState.getCollisionShape(world, fromPos);
        boolean bl = !Shapes.mergedFaceOccludes(voxelShape, voxelShape2, face);
        if (object2ByteLinkedOpenHashMap2 != null) {
            if (object2ByteLinkedOpenHashMap2.size() == 200) {
                object2ByteLinkedOpenHashMap2.removeLastByte();
            }

            object2ByteLinkedOpenHashMap2.putAndMoveToFirst(blockStatePairKey, (byte)(bl ? 1 : 0));
        }

        return bl;
    }

    public abstract Fluid getFlowing();

    public FluidState getFlowing(int level, boolean falling) {
        return this.getFlowing().defaultFluidState().setValue(LEVEL, Integer.valueOf(level)).setValue(FALLING, Boolean.valueOf(falling));
    }

    public abstract Fluid getSource();

    public FluidState getSource(boolean falling) {
        return this.getSource().defaultFluidState().setValue(FALLING, Boolean.valueOf(falling));
    }

    protected abstract boolean canConvertToSource();

    protected void spreadTo(LevelAccessor world, BlockPos pos, BlockState state, Direction direction, FluidState fluidState) {
        if (state.getBlock() instanceof LiquidBlockContainer) {
            ((LiquidBlockContainer)state.getBlock()).placeLiquid(world, pos, state, fluidState);
        } else {
            if (!state.isAir()) {
                this.beforeDestroyingBlock(world, pos, state);
            }

            world.setBlock(pos, fluidState.createLegacyBlock(), 3);
        }

    }

    protected abstract void beforeDestroyingBlock(LevelAccessor world, BlockPos pos, BlockState state);

    private static short getCacheKey(BlockPos blockPos, BlockPos blockPos2) {
        int i = blockPos2.getX() - blockPos.getX();
        int j = blockPos2.getZ() - blockPos.getZ();
        return (short)((i + 128 & 255) << 8 | j + 128 & 255);
    }

    protected int getSlopeDistance(LevelReader world, BlockPos blockPos, int i, Direction direction, BlockState blockState, BlockPos blockPos2, Short2ObjectMap<Pair<BlockState, FluidState>> short2ObjectMap, Short2BooleanMap short2BooleanMap) {
        int j = 1000;

        for(Direction direction2 : Direction.Plane.HORIZONTAL) {
            if (direction2 != direction) {
                BlockPos blockPos3 = blockPos.relative(direction2);
                short s = getCacheKey(blockPos2, blockPos3);
                Pair<BlockState, FluidState> pair = short2ObjectMap.computeIfAbsent(s, (sx) -> {
                    BlockState blockState = world.getBlockState(blockPos3);
                    return Pair.of(blockState, blockState.getFluidState());
                });
                BlockState blockState2 = pair.getFirst();
                FluidState fluidState = pair.getSecond();
                if (this.canPassThrough(world, this.getFlowing(), blockPos, blockState, direction2, blockPos3, blockState2, fluidState)) {
                    boolean bl = short2BooleanMap.computeIfAbsent(s, (sx) -> {
                        BlockPos blockPos2 = blockPos3.below();
                        BlockState blockState2 = world.getBlockState(blockPos2);
                        return this.isWaterHole(world, this.getFlowing(), blockPos3, blockState2, blockPos2, blockState2);
                    });
                    if (bl) {
                        return i;
                    }

                    if (i < this.getSlopeFindDistance(world)) {
                        int k = this.getSlopeDistance(world, blockPos3, i + 1, direction2.getOpposite(), blockState2, blockPos2, short2ObjectMap, short2BooleanMap);
                        if (k < j) {
                            j = k;
                        }
                    }
                }
            }
        }

        return j;
    }

    private boolean isWaterHole(BlockGetter world, Fluid fluid, BlockPos pos, BlockState state, BlockPos fromPos, BlockState fromState) {
        if (!this.canPassThroughWall(Direction.DOWN, world, pos, state, fromPos, fromState)) {
            return false;
        } else {
            return fromState.getFluidState().getType().isSame(this) ? true : this.canHoldFluid(world, fromPos, fromState, fluid);
        }
    }

    private boolean canPassThrough(BlockGetter world, Fluid fluid, BlockPos pos, BlockState state, Direction face, BlockPos fromPos, BlockState fromState, FluidState fluidState) {
        return !this.isSourceBlockOfThisType(fluidState) && this.canPassThroughWall(face, world, pos, state, fromPos, fromState) && this.canHoldFluid(world, fromPos, fromState, fluid);
    }

    private boolean isSourceBlockOfThisType(FluidState state) {
        return state.getType().isSame(this) && state.isSource();
    }

    protected abstract int getSlopeFindDistance(LevelReader world);

    private int sourceNeighborCount(LevelReader world, BlockPos pos) {
        int i = 0;

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockPos = pos.relative(direction);
            FluidState fluidState = world.getFluidState(blockPos);
            if (this.isSourceBlockOfThisType(fluidState)) {
                ++i;
            }
        }

        return i;
    }

    protected Map<Direction, FluidState> getSpread(LevelReader world, BlockPos pos, BlockState state) {
        int i = 1000;
        Map<Direction, FluidState> map = Maps.newEnumMap(Direction.class);
        Short2ObjectMap<Pair<BlockState, FluidState>> short2ObjectMap = new Short2ObjectOpenHashMap<>();
        Short2BooleanMap short2BooleanMap = new Short2BooleanOpenHashMap();

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockPos = pos.relative(direction);
            short s = getCacheKey(pos, blockPos);
            Pair<BlockState, FluidState> pair = short2ObjectMap.computeIfAbsent(s, (sx) -> {
                BlockState blockState = world.getBlockState(blockPos);
                return Pair.of(blockState, blockState.getFluidState());
            });
            BlockState blockState = pair.getFirst();
            FluidState fluidState = pair.getSecond();
            FluidState fluidState2 = this.getNewLiquid(world, blockPos, blockState);
            if (this.canPassThrough(world, fluidState2.getType(), pos, state, direction, blockPos, blockState, fluidState)) {
                BlockPos blockPos2 = blockPos.below();
                boolean bl = short2BooleanMap.computeIfAbsent(s, (sx) -> {
                    BlockState blockState2 = world.getBlockState(blockPos2);
                    return this.isWaterHole(world, this.getFlowing(), blockPos, blockState, blockPos2, blockState2);
                });
                int j;
                if (bl) {
                    j = 0;
                } else {
                    j = this.getSlopeDistance(world, blockPos, 1, direction.getOpposite(), blockState, pos, short2ObjectMap, short2BooleanMap);
                }

                if (j < i) {
                    map.clear();
                }

                if (j <= i) {
                    map.put(direction, fluidState2);
                    i = j;
                }
            }
        }

        return map;
    }

    private boolean canHoldFluid(BlockGetter world, BlockPos pos, BlockState state, Fluid fluid) {
        Block block = state.getBlock();
        if (block instanceof LiquidBlockContainer) {
            return ((LiquidBlockContainer)block).canPlaceLiquid(world, pos, state, fluid);
        } else if (!(block instanceof DoorBlock) && !state.is(BlockTags.SIGNS) && !state.is(Blocks.LADDER) && !state.is(Blocks.SUGAR_CANE) && !state.is(Blocks.BUBBLE_COLUMN)) {
            Material material = state.getMaterial();
            if (material != Material.PORTAL && material != Material.STRUCTURAL_AIR && material != Material.WATER_PLANT && material != Material.REPLACEABLE_WATER_PLANT) {
                return !material.blocksMotion();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected boolean canSpreadTo(BlockGetter world, BlockPos fluidPos, BlockState fluidBlockState, Direction flowDirection, BlockPos flowTo, BlockState flowToBlockState, FluidState fluidState, Fluid fluid) {
        return fluidState.canBeReplacedWith(world, flowTo, fluid, flowDirection) && this.canPassThroughWall(flowDirection, world, fluidPos, fluidBlockState, flowTo, flowToBlockState) && this.canHoldFluid(world, flowTo, flowToBlockState, fluid);
    }

    protected abstract int getDropOff(LevelReader world);

    protected int getSpreadDelay(Level world, BlockPos pos, FluidState oldState, FluidState newState) {
        return this.getTickDelay(world);
    }

    @Override
    public void tick(Level world, BlockPos pos, FluidState state) {
        if (!state.isSource()) {
            FluidState fluidState = this.getNewLiquid(world, pos, world.getBlockState(pos));
            int i = this.getSpreadDelay(world, pos, state, fluidState);
            if (fluidState.isEmpty()) {
                state = fluidState;
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            } else if (!fluidState.equals(state)) {
                state = fluidState;
                BlockState blockState = fluidState.createLegacyBlock();
                world.setBlock(pos, blockState, 2);
                world.scheduleTick(pos, fluidState.getType(), i);
                world.updateNeighborsAt(pos, blockState.getBlock());
            }
        }

        this.spread(world, pos, state);
    }

    protected static int getLegacyLevel(FluidState state) {
        return state.isSource() ? 0 : 8 - Math.min(state.getAmount(), 8) + (state.getValue(FALLING) ? 8 : 0);
    }

    private static boolean hasSameAbove(FluidState state, BlockGetter world, BlockPos pos) {
        return state.getType().isSame(world.getFluidState(pos.above()).getType());
    }

    @Override
    public float getHeight(FluidState state, BlockGetter world, BlockPos pos) {
        return hasSameAbove(state, world, pos) ? 1.0F : state.getOwnHeight();
    }

    @Override
    public float getOwnHeight(FluidState state) {
        return (float)state.getAmount() / 9.0F;
    }

    @Override
    public abstract int getAmount(FluidState state);

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter world, BlockPos pos) {
        return state.getAmount() == 9 && hasSameAbove(state, world, pos) ? Shapes.block() : this.shapes.computeIfAbsent(state, (fluidState) -> {
            return Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, (double)fluidState.getHeight(world, pos), 1.0D);
        });
    }
}
