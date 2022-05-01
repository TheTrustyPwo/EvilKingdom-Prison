package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WitherWallSkullBlock extends WallSkullBlock {
    protected WitherWallSkullBlock(BlockBehaviour.Properties settings) {
        super(SkullBlock.Types.WITHER_SKELETON, settings);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        Blocks.WITHER_SKELETON_SKULL.setPlacedBy(world, pos, state, placer, itemStack);
    }
}
