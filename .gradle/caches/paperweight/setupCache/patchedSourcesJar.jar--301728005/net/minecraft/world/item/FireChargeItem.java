package net.minecraft.world.item;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public class FireChargeItem extends Item {

    public FireChargeItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos blockposition = context.getClickedPos();
        BlockState iblockdata = world.getBlockState(blockposition);
        boolean flag = false;

        if (!CampfireBlock.canLight(iblockdata) && !CandleBlock.canLight(iblockdata) && !CandleCakeBlock.canLight(iblockdata)) {
            blockposition = blockposition.relative(context.getClickedFace());
            if (BaseFireBlock.canBePlacedAt(world, blockposition, context.getHorizontalDirection())) {
                // CraftBukkit start - fire BlockIgniteEvent
                if (org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callBlockIgniteEvent(world, blockposition, org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FIREBALL, context.getPlayer()).isCancelled()) {
                    if (!context.getPlayer().getAbilities().instabuild) {
                        context.getItemInHand().shrink(1);
                    }
                    return InteractionResult.PASS;
                }
                // CraftBukkit end
                this.playSound(world, blockposition);
                world.setBlockAndUpdate(blockposition, BaseFireBlock.getState(world, blockposition));
                world.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, blockposition);
                flag = true;
            }
        } else {
            // CraftBukkit start - fire BlockIgniteEvent
            if (org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callBlockIgniteEvent(world, blockposition, org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FIREBALL, context.getPlayer()).isCancelled()) {
                if (!context.getPlayer().getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                return InteractionResult.PASS;
            }
            // CraftBukkit end
            this.playSound(world, blockposition);
            world.setBlockAndUpdate(blockposition, (BlockState) iblockdata.setValue(BlockStateProperties.LIT, true));
            world.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, blockposition);
            flag = true;
        }

        if (flag) {
            context.getItemInHand().shrink(1);
            return InteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return InteractionResult.FAIL;
        }
    }

    private void playSound(Level world, BlockPos pos) {
        Random random = world.getRandom();

        world.playSound((Player) null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
    }
}
