package net.minecraft.world.item;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public class StandingAndWallBlockItem extends BlockItem {
    public final Block wallBlock;

    public StandingAndWallBlockItem(Block standingBlock, Block wallBlock, Item.Properties settings) {
        super(standingBlock, settings);
        this.wallBlock = wallBlock;
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState blockState = this.wallBlock.getStateForPlacement(context);
        BlockState blockState2 = null;
        LevelReader levelReader = context.getLevel();
        BlockPos blockPos = context.getClickedPos();

        for(Direction direction : context.getNearestLookingDirections()) {
            if (direction != Direction.UP) {
                BlockState blockState3 = direction == Direction.DOWN ? this.getBlock().getStateForPlacement(context) : blockState;
                if (blockState3 != null && blockState3.canSurvive(levelReader, blockPos)) {
                    blockState2 = blockState3;
                    break;
                }
            }
        }

        return blockState2 != null && levelReader.isUnobstructed(blockState2, blockPos, CollisionContext.empty()) ? blockState2 : null;
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        super.registerBlocks(map, item);
        map.put(this.wallBlock, item);
    }
}
