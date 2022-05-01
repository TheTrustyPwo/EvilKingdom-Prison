package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class RedstoneTorchBlock extends TorchBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final Map<BlockGetter, List<RedstoneTorchBlock.Toggle>> RECENT_TOGGLES = new WeakHashMap<>();
    public static final int RECENT_TOGGLE_TIMER = 60;
    public static final int MAX_RECENT_TOGGLES = 8;
    public static final int RESTART_DELAY = 160;
    private static final int TOGGLE_DELAY = 2;

    protected RedstoneTorchBlock(BlockBehaviour.Properties settings) {
        super(settings, DustParticleOptions.REDSTONE);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.valueOf(true)));
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        for(Direction direction : Direction.values()) {
            world.updateNeighborsAt(pos.relative(direction), this);
        }

    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved) {
            for(Direction direction : Direction.values()) {
                world.updateNeighborsAt(pos.relative(direction), this);
            }

        }
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return state.getValue(LIT) && Direction.UP != direction ? 15 : 0;
    }

    protected boolean hasNeighborSignal(Level world, BlockPos pos, BlockState state) {
        return world.hasSignal(pos.below(), Direction.DOWN);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        boolean bl = this.hasNeighborSignal(world, pos, state);
        List<RedstoneTorchBlock.Toggle> list = RECENT_TOGGLES.get(world);

        while(list != null && !list.isEmpty() && world.getGameTime() - (list.get(0)).when > 60L) {
            list.remove(0);
        }

        if (state.getValue(LIT)) {
            if (bl) {
                world.setBlock(pos, state.setValue(LIT, Boolean.valueOf(false)), 3);
                if (isToggledTooFrequently(world, pos, true)) {
                    world.levelEvent(1502, pos, 0);
                    world.scheduleTick(pos, world.getBlockState(pos).getBlock(), 160);
                }
            }
        } else if (!bl && !isToggledTooFrequently(world, pos, false)) {
            world.setBlock(pos, state.setValue(LIT, Boolean.valueOf(true)), 3);
        }

    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        if (state.getValue(LIT) == this.hasNeighborSignal(world, pos, state) && !world.getBlockTicks().willTickThisTick(pos, this)) {
            world.scheduleTick(pos, this, 2);
        }

    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return direction == Direction.DOWN ? state.getSignal(world, pos, direction) : 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, Random random) {
        if (state.getValue(LIT)) {
            double d = (double)pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;
            double e = (double)pos.getY() + 0.7D + (random.nextDouble() - 0.5D) * 0.2D;
            double f = (double)pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;
            world.addParticle(this.flameParticle, d, e, f, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    private static boolean isToggledTooFrequently(Level world, BlockPos pos, boolean addNew) {
        List<RedstoneTorchBlock.Toggle> list = RECENT_TOGGLES.computeIfAbsent(world, (worldx) -> {
            return Lists.newArrayList();
        });
        if (addNew) {
            list.add(new RedstoneTorchBlock.Toggle(pos.immutable(), world.getGameTime()));
        }

        int i = 0;

        for(int j = 0; j < list.size(); ++j) {
            RedstoneTorchBlock.Toggle toggle = list.get(j);
            if (toggle.pos.equals(pos)) {
                ++i;
                if (i >= 8) {
                    return true;
                }
            }
        }

        return false;
    }

    public static class Toggle {
        final BlockPos pos;
        final long when;

        public Toggle(BlockPos pos, long time) {
            this.pos = pos;
            this.when = time;
        }
    }
}
