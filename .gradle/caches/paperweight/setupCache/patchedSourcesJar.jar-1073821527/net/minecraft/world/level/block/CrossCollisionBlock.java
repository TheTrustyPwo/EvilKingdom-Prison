package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CrossCollisionBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION.entrySet().stream().filter((entry) -> {
        return entry.getKey().getAxis().isHorizontal();
    }).collect(Util.toMap());
    protected final VoxelShape[] collisionShapeByIndex;
    protected final VoxelShape[] shapeByIndex;
    private final Object2IntMap<BlockState> stateToIndex = new Object2IntOpenHashMap<>();

    protected CrossCollisionBlock(float radius1, float radius2, float boundingHeight1, float boundingHeight2, float collisionHeight, BlockBehaviour.Properties settings) {
        super(settings);
        this.collisionShapeByIndex = this.makeShapes(radius1, radius2, collisionHeight, 0.0F, collisionHeight);
        this.shapeByIndex = this.makeShapes(radius1, radius2, boundingHeight1, 0.0F, boundingHeight2);

        for(BlockState blockState : this.stateDefinition.getPossibleStates()) {
            this.getAABBIndex(blockState);
        }

    }

    protected VoxelShape[] makeShapes(float radius1, float radius2, float height1, float offset2, float height2) {
        float f = 8.0F - radius1;
        float g = 8.0F + radius1;
        float h = 8.0F - radius2;
        float i = 8.0F + radius2;
        VoxelShape voxelShape = Block.box((double)f, 0.0D, (double)f, (double)g, (double)height1, (double)g);
        VoxelShape voxelShape2 = Block.box((double)h, (double)offset2, 0.0D, (double)i, (double)height2, (double)i);
        VoxelShape voxelShape3 = Block.box((double)h, (double)offset2, (double)h, (double)i, (double)height2, 16.0D);
        VoxelShape voxelShape4 = Block.box(0.0D, (double)offset2, (double)h, (double)i, (double)height2, (double)i);
        VoxelShape voxelShape5 = Block.box((double)h, (double)offset2, (double)h, 16.0D, (double)height2, (double)i);
        VoxelShape voxelShape6 = Shapes.or(voxelShape2, voxelShape5);
        VoxelShape voxelShape7 = Shapes.or(voxelShape3, voxelShape4);
        VoxelShape[] voxelShapes = new VoxelShape[]{Shapes.empty(), voxelShape3, voxelShape4, voxelShape7, voxelShape2, Shapes.or(voxelShape3, voxelShape2), Shapes.or(voxelShape4, voxelShape2), Shapes.or(voxelShape7, voxelShape2), voxelShape5, Shapes.or(voxelShape3, voxelShape5), Shapes.or(voxelShape4, voxelShape5), Shapes.or(voxelShape7, voxelShape5), voxelShape6, Shapes.or(voxelShape3, voxelShape6), Shapes.or(voxelShape4, voxelShape6), Shapes.or(voxelShape7, voxelShape6)};

        for(int j = 0; j < 16; ++j) {
            voxelShapes[j] = Shapes.or(voxelShape, voxelShapes[j]);
        }

        return voxelShapes;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.shapeByIndex[this.getAABBIndex(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.collisionShapeByIndex[this.getAABBIndex(state)];
    }

    private static int indexFor(Direction dir) {
        return 1 << dir.get2DDataValue();
    }

    protected int getAABBIndex(BlockState state) {
        return this.stateToIndex.computeIntIfAbsent(state, (statex) -> {
            int i = 0;
            if (statex.getValue(NORTH)) {
                i |= indexFor(Direction.NORTH);
            }

            if (statex.getValue(EAST)) {
                i |= indexFor(Direction.EAST);
            }

            if (statex.getValue(SOUTH)) {
                i |= indexFor(Direction.SOUTH);
            }

            if (statex.getValue(WEST)) {
                i |= indexFor(Direction.WEST);
            }

            return i;
        });
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        switch(rotation) {
        case CLOCKWISE_180:
            return state.setValue(NORTH, state.getValue(SOUTH)).setValue(EAST, state.getValue(WEST)).setValue(SOUTH, state.getValue(NORTH)).setValue(WEST, state.getValue(EAST));
        case COUNTERCLOCKWISE_90:
            return state.setValue(NORTH, state.getValue(EAST)).setValue(EAST, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(WEST)).setValue(WEST, state.getValue(NORTH));
        case CLOCKWISE_90:
            return state.setValue(NORTH, state.getValue(WEST)).setValue(EAST, state.getValue(NORTH)).setValue(SOUTH, state.getValue(EAST)).setValue(WEST, state.getValue(SOUTH));
        default:
            return state;
        }
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        switch(mirror) {
        case LEFT_RIGHT:
            return state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
        case FRONT_BACK:
            return state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
        default:
            return super.mirror(state, mirror);
        }
    }
}
