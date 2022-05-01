package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class CaveVinesBlock extends GrowingPlantHeadBlock implements BonemealableBlock, CaveVines {

    private static final float CHANCE_OF_BERRIES_ON_GROWTH = 0.11F;

    public CaveVinesBlock(BlockBehaviour.Properties settings) {
        super(settings, Direction.DOWN, CaveVinesBlock.SHAPE, false, 0.1D);
        this.registerDefaultState((BlockState) ((BlockState) ((BlockState) this.stateDefinition.any()).setValue(CaveVinesBlock.AGE, 0)).setValue(CaveVinesBlock.BERRIES, false));
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(Random random) {
        return 1;
    }

    @Override
    protected boolean canGrowInto(BlockState state) {
        return state.isAir();
    }

    @Override
    protected Block getBodyBlock() {
        return Blocks.CAVE_VINES_PLANT;
    }

    @Override
    protected BlockState updateBodyAfterConvertedFromHead(BlockState from, BlockState to) {
        return (BlockState) to.setValue(CaveVinesBlock.BERRIES, (Boolean) from.getValue(CaveVinesBlock.BERRIES));
    }

    @Override
    protected BlockState getGrowIntoState(BlockState state, Random random) {
        // Paper start
        return this.getGrowIntoState(state, random, null);
    }

    @Override
    protected BlockState getGrowIntoState(BlockState state, Random random, Level level) {
        final boolean value = (level == null ? random.nextFloat() : random.nextFloat(100.00F / level.spigotConfig.glowBerryModifier)) < 0.11F;
        return super.getGrowIntoState(state, random).setValue(CaveVinesBlock.BERRIES, value);
    }
    // Paper end

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        return new ItemStack(Items.GLOW_BERRIES);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return CaveVines.use(state, world, pos, player); // CraftBukkit
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CaveVinesBlock.BERRIES);
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter world, BlockPos pos, BlockState state, boolean isClient) {
        return !(Boolean) state.getValue(CaveVinesBlock.BERRIES);
    }

    @Override
    public boolean isBonemealSuccess(Level world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, Random random, BlockPos pos, BlockState state) {
        world.setBlock(pos, (BlockState) state.setValue(CaveVinesBlock.BERRIES, true), 2);
    }
}
