package net.minecraft.world.level.block;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;

public class DetectorRailBlock extends BaseRailBlock {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final int PRESSED_CHECK_PERIOD = 20;

    public DetectorRailBlock(BlockBehaviour.Properties settings) {
        super(true, settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, Boolean.valueOf(false)).setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!world.isClientSide) {
            if (!state.getValue(POWERED)) {
                this.checkPressed(world, pos, state);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (state.getValue(POWERED)) {
            this.checkPressed(world, pos, state);
        }
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        if (!state.getValue(POWERED)) {
            return 0;
        } else {
            return direction == Direction.UP ? 15 : 0;
        }
    }

    private void checkPressed(Level world, BlockPos pos, BlockState state) {
        if (this.canSurvive(state, world, pos)) {
            boolean bl = state.getValue(POWERED);
            boolean bl2 = false;
            List<AbstractMinecart> list = this.getInteractingMinecartOfType(world, pos, AbstractMinecart.class, (entity) -> {
                return true;
            });
            if (!list.isEmpty()) {
                bl2 = true;
            }

            if (bl2 && !bl) {
                BlockState blockState = state.setValue(POWERED, Boolean.valueOf(true));
                world.setBlock(pos, blockState, 3);
                this.updatePowerToConnected(world, pos, blockState, true);
                world.updateNeighborsAt(pos, this);
                world.updateNeighborsAt(pos.below(), this);
                world.setBlocksDirty(pos, state, blockState);
            }

            if (!bl2 && bl) {
                BlockState blockState2 = state.setValue(POWERED, Boolean.valueOf(false));
                world.setBlock(pos, blockState2, 3);
                this.updatePowerToConnected(world, pos, blockState2, false);
                world.updateNeighborsAt(pos, this);
                world.updateNeighborsAt(pos.below(), this);
                world.setBlocksDirty(pos, state, blockState2);
            }

            if (bl2) {
                world.scheduleTick(pos, this, 20);
            }

            world.updateNeighbourForOutputSignal(pos, this);
        }
    }

    protected void updatePowerToConnected(Level world, BlockPos pos, BlockState state, boolean unpowering) {
        RailState railState = new RailState(world, pos, state);

        for(BlockPos blockPos : railState.getConnections()) {
            BlockState blockState = world.getBlockState(blockPos);
            blockState.neighborChanged(world, blockPos, blockState.getBlock(), pos, false);
        }

    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            BlockState blockState = this.updateState(state, world, pos, notify);
            this.checkPressed(world, pos, blockState);
        }
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (state.getValue(POWERED)) {
            List<MinecartCommandBlock> list = this.getInteractingMinecartOfType(world, pos, MinecartCommandBlock.class, (cart) -> {
                return true;
            });
            if (!list.isEmpty()) {
                return list.get(0).getCommandBlock().getSuccessCount();
            }

            List<AbstractMinecart> list2 = this.getInteractingMinecartOfType(world, pos, AbstractMinecart.class, EntitySelector.CONTAINER_ENTITY_SELECTOR);
            if (!list2.isEmpty()) {
                return AbstractContainerMenu.getRedstoneSignalFromContainer((Container)list2.get(0));
            }
        }

        return 0;
    }

    private <T extends AbstractMinecart> List<T> getInteractingMinecartOfType(Level world, BlockPos pos, Class<T> entityClass, Predicate<Entity> entityPredicate) {
        return world.getEntitiesOfClass(entityClass, this.getSearchBB(pos), entityPredicate);
    }

    private AABB getSearchBB(BlockPos pos) {
        double d = 0.2D;
        return new AABB((double)pos.getX() + 0.2D, (double)pos.getY(), (double)pos.getZ() + 0.2D, (double)(pos.getX() + 1) - 0.2D, (double)(pos.getY() + 1) - 0.2D, (double)(pos.getZ() + 1) - 0.2D);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            switch((RailShape)state.getValue(SHAPE)) {
            case ASCENDING_EAST:
                return state.setValue(SHAPE, RailShape.ASCENDING_WEST);
            case ASCENDING_WEST:
                return state.setValue(SHAPE, RailShape.ASCENDING_EAST);
            case ASCENDING_NORTH:
                return state.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
            case ASCENDING_SOUTH:
                return state.setValue(SHAPE, RailShape.ASCENDING_NORTH);
            case SOUTH_EAST:
                return state.setValue(SHAPE, RailShape.NORTH_WEST);
            case SOUTH_WEST:
                return state.setValue(SHAPE, RailShape.NORTH_EAST);
            case NORTH_WEST:
                return state.setValue(SHAPE, RailShape.SOUTH_EAST);
            case NORTH_EAST:
                return state.setValue(SHAPE, RailShape.SOUTH_WEST);
            }
        case COUNTERCLOCKWISE_90:
            switch((RailShape)state.getValue(SHAPE)) {
            case ASCENDING_EAST:
                return state.setValue(SHAPE, RailShape.ASCENDING_NORTH);
            case ASCENDING_WEST:
                return state.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
            case ASCENDING_NORTH:
                return state.setValue(SHAPE, RailShape.ASCENDING_WEST);
            case ASCENDING_SOUTH:
                return state.setValue(SHAPE, RailShape.ASCENDING_EAST);
            case SOUTH_EAST:
                return state.setValue(SHAPE, RailShape.NORTH_EAST);
            case SOUTH_WEST:
                return state.setValue(SHAPE, RailShape.SOUTH_EAST);
            case NORTH_WEST:
                return state.setValue(SHAPE, RailShape.SOUTH_WEST);
            case NORTH_EAST:
                return state.setValue(SHAPE, RailShape.NORTH_WEST);
            case NORTH_SOUTH:
                return state.setValue(SHAPE, RailShape.EAST_WEST);
            case EAST_WEST:
                return state.setValue(SHAPE, RailShape.NORTH_SOUTH);
            }
        case CLOCKWISE_90:
            switch((RailShape)state.getValue(SHAPE)) {
            case ASCENDING_EAST:
                return state.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
            case ASCENDING_WEST:
                return state.setValue(SHAPE, RailShape.ASCENDING_NORTH);
            case ASCENDING_NORTH:
                return state.setValue(SHAPE, RailShape.ASCENDING_EAST);
            case ASCENDING_SOUTH:
                return state.setValue(SHAPE, RailShape.ASCENDING_WEST);
            case SOUTH_EAST:
                return state.setValue(SHAPE, RailShape.SOUTH_WEST);
            case SOUTH_WEST:
                return state.setValue(SHAPE, RailShape.NORTH_WEST);
            case NORTH_WEST:
                return state.setValue(SHAPE, RailShape.NORTH_EAST);
            case NORTH_EAST:
                return state.setValue(SHAPE, RailShape.SOUTH_EAST);
            case NORTH_SOUTH:
                return state.setValue(SHAPE, RailShape.EAST_WEST);
            case EAST_WEST:
                return state.setValue(SHAPE, RailShape.NORTH_SOUTH);
            }
        default:
            return state;
        }
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        RailShape railShape = state.getValue(SHAPE);
        switch(mirror) {
        case LEFT_RIGHT:
            switch(railShape) {
            case ASCENDING_NORTH:
                return state.setValue(SHAPE, RailShape.ASCENDING_SOUTH);
            case ASCENDING_SOUTH:
                return state.setValue(SHAPE, RailShape.ASCENDING_NORTH);
            case SOUTH_EAST:
                return state.setValue(SHAPE, RailShape.NORTH_EAST);
            case SOUTH_WEST:
                return state.setValue(SHAPE, RailShape.NORTH_WEST);
            case NORTH_WEST:
                return state.setValue(SHAPE, RailShape.SOUTH_WEST);
            case NORTH_EAST:
                return state.setValue(SHAPE, RailShape.SOUTH_EAST);
            default:
                return super.mirror(state, mirror);
            }
        case FRONT_BACK:
            switch(railShape) {
            case ASCENDING_EAST:
                return state.setValue(SHAPE, RailShape.ASCENDING_WEST);
            case ASCENDING_WEST:
                return state.setValue(SHAPE, RailShape.ASCENDING_EAST);
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
            default:
                break;
            case SOUTH_EAST:
                return state.setValue(SHAPE, RailShape.SOUTH_WEST);
            case SOUTH_WEST:
                return state.setValue(SHAPE, RailShape.SOUTH_EAST);
            case NORTH_WEST:
                return state.setValue(SHAPE, RailShape.NORTH_EAST);
            case NORTH_EAST:
                return state.setValue(SHAPE, RailShape.NORTH_WEST);
            }
        }

        return super.mirror(state, mirror);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, POWERED, WATERLOGGED);
    }
}
