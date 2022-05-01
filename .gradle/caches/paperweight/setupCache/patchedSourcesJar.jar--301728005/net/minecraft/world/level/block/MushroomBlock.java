package net.minecraft.world.level.block;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
// CraftBukkit start
import org.bukkit.TreeType;
// CraftBukkit end

public class MushroomBlock extends BushBlock implements BonemealableBlock {

    protected static final float AABB_OFFSET = 3.0F;
    protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);
    private final Supplier<Holder<? extends ConfiguredFeature<?, ?>>> featureSupplier;

    public MushroomBlock(BlockBehaviour.Properties settings, Supplier<Holder<? extends ConfiguredFeature<?, ?>>> feature) {
        super(settings);
        this.featureSupplier = feature;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return MushroomBlock.SHAPE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        if (random.nextInt(Math.max(1, (int) (100.0F / world.spigotConfig.mushroomModifier) * 25)) == 0) { // Spigot
            int i = 5;
            boolean flag = true;
            Iterator iterator = BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4)).iterator();

            while (iterator.hasNext()) {
                BlockPos blockposition1 = (BlockPos) iterator.next();

                if (world.getBlockState(blockposition1).is((Block) this)) {
                    --i;
                    if (i <= 0) {
                        return;
                    }
                }
            }

            BlockPos blockposition2 = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);

            for (int j = 0; j < 4; ++j) {
                if (world.isEmptyBlock(blockposition2) && state.canSurvive(world, blockposition2)) {
                    pos = blockposition2;
                }

                blockposition2 = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
            }

            if (world.isEmptyBlock(blockposition2) && state.canSurvive(world, blockposition2)) {
                org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.handleBlockSpreadEvent(world, pos, blockposition2, state, 2); // CraftBukkit
            }
        }

    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.isSolidRender(world, pos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos blockposition1 = pos.below();
        BlockState iblockdata1 = world.getBlockState(blockposition1);

        return iblockdata1.is(BlockTags.MUSHROOM_GROW_BLOCK) ? true : world.getRawBrightness(pos, 0) < 13 && this.mayPlaceOn(iblockdata1, world, blockposition1);
    }

    public boolean growMushroom(ServerLevel world, BlockPos pos, BlockState state, Random random) {
        world.removeBlock(pos, false);
        SaplingBlock.treeType = (this == Blocks.BROWN_MUSHROOM) ? TreeType.BROWN_MUSHROOM : TreeType.RED_MUSHROOM; // CraftBukkit // Paper
        if (((ConfiguredFeature) ((Holder) this.featureSupplier.get()).value()).place(world, world.getChunkSource().getGenerator(), random, pos)) {
            return true;
        } else {
            world.setBlock(pos, state, 3);
            return false;
        }
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter world, BlockPos pos, BlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level world, Random random, BlockPos pos, BlockState state) {
        return (double) random.nextFloat() < 0.4D;
    }

    @Override
    public void performBonemeal(ServerLevel world, Random random, BlockPos pos, BlockState state) {
        this.growMushroom(world, pos, state, random);
    }
}
