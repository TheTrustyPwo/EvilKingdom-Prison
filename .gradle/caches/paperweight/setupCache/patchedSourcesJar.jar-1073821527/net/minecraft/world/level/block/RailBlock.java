package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

public class RailBlock extends BaseRailBlock {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;

    protected RailBlock(BlockBehaviour.Properties settings) {
        super(false, settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    protected void updateState(BlockState state, Level world, BlockPos pos, Block neighbor) {
        if (neighbor.defaultBlockState().isSignalSource() && (new RailState(world, pos, state)).countPotentialConnections() == 3) {
            this.updateDir(world, pos, state, false);
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
        builder.add(SHAPE, WATERLOGGED);
    }
}
