package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;

// CraftBukkit start
import java.util.List;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_18_R2.util.BlockStateListPopulator;
import org.bukkit.event.block.SpongeAbsorbEvent;
// CraftBukkit end

public class SpongeBlock extends Block {

    public static final int MAX_DEPTH = 6;
    public static final int MAX_COUNT = 64;

    protected SpongeBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.tryAbsorbWater(world, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        this.tryAbsorbWater(world, pos);
        super.neighborChanged(state, world, pos, block, fromPos, notify);
    }

    protected void tryAbsorbWater(Level world, BlockPos pos) {
        if (this.removeWaterBreadthFirstSearch(world, pos)) {
            world.setBlock(pos, Blocks.WET_SPONGE.defaultBlockState(), 2);
            world.levelEvent(2001, pos, Block.getId(Blocks.WATER.defaultBlockState()));
        }

    }

    private boolean removeWaterBreadthFirstSearch(Level world, BlockPos pos) {
        Queue<Tuple<BlockPos, Integer>> queue = Lists.newLinkedList();

        queue.add(new Tuple<>(pos, 0));
        int i = 0;
        BlockStateListPopulator blockList = new BlockStateListPopulator(world); // CraftBukkit - Use BlockStateListPopulator

        while (!queue.isEmpty()) {
            Tuple<BlockPos, Integer> tuple = (Tuple) queue.poll();
            BlockPos blockposition1 = (BlockPos) tuple.getA();
            int j = (Integer) tuple.getB();
            Direction[] aenumdirection = Direction.values();
            int k = aenumdirection.length;

            for (int l = 0; l < k; ++l) {
                Direction enumdirection = aenumdirection[l];
                BlockPos blockposition2 = blockposition1.relative(enumdirection);
                // CraftBukkit start
                BlockState iblockdata = blockList.getBlockState(blockposition2);
                FluidState fluid = blockList.getFluidState(blockposition2);
                // CraftBukkit end
                Material material = iblockdata.getMaterial();

                if (fluid.is(FluidTags.WATER)) {
                    if (iblockdata.getBlock() instanceof BucketPickup && !((BucketPickup) iblockdata.getBlock()).pickupBlock(blockList, blockposition2, iblockdata).isEmpty()) { // CraftBukkit
                        ++i;
                        if (j < 6) {
                            queue.add(new Tuple<>(blockposition2, j + 1));
                        }
                    } else if (iblockdata.getBlock() instanceof LiquidBlock) {
                        blockList.setBlock(blockposition2, Blocks.AIR.defaultBlockState(), 3); // CraftBukkit
                        ++i;
                        if (j < 6) {
                            queue.add(new Tuple<>(blockposition2, j + 1));
                        }
                    } else if (material == Material.WATER_PLANT || material == Material.REPLACEABLE_WATER_PLANT) {
                        // CraftBukkit start
                        // TileEntity tileentity = iblockdata.hasBlockEntity() ? world.getBlockEntity(blockposition2) : null;

                        // dropResources(iblockdata, world, blockposition2, tileentity);
                        blockList.setBlock(blockposition2, Blocks.AIR.defaultBlockState(), 3);
                        // CraftBukkit end
                        ++i;
                        if (j < 6) {
                            queue.add(new Tuple<>(blockposition2, j + 1));
                        }
                    }
                }
            }

            if (i > 64) {
                break;
            }
        }
        // CraftBukkit start
        List<CraftBlockState> blocks = blockList.getList(); // Is a clone
        if (!blocks.isEmpty()) {
            final org.bukkit.block.Block bblock = world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());

            SpongeAbsorbEvent event = new SpongeAbsorbEvent(bblock, (List<org.bukkit.block.BlockState>) (List) blocks);
            world.getCraftServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return false;
            }

            for (CraftBlockState block : blocks) {
                BlockPos blockposition2 = block.getPosition();
                BlockState iblockdata = world.getBlockState(blockposition2);
                FluidState fluid = world.getFluidState(blockposition2);
                Material material = iblockdata.getMaterial();

                if (fluid.is(FluidTags.WATER)) {
                    if (iblockdata.getBlock() instanceof BucketPickup && !((BucketPickup) iblockdata.getBlock()).pickupBlock(blockList, blockposition2, iblockdata).isEmpty()) {
                        // NOP
                    } else if (iblockdata.getBlock() instanceof LiquidBlock) {
                        // NOP
                    } else if (material == Material.WATER_PLANT || material == Material.REPLACEABLE_WATER_PLANT) {
                        BlockEntity tileentity = iblockdata.hasBlockEntity() ? world.getBlockEntity(blockposition2) : null;
                        // Paper start
                        if (block.getHandle().getMaterial() == Material.AIR) {
                            dropResources(iblockdata, world, blockposition2, tileentity);
                        }
                        // Paper end
                    }
                }
                world.setBlock(blockposition2, block.getHandle(), block.getFlag());
            }
        }
        // CraftBukkit end

        return i > 0;
    }
}
