package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.bukkit.event.hanging.HangingPlaceEvent; // CraftBukkit

public class LeadItem extends Item {

    public LeadItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos blockposition = context.getClickedPos();
        BlockState iblockdata = world.getBlockState(blockposition);

        if (iblockdata.is(BlockTags.FENCES)) {
            Player entityhuman = context.getPlayer();

            if (!world.isClientSide && entityhuman != null) {
                LeadItem.bindPlayerMobs(entityhuman, world, blockposition);
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    public static InteractionResult bindPlayerMobs(Player player, Level world, BlockPos pos) {
        LeashFenceKnotEntity entityleash = null;
        boolean flag = false;
        double d0 = 7.0D;
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        List<Mob> list = world.getEntitiesOfClass(Mob.class, new AABB((double) i - 7.0D, (double) j - 7.0D, (double) k - 7.0D, (double) i + 7.0D, (double) j + 7.0D, (double) k + 7.0D));
        Iterator iterator = list.iterator();

        while (iterator.hasNext()) {
            Mob entityinsentient = (Mob) iterator.next();

            if (entityinsentient.getLeashHolder() == player) {
                if (entityleash == null) {
                    entityleash = LeashFenceKnotEntity.getOrCreateKnot(world, pos);

                    // CraftBukkit start - fire HangingPlaceEvent
                    HangingPlaceEvent event = new HangingPlaceEvent((org.bukkit.entity.Hanging) entityleash.getBukkitEntity(), player != null ? (org.bukkit.entity.Player) player.getBukkitEntity() : null, world.getWorld().getBlockAt(i, j, k), org.bukkit.block.BlockFace.SELF);
                    world.getCraftServer().getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        entityleash.discard();
                        return InteractionResult.PASS;
                    }
                    // CraftBukkit end
                    entityleash.playPlacementSound();
                }

                // CraftBukkit start
                if (org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callPlayerLeashEntityEvent(entityinsentient, entityleash, player).isCancelled()) {
                    continue;
                }
                // CraftBukkit end

                entityinsentient.setLeashedTo(entityleash, true);
                flag = true;
            }
        }

        return flag ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
