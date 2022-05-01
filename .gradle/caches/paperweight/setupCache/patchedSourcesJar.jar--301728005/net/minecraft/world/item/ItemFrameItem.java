package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;

public class ItemFrameItem extends HangingEntityItem {
    public ItemFrameItem(EntityType<? extends HangingEntity> type, Item.Properties settings) {
        super(type, settings);
    }

    @Override
    protected boolean mayPlace(Player player, Direction side, ItemStack stack, BlockPos pos) {
        return !player.level.isOutsideBuildHeight(pos) && player.mayUseItemAt(pos, side, stack);
    }
}
