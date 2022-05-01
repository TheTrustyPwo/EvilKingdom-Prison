package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BoatItem extends Item {

    private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);
    private final Boat.Type type;

    public BoatItem(Boat.Type type, Item.Properties settings) {
        super(settings);
        this.type = type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack itemstack = user.getItemInHand(hand);
        BlockHitResult movingobjectpositionblock = getPlayerPOVHitResult(world, user, ClipContext.Fluid.ANY);

        if (movingobjectpositionblock.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemstack);
        } else {
            Vec3 vec3d = user.getViewVector(1.0F);
            double d0 = 5.0D;
            List<Entity> list = world.getEntities((Entity) user, user.getBoundingBox().expandTowards(vec3d.scale(5.0D)).inflate(1.0D), BoatItem.ENTITY_PREDICATE);

            if (!list.isEmpty()) {
                Vec3 vec3d1 = user.getEyePosition();
                Iterator iterator = list.iterator();

                while (iterator.hasNext()) {
                    Entity entity = (Entity) iterator.next();
                    AABB axisalignedbb = entity.getBoundingBox().inflate((double) entity.getPickRadius());

                    if (axisalignedbb.contains(vec3d1)) {
                        return InteractionResultHolder.pass(itemstack);
                    }
                }
            }

            if (movingobjectpositionblock.getType() == HitResult.Type.BLOCK) {
                // CraftBukkit start - Boat placement
                org.bukkit.event.player.PlayerInteractEvent event = org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callPlayerInteractEvent(user, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, movingobjectpositionblock.getBlockPos(), movingobjectpositionblock.getDirection(), itemstack, hand);

                if (event.isCancelled()) {
                    return InteractionResultHolder.pass(itemstack);
                }
                // CraftBukkit end
                Boat entityboat = new Boat(world, movingobjectpositionblock.getLocation().x, movingobjectpositionblock.getLocation().y, movingobjectpositionblock.getLocation().z);

                entityboat.setType(this.type);
                entityboat.setYRot(user.getYRot());
                if (!world.noCollision(entityboat, entityboat.getBoundingBox())) {
                    return InteractionResultHolder.fail(itemstack);
                } else {
                    if (!world.isClientSide) {
                        // CraftBukkit start
                        if (org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callEntityPlaceEvent(world, movingobjectpositionblock.getBlockPos(), movingobjectpositionblock.getDirection(), user, entityboat).isCancelled()) {
                            return InteractionResultHolder.fail(itemstack);
                        }

                        if (!world.addFreshEntity(entityboat)) {
                            return InteractionResultHolder.pass(itemstack);
                        }
                        // CraftBukkit end
                        world.gameEvent(user, GameEvent.ENTITY_PLACE, new BlockPos(movingobjectpositionblock.getLocation()));
                        if (!user.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                    }

                    user.awardStat(Stats.ITEM_USED.get(this));
                    return InteractionResultHolder.sidedSuccess(itemstack, world.isClientSide());
                }
            } else {
                return InteractionResultHolder.pass(itemstack);
            }
        }
    }
}
