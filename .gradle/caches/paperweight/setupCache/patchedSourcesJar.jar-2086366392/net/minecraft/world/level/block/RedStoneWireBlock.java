package net.minecraft.world.level.block;

import com.destroystokyo.paper.PaperConfig;
import com.destroystokyo.paper.util.RedstoneWireTurbo;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.math.Vector3f;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.event.block.BlockRedstoneEvent; // CraftBukkit

public class RedStoneWireBlock extends Block {

    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = Maps.newEnumMap(ImmutableMap.of(Direction.NORTH, RedStoneWireBlock.NORTH, Direction.EAST, RedStoneWireBlock.EAST, Direction.SOUTH, RedStoneWireBlock.SOUTH, Direction.WEST, RedStoneWireBlock.WEST));
    protected static final int H = 1;
    protected static final int W = 3;
    protected static final int E = 13;
    protected static final int N = 3;
    protected static final int S = 13;
    private static final VoxelShape SHAPE_DOT = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D);
    private static final Map<Direction, VoxelShape> SHAPES_FLOOR = Maps.newEnumMap(ImmutableMap.of(Direction.NORTH, Block.box(3.0D, 0.0D, 0.0D, 13.0D, 1.0D, 13.0D), Direction.SOUTH, Block.box(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 16.0D), Direction.EAST, Block.box(3.0D, 0.0D, 3.0D, 16.0D, 1.0D, 13.0D), Direction.WEST, Block.box(0.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D)));
    private static final Map<Direction, VoxelShape> SHAPES_UP = Maps.newEnumMap(ImmutableMap.of(Direction.NORTH, Shapes.or((VoxelShape) RedStoneWireBlock.SHAPES_FLOOR.get(Direction.NORTH), Block.box(3.0D, 0.0D, 0.0D, 13.0D, 16.0D, 1.0D)), Direction.SOUTH, Shapes.or((VoxelShape) RedStoneWireBlock.SHAPES_FLOOR.get(Direction.SOUTH), Block.box(3.0D, 0.0D, 15.0D, 13.0D, 16.0D, 16.0D)), Direction.EAST, Shapes.or((VoxelShape) RedStoneWireBlock.SHAPES_FLOOR.get(Direction.EAST), Block.box(15.0D, 0.0D, 3.0D, 16.0D, 16.0D, 13.0D)), Direction.WEST, Shapes.or((VoxelShape) RedStoneWireBlock.SHAPES_FLOOR.get(Direction.WEST), Block.box(0.0D, 0.0D, 3.0D, 1.0D, 16.0D, 13.0D))));
    private static final Map<BlockState, VoxelShape> SHAPES_CACHE = Maps.newHashMap();
    private static final Vec3[] COLORS = (Vec3[]) Util.make(new Vec3[16], (avec3d) -> {
        for (int i = 0; i <= 15; ++i) {
            float f = (float) i / 15.0F;
            float f1 = f * 0.6F + (f > 0.0F ? 0.4F : 0.3F);
            float f2 = Mth.clamp(f * f * 0.7F - 0.5F, 0.0F, 1.0F);
            float f3 = Mth.clamp(f * f * 0.6F - 0.7F, 0.0F, 1.0F);

            avec3d[i] = new Vec3((double) f1, (double) f2, (double) f3);
        }

    });
    private static final float PARTICLE_DENSITY = 0.2F;
    private final BlockState crossState;
    public boolean shouldSignal = true;

    public RedStoneWireBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState) ((BlockState) ((BlockState) ((BlockState) ((BlockState) ((BlockState) this.stateDefinition.any()).setValue(RedStoneWireBlock.NORTH, RedstoneSide.NONE)).setValue(RedStoneWireBlock.EAST, RedstoneSide.NONE)).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.NONE)).setValue(RedStoneWireBlock.WEST, RedstoneSide.NONE)).setValue(RedStoneWireBlock.POWER, 0));
        this.crossState = (BlockState) ((BlockState) ((BlockState) ((BlockState) this.defaultBlockState().setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE)).setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE)).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE)).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE);
        UnmodifiableIterator unmodifiableiterator = this.getStateDefinition().getPossibleStates().iterator();

        while (unmodifiableiterator.hasNext()) {
            BlockState iblockdata = (BlockState) unmodifiableiterator.next();

            if ((Integer) iblockdata.getValue(RedStoneWireBlock.POWER) == 0) {
                RedStoneWireBlock.SHAPES_CACHE.put(iblockdata, this.calculateShape(iblockdata));
            }
        }

    }

    private VoxelShape calculateShape(BlockState state) {
        VoxelShape voxelshape = RedStoneWireBlock.SHAPE_DOT;
        Iterator iterator = Direction.Plane.HORIZONTAL.iterator();

        while (iterator.hasNext()) {
            Direction enumdirection = (Direction) iterator.next();
            RedstoneSide blockpropertyredstoneside = (RedstoneSide) state.getValue((Property) RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(enumdirection));

            if (blockpropertyredstoneside == RedstoneSide.SIDE) {
                voxelshape = Shapes.or(voxelshape, (VoxelShape) RedStoneWireBlock.SHAPES_FLOOR.get(enumdirection));
            } else if (blockpropertyredstoneside == RedstoneSide.UP) {
                voxelshape = Shapes.or(voxelshape, (VoxelShape) RedStoneWireBlock.SHAPES_UP.get(enumdirection));
            }
        }

        return voxelshape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return (VoxelShape) RedStoneWireBlock.SHAPES_CACHE.get(state.setValue(RedStoneWireBlock.POWER, 0));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.getConnectionState(ctx.getLevel(), this.crossState, ctx.getClickedPos());
    }

    private BlockState getConnectionState(BlockGetter world, BlockState state, BlockPos pos) {
        boolean flag = RedStoneWireBlock.isDot(state);

        state = this.getMissingConnections(world, (BlockState) this.defaultBlockState().setValue(RedStoneWireBlock.POWER, (Integer) state.getValue(RedStoneWireBlock.POWER)), pos);
        if (flag && RedStoneWireBlock.isDot(state)) {
            return state;
        } else {
            boolean flag1 = ((RedstoneSide) state.getValue(RedStoneWireBlock.NORTH)).isConnected();
            boolean flag2 = ((RedstoneSide) state.getValue(RedStoneWireBlock.SOUTH)).isConnected();
            boolean flag3 = ((RedstoneSide) state.getValue(RedStoneWireBlock.EAST)).isConnected();
            boolean flag4 = ((RedstoneSide) state.getValue(RedStoneWireBlock.WEST)).isConnected();
            boolean flag5 = !flag1 && !flag2;
            boolean flag6 = !flag3 && !flag4;

            if (!flag4 && flag5) {
                state = (BlockState) state.setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE);
            }

            if (!flag3 && flag5) {
                state = (BlockState) state.setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE);
            }

            if (!flag1 && flag6) {
                state = (BlockState) state.setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE);
            }

            if (!flag2 && flag6) {
                state = (BlockState) state.setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE);
            }

            return state;
        }
    }

    private BlockState getMissingConnections(BlockGetter world, BlockState state, BlockPos pos) {
        boolean flag = !world.getBlockState(pos.above()).isRedstoneConductor(world, pos);
        Iterator iterator = Direction.Plane.HORIZONTAL.iterator();

        while (iterator.hasNext()) {
            Direction enumdirection = (Direction) iterator.next();

            if (!((RedstoneSide) state.getValue((Property) RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(enumdirection))).isConnected()) {
                RedstoneSide blockpropertyredstoneside = this.getConnectingSide(world, pos, enumdirection, flag);

                state = (BlockState) state.setValue((Property) RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(enumdirection), blockpropertyredstoneside);
            }
        }

        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return state;
        } else if (direction == Direction.UP) {
            return this.getConnectionState(world, state, pos);
        } else {
            RedstoneSide blockpropertyredstoneside = this.getConnectingSide(world, pos, direction);

            return blockpropertyredstoneside.isConnected() == ((RedstoneSide) state.getValue((Property) RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(direction))).isConnected() && !RedStoneWireBlock.isCross(state) ? (BlockState) state.setValue((Property) RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(direction), blockpropertyredstoneside) : this.getConnectionState(world, (BlockState) ((BlockState) this.crossState.setValue(RedStoneWireBlock.POWER, (Integer) state.getValue(RedStoneWireBlock.POWER))).setValue((Property) RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(direction), blockpropertyredstoneside), pos);
        }
    }

    private static boolean isCross(BlockState state) {
        return ((RedstoneSide) state.getValue(RedStoneWireBlock.NORTH)).isConnected() && ((RedstoneSide) state.getValue(RedStoneWireBlock.SOUTH)).isConnected() && ((RedstoneSide) state.getValue(RedStoneWireBlock.EAST)).isConnected() && ((RedstoneSide) state.getValue(RedStoneWireBlock.WEST)).isConnected();
    }

    private static boolean isDot(BlockState state) {
        return !((RedstoneSide) state.getValue(RedStoneWireBlock.NORTH)).isConnected() && !((RedstoneSide) state.getValue(RedStoneWireBlock.SOUTH)).isConnected() && !((RedstoneSide) state.getValue(RedStoneWireBlock.EAST)).isConnected() && !((RedstoneSide) state.getValue(RedStoneWireBlock.WEST)).isConnected();
    }

    @Override
    public void updateIndirectNeighbourShapes(BlockState state, LevelAccessor world, BlockPos pos, int flags, int maxUpdateDepth) {
        BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
        Iterator iterator = Direction.Plane.HORIZONTAL.iterator();

        while (iterator.hasNext()) {
            Direction enumdirection = (Direction) iterator.next();
            RedstoneSide blockpropertyredstoneside = (RedstoneSide) state.getValue((Property) RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(enumdirection));

            if (blockpropertyredstoneside != RedstoneSide.NONE && !world.getBlockState(blockposition_mutableblockposition.setWithOffset(pos, enumdirection)).is((Block) this)) {
                blockposition_mutableblockposition.move(Direction.DOWN);
                BlockState iblockdata1 = world.getBlockState(blockposition_mutableblockposition);

                if (!iblockdata1.is(Blocks.OBSERVER)) {
                    BlockPos blockposition1 = blockposition_mutableblockposition.relative(enumdirection.getOpposite());
                    BlockState iblockdata2 = iblockdata1.updateShape(enumdirection.getOpposite(), world.getBlockState(blockposition1), world, blockposition_mutableblockposition, blockposition1);

                    updateOrDestroy(iblockdata1, iblockdata2, world, blockposition_mutableblockposition, flags, maxUpdateDepth);
                }

                blockposition_mutableblockposition.setWithOffset(pos, enumdirection).move(Direction.UP);
                BlockState iblockdata3 = world.getBlockState(blockposition_mutableblockposition);

                if (!iblockdata3.is(Blocks.OBSERVER)) {
                    BlockPos blockposition2 = blockposition_mutableblockposition.relative(enumdirection.getOpposite());
                    BlockState iblockdata4 = iblockdata3.updateShape(enumdirection.getOpposite(), world.getBlockState(blockposition2), world, blockposition_mutableblockposition, blockposition2);

                    updateOrDestroy(iblockdata3, iblockdata4, world, blockposition_mutableblockposition, flags, maxUpdateDepth);
                }
            }
        }

    }

    private RedstoneSide getConnectingSide(BlockGetter world, BlockPos pos, Direction direction) {
        return this.getConnectingSide(world, pos, direction, !world.getBlockState(pos.above()).isRedstoneConductor(world, pos));
    }

    private RedstoneSide getConnectingSide(BlockGetter world, BlockPos pos, Direction direction, boolean flag) {
        BlockPos blockposition1 = pos.relative(direction);
        BlockState iblockdata = world.getBlockState(blockposition1);

        if (flag) {
            boolean flag1 = this.canSurviveOn(world, blockposition1, iblockdata);

            if (flag1 && RedStoneWireBlock.shouldConnectTo(world.getBlockState(blockposition1.above()))) {
                if (iblockdata.isFaceSturdy(world, blockposition1, direction.getOpposite())) {
                    return RedstoneSide.UP;
                }

                return RedstoneSide.SIDE;
            }
        }

        return !RedStoneWireBlock.shouldConnectTo(iblockdata, direction) && (iblockdata.isRedstoneConductor(world, blockposition1) || !RedStoneWireBlock.shouldConnectTo(world.getBlockState(blockposition1.below()))) ? RedstoneSide.NONE : RedstoneSide.SIDE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos blockposition1 = pos.below();
        BlockState iblockdata1 = world.getBlockState(blockposition1);

        return this.canSurviveOn(world, blockposition1, iblockdata1);
    }

    private boolean canSurviveOn(BlockGetter world, BlockPos pos, BlockState floor) {
        return floor.isFaceSturdy(world, pos, Direction.UP) || floor.is(Blocks.HOPPER);
    }

    // Paper start - Optimize redstone
    // The bulk of the new functionality is found in RedstoneWireTurbo.java
    RedstoneWireTurbo turbo = new RedstoneWireTurbo(this);

    /*
     * Modified version of pre-existing updateSurroundingRedstone, which is called from
     * this.neighborChanged and a few other methods in this class.
     * Note: Added 'source' argument so as to help determine direction of information flow
     */
    private void updateSurroundingRedstone(Level worldIn, BlockPos pos, BlockState state, BlockPos source) {
        if (worldIn.paperConfig.useEigencraftRedstone) {
            turbo.updateSurroundingRedstone(worldIn, pos, state, source);
            return;
        }
        updatePowerStrength(worldIn, pos, state);
    }

    /*
     * Slightly modified method to compute redstone wire power levels from neighboring blocks.
     * Modifications cut the number of power level changes by about 45% from vanilla, and this
     * optimization synergizes well with the breadth-first search implemented in
     * RedstoneWireTurbo.
     * Note:  RedstoneWireTurbo contains a faster version of this code.
     * Note:  Made this public so that RedstoneWireTurbo can access it.
     */
    public BlockState calculateCurrentChanges(Level worldIn, BlockPos pos1, BlockPos pos2, BlockState state) {
        BlockState iblockstate = state;
        int i = state.getValue(POWER);
        int j = 0;
        j = this.getPower(j, worldIn.getBlockState(pos2));
        this.shouldSignal = false;
        int k = worldIn.getBestNeighborSignal(pos1);
        this.shouldSignal = true;

        if (!worldIn.paperConfig.useEigencraftRedstone) {
            // This code is totally redundant to if statements just below the loop.
            if (k > 0 && k > j - 1) {
                j = k;
            }
        }

        int l = 0;

        // The variable 'k' holds the maximum redstone power value of any adjacent blocks.
        // If 'k' has the highest level of all neighbors, then the power level of this
        // redstone wire will be set to 'k'.  If 'k' is already 15, then nothing inside the
        // following loop can affect the power level of the wire.  Therefore, the loop is
        // skipped if k is already 15.
        if (!worldIn.paperConfig.useEigencraftRedstone || k < 15) {
            for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
                BlockPos blockpos = pos1.relative(enumfacing);
                boolean flag = blockpos.getX() != pos2.getX() || blockpos.getZ() != pos2.getZ();

                if (flag) {
                    l = this.getPower(l, worldIn.getBlockState(blockpos));
                }

                if (worldIn.getBlockState(blockpos).isRedstoneConductor(worldIn, blockpos) && !worldIn.getBlockState(pos1.above()).isRedstoneConductor(worldIn, pos1)) {
                    if (flag && pos1.getY() >= pos2.getY()) {
                        l = this.getPower(l, worldIn.getBlockState(blockpos.above()));
                    }
                } else if (!worldIn.getBlockState(blockpos).isRedstoneConductor(worldIn, blockpos) && flag && pos1.getY() <= pos2.getY()) {
                    l = this.getPower(l, worldIn.getBlockState(blockpos.below()));
                }
            }
        }

        if (!worldIn.paperConfig.useEigencraftRedstone) {
            // The old code would decrement the wire value only by 1 at a time.
            if (l > j) {
                j = l - 1;
            } else if (j > 0) {
                --j;
            } else {
                j = 0;
            }

            if (k > j - 1) {
                j = k;
            }
        } else {
            // The new code sets this RedstoneWire block's power level to the highest neighbor
            // minus 1.  This usually results in wire power levels dropping by 2 at a time.
            // This optimization alone has no impact on update order, only the number of updates.
            j = l - 1;

            // If 'l' turns out to be zero, then j will be set to -1, but then since 'k' will
            // always be in the range of 0 to 15, the following if will correct that.
            if (k > j) j = k;
        }

        if (i != j) {
            state = state.setValue(POWER, j);

            if (worldIn.getBlockState(pos1) == iblockstate) {
                worldIn.setBlock(pos1, state, 2);
            }

            // 1.16(.1?) dropped the need for blocks needing updates.
            // Whether this is necessary after all is to be seen.
//            if (!worldIn.paperConfig.useEigencraftRedstone) {
//                // The new search algorithm keeps track of blocks needing updates in its own data structures,
//                // so only add anything to blocksNeedingUpdate if we're using the vanilla update algorithm.
//                this.getBlocksNeedingUpdate().add(pos1);
//
//                for (EnumDirection enumfacing1 : EnumDirection.values()) {
//                    this.getBlocksNeedingUpdate().add(pos1.shift(enumfacing1));
//                }
//            }
        }

        return state;
    }
    // Paper end

    private void updatePowerStrength(Level world, BlockPos pos, BlockState state) {
        int i = this.calculateTargetStrength(world, pos);

        // CraftBukkit start
        int oldPower = (Integer) state.getValue(RedStoneWireBlock.POWER);
        if (oldPower != i) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()), oldPower, i);
            world.getCraftServer().getPluginManager().callEvent(event);

            i = event.getNewCurrent();
        }
        if (oldPower != i) {
            // CraftBukkit end
            if (world.getBlockState(pos) == state) {
                world.setBlock(pos, (BlockState) state.setValue(RedStoneWireBlock.POWER, i), 2);
            }

            Set<BlockPos> set = Sets.newHashSet();

            set.add(pos);
            Direction[] aenumdirection = Direction.values();
            int j = aenumdirection.length;

            for (int k = 0; k < j; ++k) {
                Direction enumdirection = aenumdirection[k];

                set.add(pos.relative(enumdirection));
            }

            Iterator iterator = set.iterator();

            while (iterator.hasNext()) {
                BlockPos blockposition1 = (BlockPos) iterator.next();

                world.updateNeighborsAt(blockposition1, this);
            }
        }

    }

    private int calculateTargetStrength(Level world, BlockPos pos) {
        this.shouldSignal = false;
        int i = world.getBestNeighborSignal(pos);

        this.shouldSignal = true;
        int j = 0;

        if (i < 15) {
            Iterator iterator = Direction.Plane.HORIZONTAL.iterator();

            while (iterator.hasNext()) {
                Direction enumdirection = (Direction) iterator.next();
                BlockPos blockposition1 = pos.relative(enumdirection);
                BlockState iblockdata = world.getBlockState(blockposition1);

                j = Math.max(j, this.getWireSignal(iblockdata));
                BlockPos blockposition2 = pos.above();

                if (iblockdata.isRedstoneConductor(world, blockposition1) && !world.getBlockState(blockposition2).isRedstoneConductor(world, blockposition2)) {
                    j = Math.max(j, this.getWireSignal(world.getBlockState(blockposition1.above())));
                } else if (!iblockdata.isRedstoneConductor(world, blockposition1)) {
                    j = Math.max(j, this.getWireSignal(world.getBlockState(blockposition1.below())));
                }
            }
        }

        return Math.max(i, j - 1);
    }

    private int getPower(int min, BlockState iblockdata) { return Math.max(min, getWireSignal(iblockdata)); } // Paper - Optimize redstone
    private int getWireSignal(BlockState state) {
        return state.is((Block) this) ? (Integer) state.getValue(RedStoneWireBlock.POWER) : 0;
    }

    private void checkCornerChangeAt(Level world, BlockPos pos) {
        if (world.getBlockState(pos).is((Block) this)) {
            world.updateNeighborsAt(pos, this);
            Direction[] aenumdirection = Direction.values();
            int i = aenumdirection.length;

            for (int j = 0; j < i; ++j) {
                Direction enumdirection = aenumdirection[j];

                world.updateNeighborsAt(pos.relative(enumdirection), this);
            }

        }
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock()) && !world.isClientSide) {
            this.updateSurroundingRedstone(world, pos, state, null); // Paper - Optimize redstone
            Iterator iterator = Direction.Plane.VERTICAL.iterator();

            while (iterator.hasNext()) {
                Direction enumdirection = (Direction) iterator.next();

                world.updateNeighborsAt(pos.relative(enumdirection), this);
            }

            this.updateNeighborsOfNeighboringWires(world, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            super.onRemove(state, world, pos, newState, moved);
            if (!world.isClientSide) {
                Direction[] aenumdirection = Direction.values();
                int i = aenumdirection.length;

                for (int j = 0; j < i; ++j) {
                    Direction enumdirection = aenumdirection[j];

                    world.updateNeighborsAt(pos.relative(enumdirection), this);
                }

                this.updateSurroundingRedstone(world, pos, state, null); // Paper - Optimize redstone
                this.updateNeighborsOfNeighboringWires(world, pos);
            }
        }
    }

    private void updateNeighborsOfNeighboringWires(Level world, BlockPos pos) {
        Iterator iterator = Direction.Plane.HORIZONTAL.iterator();

        Direction enumdirection;

        while (iterator.hasNext()) {
            enumdirection = (Direction) iterator.next();
            this.checkCornerChangeAt(world, pos.relative(enumdirection));
        }

        iterator = Direction.Plane.HORIZONTAL.iterator();

        while (iterator.hasNext()) {
            enumdirection = (Direction) iterator.next();
            BlockPos blockposition1 = pos.relative(enumdirection);

            if (world.getBlockState(blockposition1).isRedstoneConductor(world, blockposition1)) {
                this.checkCornerChangeAt(world, blockposition1.above());
            } else {
                this.checkCornerChangeAt(world, blockposition1.below());
            }
        }

    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        if (!world.isClientSide) {
            if (state.canSurvive(world, pos)) {
                this.updateSurroundingRedstone(world, pos, state, fromPos); // Paper - Optimize redstone
            } else {
                dropResources(state, world, pos);
                world.removeBlock(pos, false);
            }

        }
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return !this.shouldSignal ? 0 : state.getSignal(world, pos, direction);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        if (this.shouldSignal && direction != Direction.DOWN) {
            int i = (Integer) state.getValue(RedStoneWireBlock.POWER);

            return i == 0 ? 0 : (direction != Direction.UP && !((RedstoneSide) this.getConnectionState(world, state, pos).getValue((Property) RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(direction.getOpposite()))).isConnected() ? 0 : i);
        } else {
            return 0;
        }
    }

    protected static boolean shouldConnectTo(BlockState state) {
        return RedStoneWireBlock.shouldConnectTo(state, (Direction) null);
    }

    protected static boolean shouldConnectTo(BlockState state, @Nullable Direction dir) {
        if (state.is(Blocks.REDSTONE_WIRE)) {
            return true;
        } else if (state.is(Blocks.REPEATER)) {
            Direction enumdirection1 = (Direction) state.getValue(RepeaterBlock.FACING);

            return enumdirection1 == dir || enumdirection1.getOpposite() == dir;
        } else {
            return state.is(Blocks.OBSERVER) ? dir == state.getValue(ObserverBlock.FACING) : state.isSignalSource() && dir != null;
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return this.shouldSignal;
    }

    public static int getColorForPower(int powerLevel) {
        Vec3 vec3d = RedStoneWireBlock.COLORS[powerLevel];

        return Mth.color((float) vec3d.x(), (float) vec3d.y(), (float) vec3d.z());
    }

    private void spawnParticlesAlongLine(Level world, Random random, BlockPos pos, Vec3 color, Direction enumdirection, Direction enumdirection1, float f, float f1) {
        float f2 = f1 - f;

        if (random.nextFloat() < 0.2F * f2) {
            float f3 = 0.4375F;
            float f4 = f + f2 * random.nextFloat();
            double d0 = 0.5D + (double) (0.4375F * (float) enumdirection.getStepX()) + (double) (f4 * (float) enumdirection1.getStepX());
            double d1 = 0.5D + (double) (0.4375F * (float) enumdirection.getStepY()) + (double) (f4 * (float) enumdirection1.getStepY());
            double d2 = 0.5D + (double) (0.4375F * (float) enumdirection.getStepZ()) + (double) (f4 * (float) enumdirection1.getStepZ());

            world.addParticle(new DustParticleOptions(new Vector3f(color), 1.0F), (double) pos.getX() + d0, (double) pos.getY() + d1, (double) pos.getZ() + d2, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, Random random) {
        int i = (Integer) state.getValue(RedStoneWireBlock.POWER);

        if (i != 0) {
            Iterator iterator = Direction.Plane.HORIZONTAL.iterator();

            while (iterator.hasNext()) {
                Direction enumdirection = (Direction) iterator.next();
                RedstoneSide blockpropertyredstoneside = (RedstoneSide) state.getValue((Property) RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(enumdirection));

                switch (blockpropertyredstoneside) {
                    case UP:
                        this.spawnParticlesAlongLine(world, random, pos, RedStoneWireBlock.COLORS[i], enumdirection, Direction.UP, -0.5F, 0.5F);
                    case SIDE:
                        this.spawnParticlesAlongLine(world, random, pos, RedStoneWireBlock.COLORS[i], Direction.DOWN, enumdirection, 0.0F, 0.5F);
                        break;
                    case NONE:
                    default:
                        this.spawnParticlesAlongLine(world, random, pos, RedStoneWireBlock.COLORS[i], Direction.DOWN, enumdirection, 0.0F, 0.3F);
                }
            }

        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_180:
                return (BlockState) ((BlockState) ((BlockState) ((BlockState) state.setValue(RedStoneWireBlock.NORTH, (RedstoneSide) state.getValue(RedStoneWireBlock.SOUTH))).setValue(RedStoneWireBlock.EAST, (RedstoneSide) state.getValue(RedStoneWireBlock.WEST))).setValue(RedStoneWireBlock.SOUTH, (RedstoneSide) state.getValue(RedStoneWireBlock.NORTH))).setValue(RedStoneWireBlock.WEST, (RedstoneSide) state.getValue(RedStoneWireBlock.EAST));
            case COUNTERCLOCKWISE_90:
                return (BlockState) ((BlockState) ((BlockState) ((BlockState) state.setValue(RedStoneWireBlock.NORTH, (RedstoneSide) state.getValue(RedStoneWireBlock.EAST))).setValue(RedStoneWireBlock.EAST, (RedstoneSide) state.getValue(RedStoneWireBlock.SOUTH))).setValue(RedStoneWireBlock.SOUTH, (RedstoneSide) state.getValue(RedStoneWireBlock.WEST))).setValue(RedStoneWireBlock.WEST, (RedstoneSide) state.getValue(RedStoneWireBlock.NORTH));
            case CLOCKWISE_90:
                return (BlockState) ((BlockState) ((BlockState) ((BlockState) state.setValue(RedStoneWireBlock.NORTH, (RedstoneSide) state.getValue(RedStoneWireBlock.WEST))).setValue(RedStoneWireBlock.EAST, (RedstoneSide) state.getValue(RedStoneWireBlock.NORTH))).setValue(RedStoneWireBlock.SOUTH, (RedstoneSide) state.getValue(RedStoneWireBlock.EAST))).setValue(RedStoneWireBlock.WEST, (RedstoneSide) state.getValue(RedStoneWireBlock.SOUTH));
            default:
                return state;
        }
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        switch (mirror) {
            case LEFT_RIGHT:
                return (BlockState) ((BlockState) state.setValue(RedStoneWireBlock.NORTH, (RedstoneSide) state.getValue(RedStoneWireBlock.SOUTH))).setValue(RedStoneWireBlock.SOUTH, (RedstoneSide) state.getValue(RedStoneWireBlock.NORTH));
            case FRONT_BACK:
                return (BlockState) ((BlockState) state.setValue(RedStoneWireBlock.EAST, (RedstoneSide) state.getValue(RedStoneWireBlock.WEST))).setValue(RedStoneWireBlock.WEST, (RedstoneSide) state.getValue(RedStoneWireBlock.EAST));
            default:
                return super.mirror(state, mirror);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RedStoneWireBlock.NORTH, RedStoneWireBlock.EAST, RedStoneWireBlock.SOUTH, RedStoneWireBlock.WEST, RedStoneWireBlock.POWER);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            if (RedStoneWireBlock.isCross(state) || RedStoneWireBlock.isDot(state)) {
                BlockState iblockdata1 = RedStoneWireBlock.isCross(state) ? this.defaultBlockState() : this.crossState;

                iblockdata1 = (BlockState) iblockdata1.setValue(RedStoneWireBlock.POWER, (Integer) state.getValue(RedStoneWireBlock.POWER));
                iblockdata1 = this.getConnectionState(world, iblockdata1, pos);
                if (iblockdata1 != state) {
                    world.setBlock(pos, iblockdata1, 3);
                    this.updatesOnShapeChange(world, pos, state, iblockdata1);
                    return InteractionResult.SUCCESS;
                }
            }

            return InteractionResult.PASS;
        }
    }

    private void updatesOnShapeChange(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        Iterator iterator = Direction.Plane.HORIZONTAL.iterator();

        while (iterator.hasNext()) {
            Direction enumdirection = (Direction) iterator.next();
            BlockPos blockposition1 = pos.relative(enumdirection);

            if (((RedstoneSide) oldState.getValue((Property) RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(enumdirection))).isConnected() != ((RedstoneSide) newState.getValue((Property) RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(enumdirection))).isConnected() && world.getBlockState(blockposition1).isRedstoneConductor(world, blockposition1)) {
                world.updateNeighborsAtExceptFromFacing(blockposition1, newState.getBlock(), enumdirection.getOpposite());
            }
        }

    }
}
