package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

// CraftBukkit start
import org.bukkit.entity.Player;
import org.bukkit.event.hanging.HangingPlaceEvent;
// CraftBukkit end

public class HangingEntityItem extends Item {

    private final EntityType<? extends HangingEntity> type;

    public HangingEntityItem(EntityType<? extends HangingEntity> type, Item.Properties settings) {
        super(settings);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos blockposition = context.getClickedPos();
        Direction enumdirection = context.getClickedFace();
        BlockPos blockposition1 = blockposition.relative(enumdirection);
        net.minecraft.world.entity.player.Player entityhuman = context.getPlayer();
        ItemStack itemstack = context.getItemInHand();

        if (entityhuman != null && !this.mayPlace(entityhuman, enumdirection, itemstack, blockposition1)) {
            return InteractionResult.FAIL;
        } else {
            Level world = context.getLevel();
            Object object;

            if (this.type == EntityType.PAINTING) {
                object = new Painting(world, blockposition1, enumdirection);
            } else if (this.type == EntityType.ITEM_FRAME) {
                object = new ItemFrame(world, blockposition1, enumdirection);
            } else {
                if (this.type != EntityType.GLOW_ITEM_FRAME) {
                    return InteractionResult.sidedSuccess(world.isClientSide);
                }

                object = new GlowItemFrame(world, blockposition1, enumdirection);
            }

            CompoundTag nbttagcompound = itemstack.getTag();

            if (nbttagcompound != null) {
                EntityType.updateCustomEntityTag(world, entityhuman, (Entity) object, nbttagcompound);
            }

            if (((HangingEntity) object).survives()) {
                if (!world.isClientSide) {
                    // CraftBukkit start - fire HangingPlaceEvent
                    Player who = (context.getPlayer() == null) ? null : (Player) context.getPlayer().getBukkitEntity();
                    org.bukkit.block.Block blockClicked = world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
                    org.bukkit.block.BlockFace blockFace = org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock.notchToBlockFace(enumdirection);

                    HangingPlaceEvent event = new HangingPlaceEvent((org.bukkit.entity.Hanging) ((HangingEntity) object).getBukkitEntity(), who, blockClicked, blockFace, org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack.asBukkitCopy(itemstack));
                    world.getCraftServer().getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        return InteractionResult.FAIL;
                    }
                    // CraftBukkit end
                    ((HangingEntity) object).playPlacementSound();
                    world.gameEvent(entityhuman, GameEvent.ENTITY_PLACE, blockposition);
                    world.addFreshEntity((Entity) object);
                }

                itemstack.shrink(1);
                return InteractionResult.sidedSuccess(world.isClientSide);
            } else {
                return InteractionResult.CONSUME;
            }
        }
    }

    protected boolean mayPlace(net.minecraft.world.entity.player.Player player, Direction side, ItemStack stack, BlockPos pos) {
        return !side.getAxis().isVertical() && player.mayUseItemAt(pos, side, stack);
    }
}
