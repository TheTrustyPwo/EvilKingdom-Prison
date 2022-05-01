package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

public class PoweredRailBlock extends BaseRailBlock {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    protected PoweredRailBlock(BlockBehaviour.Properties settings) {
        super(true, settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(POWERED, Boolean.valueOf(false)).setValue(WATERLOGGED, Boolean.valueOf(false)));
    }

    protected boolean findPoweredRailSignal(Level world, BlockPos pos, BlockState state, boolean bl, int distance) {
        if (distance >= 8) {
            return false;
        } else {
            int i = pos.getX();
            int j = pos.getY();
            int k = pos.getZ();
            boolean bl2 = true;
            RailShape railShape = state.getValue(SHAPE);
            switch(railShape) {
            case NORTH_SOUTH:
                if (bl) {
                    ++k;
                } else {
                    --k;
                }
                break;
            case EAST_WEST:
                if (bl) {
                    --i;
                } else {
                    ++i;
                }
                break;
            case ASCENDING_EAST:
                if (bl) {
                    --i;
                } else {
                    ++i;
                    ++j;
                    bl2 = false;
                }

                railShape = RailShape.EAST_WEST;
                break;
            case ASCENDING_WEST:
                if (bl) {
                    --i;
                    ++j;
                    bl2 = false;
                } else {
                    ++i;
                }

                railShape = RailShape.EAST_WEST;
                break;
            case ASCENDING_NORTH:
                if (bl) {
                    ++k;
                } else {
                    --k;
                    ++j;
                    bl2 = false;
                }

                railShape = RailShape.NORTH_SOUTH;
                break;
            case ASCENDING_SOUTH:
                if (bl) {
                    ++k;
                    ++j;
                    bl2 = false;
                } else {
                    --k;
                }

                railShape = RailShape.NORTH_SOUTH;
            }

            if (this.isSameRailWithPower(world, new BlockPos(i, j, k), bl, distance, railShape)) {
                return true;
            } else {
                return bl2 && this.isSameRailWithPower(world, new BlockPos(i, j - 1, k), bl, distance, railShape);
            }
        }
    }

    protected boolean isSameRailWithPower(Level world, BlockPos pos, boolean bl, int distance, RailShape shape) {
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.is(this)) {
            return false;
        } else {
            RailShape railShape = blockState.getValue(SHAPE);
            if (shape != RailShape.EAST_WEST || railShape != RailShape.NORTH_SOUTH && railShape != RailShape.ASCENDING_NORTH && railShape != RailShape.ASCENDING_SOUTH) {
                if (shape != RailShape.NORTH_SOUTH || railShape != RailShape.EAST_WEST && railShape != RailShape.ASCENDING_EAST && railShape != RailShape.ASCENDING_WEST) {
                    if (blockState.getValue(POWERED)) {
                        return world.hasNeighborSignal(pos) ? true : this.findPoweredRailSignal(world, pos, blockState, bl, distance + 1);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    protected void updateState(BlockState state, Level world, BlockPos pos, Block neighbor) {
        boolean bl = state.getValue(POWERED);
        boolean bl2 = world.hasNeighborSignal(pos) || this.findPoweredRailSignal(world, pos, state, true, 0) || this.findPoweredRailSignal(world, pos, state, false, 0);
        if (bl2 != bl) {
            world.setBlock(pos, state.setValue(POWERED, Boolean.valueOf(bl2)), 3);
            world.updateNeighborsAt(pos.below(), this);
            if (state.getValue(SHAPE).isAscending()) {
                world.updateNeighborsAt(pos.above(), this);
            }
        }

    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
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
            case NORTH_SOUTH:
                return state.setValue(SHAPE, RailShape.EAST_WEST);
            case EAST_WEST:
                return state.setValue(SHAPE, RailShape.NORTH_SOUTH);
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
            }
        case CLOCKWISE_90:
            switch((RailShape)state.getValue(SHAPE)) {
            case NORTH_SOUTH:
                return state.setValue(SHAPE, RailShape.EAST_WEST);
            case EAST_WEST:
                return state.setValue(SHAPE, RailShape.NORTH_SOUTH);
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
