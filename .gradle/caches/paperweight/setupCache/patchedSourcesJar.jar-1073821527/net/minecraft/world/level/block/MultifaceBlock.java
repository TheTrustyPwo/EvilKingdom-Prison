package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MultifaceBlock extends Block {
    private static final float AABB_OFFSET = 1.0F;
    private static final VoxelShape UP_AABB = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape DOWN_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
    private static final VoxelShape WEST_AABB = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
    private static final VoxelShape EAST_AABB = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final Map<Direction, VoxelShape> SHAPE_BY_DIRECTION = Util.make(Maps.newEnumMap(Direction.class), (shapes) -> {
        shapes.put(Direction.NORTH, NORTH_AABB);
        shapes.put(Direction.EAST, EAST_AABB);
        shapes.put(Direction.SOUTH, SOUTH_AABB);
        shapes.put(Direction.WEST, WEST_AABB);
        shapes.put(Direction.UP, UP_AABB);
        shapes.put(Direction.DOWN, DOWN_AABB);
    });
    protected static final Direction[] DIRECTIONS = Direction.values();
    private final ImmutableMap<BlockState, VoxelShape> shapesCache;
    private final boolean canRotate;
    private final boolean canMirrorX;
    private final boolean canMirrorZ;

    public MultifaceBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(getDefaultMultifaceState(this.stateDefinition));
        this.shapesCache = this.getShapeForEachState(MultifaceBlock::calculateMultifaceShape);
        this.canRotate = Direction.Plane.HORIZONTAL.stream().allMatch(this::isFaceSupported);
        this.canMirrorX = Direction.Plane.HORIZONTAL.stream().filter(Direction.Axis.X).filter(this::isFaceSupported).count() % 2L == 0L;
        this.canMirrorZ = Direction.Plane.HORIZONTAL.stream().filter(Direction.Axis.Z).filter(this::isFaceSupported).count() % 2L == 0L;
    }

    protected boolean isFaceSupported(Direction direction) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for(Direction direction : DIRECTIONS) {
            if (this.isFaceSupported(direction)) {
                builder.add(getFaceProperty(direction));
            }
        }

    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (!hasAnyFace(state)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            return hasFace(state, direction) && !canAttachTo(world, direction, neighborPos, neighborState) ? removeFace(state, getFaceProperty(direction)) : state;
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.shapesCache.get(state);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        boolean bl = false;

        for(Direction direction : DIRECTIONS) {
            if (hasFace(state, direction)) {
                BlockPos blockPos = pos.relative(direction);
                if (!canAttachTo(world, direction, blockPos, world.getBlockState(blockPos))) {
                    return false;
                }

                bl = true;
            }
        }

        return bl;
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return hasAnyVacantFace(state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos blockPos = ctx.getClickedPos();
        BlockState blockState = level.getBlockState(blockPos);
        return Arrays.stream(ctx.getNearestLookingDirections()).map((direction) -> {
            return this.getStateForPlacement(blockState, level, blockPos, direction);
        }).filter(Objects::nonNull).findFirst().orElse((BlockState)null);
    }

    @Nullable
    public BlockState getStateForPlacement(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        if (!this.isFaceSupported(direction)) {
            return null;
        } else {
            BlockState blockState;
            if (state.is(this)) {
                if (hasFace(state, direction)) {
                    return null;
                }

                blockState = state;
            } else if (this.isWaterloggable() && state.getFluidState().isSourceOfType(Fluids.WATER)) {
                blockState = this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true));
            } else {
                blockState = this.defaultBlockState();
            }

            BlockPos blockPos = pos.relative(direction);
            return canAttachTo(world, direction, blockPos, world.getBlockState(blockPos)) ? blockState.setValue(getFaceProperty(direction), Boolean.valueOf(true)) : null;
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return !this.canRotate ? state : this.mapDirections(state, rotation::rotate);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.FRONT_BACK && !this.canMirrorX) {
            return state;
        } else {
            return mirror == Mirror.LEFT_RIGHT && !this.canMirrorZ ? state : this.mapDirections(state, mirror::mirror);
        }
    }

    private BlockState mapDirections(BlockState state, Function<Direction, Direction> mirror) {
        BlockState blockState = state;

        for(Direction direction : DIRECTIONS) {
            if (this.isFaceSupported(direction)) {
                blockState = blockState.setValue(getFaceProperty(mirror.apply(direction)), state.getValue(getFaceProperty(direction)));
            }
        }

        return blockState;
    }

    public boolean spreadFromRandomFaceTowardRandomDirection(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        List<Direction> list = Lists.newArrayList(DIRECTIONS);
        Collections.shuffle(list);
        return list.stream().filter((from) -> {
            return hasFace(state, from);
        }).anyMatch((to) -> {
            return this.spreadFromFaceTowardRandomDirection(state, world, pos, to, random, false);
        });
    }

    public boolean spreadFromFaceTowardRandomDirection(BlockState state, LevelAccessor world, BlockPos pos, Direction from, Random random, boolean postProcess) {
        List<Direction> list = Arrays.asList(DIRECTIONS);
        Collections.shuffle(list, random);
        return list.stream().anyMatch((to) -> {
            return this.spreadFromFaceTowardDirection(state, world, pos, from, to, postProcess);
        });
    }

    public boolean spreadFromFaceTowardDirection(BlockState state, LevelAccessor world, BlockPos pos, Direction from, Direction to, boolean postProcess) {
        Optional<Pair<BlockPos, Direction>> optional = this.getSpreadFromFaceTowardDirection(state, world, pos, from, to);
        if (optional.isPresent()) {
            Pair<BlockPos, Direction> pair = optional.get();
            return this.spreadToFace(world, pair.getFirst(), pair.getSecond(), postProcess);
        } else {
            return false;
        }
    }

    protected boolean canSpread(BlockState state, BlockGetter world, BlockPos pos, Direction from) {
        return Stream.of(DIRECTIONS).anyMatch((to) -> {
            return this.getSpreadFromFaceTowardDirection(state, world, pos, from, to).isPresent();
        });
    }

    private Optional<Pair<BlockPos, Direction>> getSpreadFromFaceTowardDirection(BlockState state, BlockGetter world, BlockPos pos, Direction from, Direction to) {
        if (to.getAxis() != from.getAxis() && hasFace(state, from) && !hasFace(state, to)) {
            if (this.canSpreadToFace(world, pos, to)) {
                return Optional.of(Pair.of(pos, to));
            } else {
                BlockPos blockPos = pos.relative(to);
                if (this.canSpreadToFace(world, blockPos, from)) {
                    return Optional.of(Pair.of(blockPos, from));
                } else {
                    BlockPos blockPos2 = blockPos.relative(from);
                    Direction direction = to.getOpposite();
                    return this.canSpreadToFace(world, blockPos2, direction) ? Optional.of(Pair.of(blockPos2, direction)) : Optional.empty();
                }
            }
        } else {
            return Optional.empty();
        }
    }

    private boolean canSpreadToFace(BlockGetter world, BlockPos pos, Direction direction) {
        BlockState blockState = world.getBlockState(pos);
        if (!this.canSpreadInto(blockState)) {
            return false;
        } else {
            BlockState blockState2 = this.getStateForPlacement(blockState, world, pos, direction);
            return blockState2 != null;
        }
    }

    private boolean spreadToFace(LevelAccessor world, BlockPos pos, Direction direction, boolean postProcess) {
        BlockState blockState = world.getBlockState(pos);
        BlockState blockState2 = this.getStateForPlacement(blockState, world, pos, direction);
        if (blockState2 != null) {
            if (postProcess) {
                world.getChunk(pos).markPosForPostprocessing(pos);
            }

            return world.setBlock(pos, blockState2, 2);
        } else {
            return false;
        }
    }

    private boolean canSpreadInto(BlockState state) {
        return state.isAir() || state.is(this) || state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    private static boolean hasFace(BlockState state, Direction direction) {
        BooleanProperty booleanProperty = getFaceProperty(direction);
        return state.hasProperty(booleanProperty) && state.getValue(booleanProperty);
    }

    private static boolean canAttachTo(BlockGetter world, Direction direction, BlockPos pos, BlockState state) {
        return Block.isFaceFull(state.getCollisionShape(world, pos), direction.getOpposite());
    }

    private boolean isWaterloggable() {
        return this.stateDefinition.getProperties().contains(BlockStateProperties.WATERLOGGED);
    }

    private static BlockState removeFace(BlockState state, BooleanProperty direction) {
        BlockState blockState = state.setValue(direction, Boolean.valueOf(false));
        return hasAnyFace(blockState) ? blockState : Blocks.AIR.defaultBlockState();
    }

    public static BooleanProperty getFaceProperty(Direction direction) {
        return PROPERTY_BY_DIRECTION.get(direction);
    }

    private static BlockState getDefaultMultifaceState(StateDefinition<Block, BlockState> stateManager) {
        BlockState blockState = stateManager.any();

        for(BooleanProperty booleanProperty : PROPERTY_BY_DIRECTION.values()) {
            if (blockState.hasProperty(booleanProperty)) {
                blockState = blockState.setValue(booleanProperty, Boolean.valueOf(false));
            }
        }

        return blockState;
    }

    private static VoxelShape calculateMultifaceShape(BlockState state) {
        VoxelShape voxelShape = Shapes.empty();

        for(Direction direction : DIRECTIONS) {
            if (hasFace(state, direction)) {
                voxelShape = Shapes.or(voxelShape, SHAPE_BY_DIRECTION.get(direction));
            }
        }

        return voxelShape.isEmpty() ? Shapes.block() : voxelShape;
    }

    protected static boolean hasAnyFace(BlockState state) {
        return Arrays.stream(DIRECTIONS).anyMatch((direction) -> {
            return hasFace(state, direction);
        });
    }

    private static boolean hasAnyVacantFace(BlockState state) {
        return Arrays.stream(DIRECTIONS).anyMatch((direction) -> {
            return !hasFace(state, direction);
        });
    }
}
