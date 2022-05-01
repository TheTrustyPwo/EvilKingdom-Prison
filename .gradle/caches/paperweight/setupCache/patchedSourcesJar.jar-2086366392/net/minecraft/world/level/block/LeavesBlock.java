package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.event.block.LeavesDecayEvent; // CraftBukkit

public class LeavesBlock extends Block {

    public static final int DECAY_DISTANCE = 7;
    public static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
    public static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;
    private static final int TICK_DELAY = 1;

    public LeavesBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState) ((BlockState) ((BlockState) this.stateDefinition.any()).setValue(LeavesBlock.DISTANCE, 7)).setValue(LeavesBlock.PERSISTENT, false));
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter world, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return (Integer) state.getValue(LeavesBlock.DISTANCE) == 7 && !(Boolean) state.getValue(LeavesBlock.PERSISTENT);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (!(Boolean) state.getValue(LeavesBlock.PERSISTENT) && (Integer) state.getValue(LeavesBlock.DISTANCE) == 7) {
            // CraftBukkit start
            LeavesDecayEvent event = new LeavesDecayEvent(world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()));
            world.getCraftServer().getPluginManager().callEvent(event);

            if (event.isCancelled() || world.getBlockState(pos).getBlock() != this) {
                return;
            }
            // CraftBukkit end
            dropResources(state, world, pos);
            world.removeBlock(pos, false);
        }

    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        world.setBlock(pos, LeavesBlock.updateDistance(state, world, pos), 3);
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 1;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        int i = LeavesBlock.getDistanceAt(neighborState) + 1;

        if (i != 1 || (Integer) state.getValue(LeavesBlock.DISTANCE) != i) {
            world.scheduleTick(pos, (Block) this, 1);
        }

        return state;
    }

    private static BlockState updateDistance(BlockState state, LevelAccessor world, BlockPos pos) {
        int i = 7;
        BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
        Direction[] aenumdirection = Direction.values();
        int j = aenumdirection.length;

        for (int k = 0; k < j; ++k) {
            Direction enumdirection = aenumdirection[k];

            blockposition_mutableblockposition.setWithOffset(pos, enumdirection);
            i = Math.min(i, LeavesBlock.getDistanceAt(world.getBlockState(blockposition_mutableblockposition)) + 1);
            if (i == 1) {
                break;
            }
        }

        return (BlockState) state.setValue(LeavesBlock.DISTANCE, i);
    }

    private static int getDistanceAt(BlockState state) {
        return state.is(BlockTags.LOGS) ? 0 : (state.getBlock() instanceof LeavesBlock ? (Integer) state.getValue(LeavesBlock.DISTANCE) : 7);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, Random random) {
        if (world.isRainingAt(pos.above())) {
            if (random.nextInt(15) == 1) {
                BlockPos blockposition1 = pos.below();
                BlockState iblockdata1 = world.getBlockState(blockposition1);

                if (!iblockdata1.canOcclude() || !iblockdata1.isFaceSturdy(world, blockposition1, Direction.UP)) {
                    double d0 = (double) pos.getX() + random.nextDouble();
                    double d1 = (double) pos.getY() - 0.05D;
                    double d2 = (double) pos.getZ() + random.nextDouble();

                    world.addParticle(ParticleTypes.DRIPPING_WATER, d0, d1, d2, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LeavesBlock.DISTANCE, LeavesBlock.PERSISTENT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return LeavesBlock.updateDistance((BlockState) this.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), ctx.getLevel(), ctx.getClickedPos());
    }
}
