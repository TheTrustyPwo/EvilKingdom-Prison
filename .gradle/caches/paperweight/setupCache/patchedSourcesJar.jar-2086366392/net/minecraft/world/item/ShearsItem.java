package net.minecraft.world.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ShearsItem extends Item {
    public ShearsItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClientSide && !state.is(BlockTags.FIRE)) {
            stack.hurtAndBreak(1, miner, (e) -> {
                e.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            });
        }

        return !state.is(BlockTags.LEAVES) && !state.is(Blocks.COBWEB) && !state.is(Blocks.GRASS) && !state.is(Blocks.FERN) && !state.is(Blocks.DEAD_BUSH) && !state.is(Blocks.HANGING_ROOTS) && !state.is(Blocks.VINE) && !state.is(Blocks.TRIPWIRE) && !state.is(BlockTags.WOOL) ? super.mineBlock(stack, world, state, pos, miner) : true;
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return state.is(Blocks.COBWEB) || state.is(Blocks.REDSTONE_WIRE) || state.is(Blocks.TRIPWIRE);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!state.is(Blocks.COBWEB) && !state.is(BlockTags.LEAVES)) {
            if (state.is(BlockTags.WOOL)) {
                return 5.0F;
            } else {
                return !state.is(Blocks.VINE) && !state.is(Blocks.GLOW_LICHEN) ? super.getDestroySpeed(stack, state) : 2.0F;
            }
        } else {
            return 15.0F;
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        BlockState blockState = level.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof GrowingPlantHeadBlock) {
            GrowingPlantHeadBlock growingPlantHeadBlock = (GrowingPlantHeadBlock)block;
            if (!growingPlantHeadBlock.isMaxAge(blockState)) {
                Player player = context.getPlayer();
                ItemStack itemStack = context.getItemInHand();
                if (player instanceof ServerPlayer) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockPos, itemStack);
                }

                level.playSound(player, blockPos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.setBlockAndUpdate(blockPos, growingPlantHeadBlock.getMaxAgeState(blockState));
                if (player != null) {
                    itemStack.hurtAndBreak(1, player, (playerx) -> {
                        playerx.broadcastBreakEvent(context.getHand());
                    });
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return super.useOn(context);
    }
}
