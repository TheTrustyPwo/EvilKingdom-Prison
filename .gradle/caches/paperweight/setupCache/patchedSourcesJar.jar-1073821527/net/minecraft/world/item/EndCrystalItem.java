package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class EndCrystalItem extends Item {
    public EndCrystalItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        BlockState blockState = level.getBlockState(blockPos);
        if (!blockState.is(Blocks.OBSIDIAN) && !blockState.is(Blocks.BEDROCK)) {
            return InteractionResult.FAIL;
        } else {
            BlockPos blockPos2 = blockPos.above();
            if (!level.isEmptyBlock(blockPos2)) {
                return InteractionResult.FAIL;
            } else {
                double d = (double)blockPos2.getX();
                double e = (double)blockPos2.getY();
                double f = (double)blockPos2.getZ();
                List<Entity> list = level.getEntities((Entity)null, new AABB(d, e, f, d + 1.0D, e + 2.0D, f + 1.0D));
                if (!list.isEmpty()) {
                    return InteractionResult.FAIL;
                } else {
                    if (level instanceof ServerLevel) {
                        EndCrystal endCrystal = new EndCrystal(level, d + 0.5D, e, f + 0.5D);
                        endCrystal.setShowBottom(false);
                        level.addFreshEntity(endCrystal);
                        level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, blockPos2);
                        EndDragonFight endDragonFight = ((ServerLevel)level).dragonFight();
                        if (endDragonFight != null) {
                            endDragonFight.tryRespawn();
                        }
                    }

                    context.getItemInHand().shrink(1);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
