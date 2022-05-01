package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SignItem extends StandingAndWallBlockItem {

    public static BlockPos openSign; // CraftBukkit

    public SignItem(Item.Properties settings, Block standingBlock, Block wallBlock) {
        super(standingBlock, wallBlock, settings);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level world, @Nullable Player player, ItemStack stack, BlockState state) {
        boolean flag = super.updateCustomBlockEntityTag(pos, world, player, stack, state);

        if (!world.isClientSide && !flag && player != null) {
            // CraftBukkit start - SPIGOT-4678
            // entityhuman.openTextEdit((TileEntitySign) world.getBlockEntity(blockposition));
            SignItem.openSign = pos;
            // CraftBukkit end
        }

        return flag;
    }
}
