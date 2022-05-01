package net.minecraft.world.item;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BedItem extends BlockItem {
    public BedItem(Block block, Item.Properties settings) {
        super(block, settings);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        return context.getLevel().setBlock(context.getClickedPos(), state, 26);
    }
}
