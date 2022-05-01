package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
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
    private static final Map<Direction, VoxelShape> SHAPE_BY_DIRECTION = (Map) Util.make(Maps.newEnumMap(Direction.class), (enummap) -> {
        enummap.put(Direction.NORTH, MultifaceBlock.NORTH_AABB);
        enummap.put(Direction.EAST, MultifaceBlock.EAST_AABB);
        enummap.put(Direction.SOUTH, MultifaceBlock.SOUTH_AABB);
        enummap.put(Direction.WEST, MultifaceBlock.WEST_AABB);
        enummap.put(Direction.UP, MultifaceBlock.UP_AABB);
        enummap.put(Direction.DOWN, MultifaceBlock.DOWN_AABB);
    });
    protected static final Direction[] DIRECTIONS = Direction.values();
    private final ImmutableMap<BlockState, VoxelShape> shapesCache;
    private final boolean canRotate;
    private final boolean canMirrorX;
    private final boolean canMirrorZ;

    public MultifaceBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(MultifaceBlock.getDefaultMultifaceState(this.stateDefinition));
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
        Direction[] aenumdirection = MultifaceBlock.DIRECTIONS;
        int i = aenumdirection.length;

        for (int j = 0; j < i; ++j) {
            Direction enumdirection = aenumdirection[j];

            if (this.isFaceSupported(enumdirection)) {
                builder.add(MultifaceBlock.getFaceProperty(enumdirection));
            }
        }

    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return !MultifaceBlock.hasAnyFace(state) ? Blocks.AIR.defaultBlockState() : (MultifaceBlock.hasFace(state, direction) && !MultifaceBlock.canAttachTo(world, direction, neighborPos, neighborState) ? MultifaceBlock.removeFace(state, MultifaceBlock.getFaceProperty(direction)) : state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return (VoxelShape) this.shapesCache.get(state);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        boolean flag = false;
        Direction[] aenumdirection = MultifaceBlock.DIRECTIONS;
        int i = aenumdirection.length;

        for (int j = 0; j < i; ++j) {
            Direction enumdirection = aenumdirection[j];

            if (MultifaceBlock.hasFace(state, enumdirection)) {
                BlockPos blockposition1 = pos.relative(enumdirection);

                if (!MultifaceBlock.canAttachTo(world, enumdirection, blockposition1, world.getBlockState(blockposition1))) {
                    return false;
                }

                flag = true;
            }
        }

        return flag;
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return MultifaceBlock.hasAnyVacantFace(state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos blockposition = ctx.getClickedPos();
        BlockState iblockdata = world.getBlockState(blockposition);

        return (BlockState) Arrays.stream(ctx.getNearestLookingDirections()).map((enumdirection) -> {
            return this.getStateForPlacement(iblockdata, world, blockposition, enumdirection);
        }).filter(Objects::nonNull).findFirst().orElse(null); // CraftBukkit - decompile error
    }

    @Nullable
    public BlockState getStateForPlacement(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        if (!this.isFaceSupported(direction)) {
            return null;
        } else {
            BlockState iblockdata1;

            if (state.is((Block) this)) {
                if (MultifaceBlock.hasFace(state, direction)) {
                    return null;
                }

                iblockdata1 = state;
            } else if (this.isWaterloggable() && state.getFluidState().isSourceOfType(Fluids.WATER)) {
                iblockdata1 = (BlockState) this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
            } else {
                iblockdata1 = this.defaultBlockState();
            }

            BlockPos blockposition1 = pos.relative(direction);

            return MultifaceBlock.canAttachTo(world, direction, blockposition1, world.getBlockState(blockposition1)) ? (BlockState) iblockdata1.setValue(MultifaceBlock.getFaceProperty(direction), true) : null;
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        if (!this.canRotate) {
            return state;
        } else {
            Objects.requireNonNull(rotation);
            return this.mapDirections(state, rotation::rotate);
        }
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.FRONT_BACK && !this.canMirrorX) {
            return state;
        } else if (mirror == Mirror.LEFT_RIGHT && !this.canMirrorZ) {
            return state;
        } else {
            Objects.requireNonNull(mirror);
            return this.mapDirections(state, mirror::mirror);
        }
    }

    private BlockState mapDirections(BlockState state, Function<Direction, Direction> mirror) {
        BlockState iblockdata1 = state;
        Direction[] aenumdirection = MultifaceBlock.DIRECTIONS;
        int i = aenumdirection.length;

        for (int j = 0; j < i; ++j) {
            Direction enumdirection = aenumdirection[j];

            if (this.isFaceSupported(enumdirection)) {
                iblockdata1 = (BlockState) iblockdata1.setValue(MultifaceBlock.getFaceProperty((Direction) mirror.apply(enumdirection)), (Boolean) state.getValue(MultifaceBlock.getFaceProperty(enumdirection)));
            }
        }

        return iblockdata1;
    }

    public boolean spreadFromRandomFaceTowardRandomDirection(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        List<Direction> list = Lists.newArrayList(MultifaceBlock.DIRECTIONS);

        Collections.shuffle(list);
        return list.stream().filter((enumdirection) -> {
            return MultifaceBlock.hasFace(state, enumdirection);
        }).anyMatch((enumdirection) -> {
            return this.spreadFromFaceTowardRandomDirection(state, world, pos, enumdirection, random, false);
        });
    }

    public boolean spreadFromFaceTowardRandomDirection(BlockState state, LevelAccessor world, BlockPos pos, Direction from, Random random, boolean postProcess) {
        List<Direction> list = Arrays.asList(MultifaceBlock.DIRECTIONS);

        Collections.shuffle(list, random);
        return list.stream().anyMatch((enumdirection1) -> {
            return this.spreadFromFaceTowardDirection(state, world, pos, from, enumdirection1, postProcess);
        });
    }

    public boolean spreadFromFaceTowardDirection(BlockState state, LevelAccessor world, BlockPos pos, Direction from, Direction to, boolean postProcess) {
        Optional<Pair<BlockPos, Direction>> optional = this.getSpreadFromFaceTowardDirection(state, world, pos, from, to);

        if (optional.isPresent()) {
            Pair<BlockPos, Direction> pair = (Pair) optional.get();

            return this.spreadToFace(world, (BlockPos) pair.getFirst(), (Direction) pair.getSecond(), postProcess, pos); // CraftBukkit
        } else {
            return false;
        }
    }

    protected boolean canSpread(BlockState state, BlockGetter world, BlockPos pos, Direction from) {
        return Stream.of(MultifaceBlock.DIRECTIONS).anyMatch((enumdirection1) -> {
            return this.getSpreadFromFaceTowardDirection(state, world, pos, from, enumdirection1).isPresent();
        });
    }

    private Optional<Pair<BlockPos, Direction>> getSpreadFromFaceTowardDirection(BlockState state, BlockGetter world, BlockPos pos, Direction from, Direction to) {
        if (to.getAxis() != from.getAxis() && MultifaceBlock.hasFace(state, from) && !MultifaceBlock.hasFace(state, to)) {
            if (this.canSpreadToFace(world, pos, to)) {
                return Optional.of(Pair.of(pos, to));
            } else {
                BlockPos blockposition1 = pos.relative(to);

                if (this.canSpreadToFace(world, blockposition1, from)) {
                    return Optional.of(Pair.of(blockposition1, from));
                } else {
                    BlockPos blockposition2 = blockposition1.relative(from);
                    Direction enumdirection2 = to.getOpposite();

                    return this.canSpreadToFace(world, blockposition2, enumdirection2) ? Optional.of(Pair.of(blockposition2, enumdirection2)) : Optional.empty();
                }
            }
        } else {
            return Optional.empty();
        }
    }

    private boolean canSpreadToFace(BlockGetter world, BlockPos pos, Direction direction) {
        BlockState iblockdata = world.getBlockState(pos);

        if (!this.canSpreadInto(iblockdata)) {
            return false;
        } else {
            BlockState iblockdata1 = this.getStateForPlacement(iblockdata, world, pos, direction);

            return iblockdata1 != null;
        }
    }

    private boolean spreadToFace(LevelAccessor generatoraccess, BlockPos blockposition, Direction enumdirection, boolean flag, BlockPos source) { // CraftBukkit
        BlockState iblockdata = generatoraccess.getBlockState(blockposition);
        BlockState iblockdata1 = this.getStateForPlacement(iblockdata, generatoraccess, blockposition, enumdirection);

        if (iblockdata1 != null) {
            if (flag) {
                generatoraccess.getChunk(blockposition).markPosForPostprocessing(blockposition);
            }

            return org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.handleBlockSpreadEvent(generatoraccess, source, blockposition, iblockdata1, 2); // CraftBukkit
        } else {
            return false;
        }
    }

    private boolean canSpreadInto(BlockState state) {
        return state.isAir() || state.is((Block) this) || state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    private static boolean hasFace(BlockState state, Direction direction) {
        BooleanProperty blockstateboolean = MultifaceBlock.getFaceProperty(direction);

        return state.hasProperty(blockstateboolean) && (Boolean) state.getValue(blockstateboolean);
    }

    private static boolean canAttachTo(BlockGetter world, Direction direction, BlockPos pos, BlockState state) {
        return Block.isFaceFull(state.getCollisionShape(world, pos), direction.getOpposite());
    }

    private boolean isWaterloggable() {
        return this.stateDefinition.getProperties().contains(BlockStateProperties.WATERLOGGED);
    }

    private static BlockState removeFace(BlockState state, BooleanProperty direction) {
        BlockState iblockdata1 = (BlockState) state.setValue(direction, false);

        return MultifaceBlock.hasAnyFace(iblockdata1) ? iblockdata1 : Blocks.AIR.defaultBlockState();
    }

    public static BooleanProperty getFaceProperty(Direction direction) {
        return (BooleanProperty) MultifaceBlock.PROPERTY_BY_DIRECTION.get(direction);
    }

    private static BlockState getDefaultMultifaceState(StateDefinition<Block, BlockState> stateManager) {
        BlockState iblockdata = (BlockState) stateManager.any();
        Iterator iterator = MultifaceBlock.PROPERTY_BY_DIRECTION.values().iterator();

        while (iterator.hasNext()) {
            BooleanProperty blockstateboolean = (BooleanProperty) iterator.next();

            if (iblockdata.hasProperty(blockstateboolean)) {
                iblockdata = (BlockState) iblockdata.setValue(blockstateboolean, false);
            }
        }

        return iblockdata;
    }

    private static VoxelShape calculateMultifaceShape(BlockState state) {
        VoxelShape voxelshape = Shapes.empty();
        Direction[] aenumdirection = MultifaceBlock.DIRECTIONS;
        int i = aenumdirection.length;

        for (int j = 0; j < i; ++j) {
            Direction enumdirection = aenumdirection[j];

            if (MultifaceBlock.hasFace(state, enumdirection)) {
                voxelshape = Shapes.or(voxelshape, (VoxelShape) MultifaceBlock.SHAPE_BY_DIRECTION.get(enumdirection));
            }
        }

        return voxelshape.isEmpty() ? Shapes.block() : voxelshape;
    }

    protected static boolean hasAnyFace(BlockState state) {
        return Arrays.stream(MultifaceBlock.DIRECTIONS).anyMatch((enumdirection) -> {
            return MultifaceBlock.hasFace(state, enumdirection);
        });
    }

    private static boolean hasAnyVacantFace(BlockState state) {
        return Arrays.stream(MultifaceBlock.DIRECTIONS).anyMatch((enumdirection) -> {
            return !MultifaceBlock.hasFace(state, enumdirection);
        });
    }
}
