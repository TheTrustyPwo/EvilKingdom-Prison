package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class ShearsDispenseItemBehavior extends OptionalDispenseItemBehavior {
    @Override
    protected ItemStack execute(BlockSource pointer, ItemStack stack) {
        Level level = pointer.getLevel();
        if (!level.isClientSide()) {
            BlockPos blockPos = pointer.getPos().relative(pointer.getBlockState().getValue(DispenserBlock.FACING));
            this.setSuccess(tryShearBeehive((ServerLevel)level, blockPos) || tryShearLivingEntity((ServerLevel)level, blockPos));
            if (this.isSuccess() && stack.hurt(1, level.getRandom(), (ServerPlayer)null)) {
                stack.setCount(0);
            }
        }

        return stack;
    }

    private static boolean tryShearBeehive(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.is(BlockTags.BEEHIVES, (state) -> {
            return state.hasProperty(BeehiveBlock.HONEY_LEVEL) && state.getBlock() instanceof BeehiveBlock;
        })) {
            int i = blockState.getValue(BeehiveBlock.HONEY_LEVEL);
            if (i >= 5) {
                world.playSound((Player)null, pos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
                BeehiveBlock.dropHoneycomb(world, pos);
                ((BeehiveBlock)blockState.getBlock()).releaseBeesAndResetHoneyLevel(world, blockState, pos, (Player)null, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
                world.gameEvent((Entity)null, GameEvent.SHEAR, pos);
                return true;
            }
        }

        return false;
    }

    private static boolean tryShearLivingEntity(ServerLevel world, BlockPos pos) {
        for(LivingEntity livingEntity : world.getEntitiesOfClass(LivingEntity.class, new AABB(pos), EntitySelector.NO_SPECTATORS)) {
            if (livingEntity instanceof Shearable) {
                Shearable shearable = (Shearable)livingEntity;
                if (shearable.readyForShearing()) {
                    shearable.shear(SoundSource.BLOCKS);
                    world.gameEvent((Entity)null, GameEvent.SHEAR, pos);
                    return true;
                }
            }
        }

        return false;
    }
}
