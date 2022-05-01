package net.minecraft.world.item;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ArmorStandItem extends Item {

    public ArmorStandItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction enumdirection = context.getClickedFace();

        if (enumdirection == Direction.DOWN) {
            return InteractionResult.FAIL;
        } else {
            Level world = context.getLevel();
            BlockPlaceContext blockactioncontext = new BlockPlaceContext(context);
            BlockPos blockposition = blockactioncontext.getClickedPos();
            ItemStack itemstack = context.getItemInHand();
            Vec3 vec3d = Vec3.atBottomCenterOf(blockposition);
            AABB axisalignedbb = EntityType.ARMOR_STAND.getDimensions().makeBoundingBox(vec3d.x(), vec3d.y(), vec3d.z());

            if (world.noCollision((Entity) null, axisalignedbb) && world.getEntities((Entity) null, axisalignedbb).isEmpty()) {
                if (world instanceof ServerLevel) {
                    ServerLevel worldserver = (ServerLevel) world;
                    ArmorStand entityarmorstand = (ArmorStand) EntityType.ARMOR_STAND.create(worldserver, itemstack.getTag(), (Component) null, context.getPlayer(), blockposition, MobSpawnType.SPAWN_EGG, true, true);

                    if (entityarmorstand == null) {
                        return InteractionResult.FAIL;
                    }

                    float f = (float) Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;

                    entityarmorstand.moveTo(entityarmorstand.getX(), entityarmorstand.getY(), entityarmorstand.getZ(), f, 0.0F);
                    this.randomizePose(entityarmorstand, world.random);
                    // CraftBukkit start
                    if (org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callEntityPlaceEvent(context, entityarmorstand).isCancelled()) {
                        return InteractionResult.FAIL;
                    }
                    // CraftBukkit end
                    worldserver.addFreshEntityWithPassengers(entityarmorstand);
                    world.playSound((Player) null, entityarmorstand.getX(), entityarmorstand.getY(), entityarmorstand.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
                    world.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, (Entity) entityarmorstand);
                }

                itemstack.shrink(1);
                return InteractionResult.sidedSuccess(world.isClientSide);
            } else {
                return InteractionResult.FAIL;
            }
        }
    }

    private void randomizePose(ArmorStand stand, Random random) {
        Rotations vector3f = stand.getHeadPose();
        float f = random.nextFloat() * 5.0F;
        float f1 = random.nextFloat() * 20.0F - 10.0F;
        Rotations vector3f1 = new Rotations(vector3f.getX() + f, vector3f.getY() + f1, vector3f.getZ());

        stand.setHeadPose(vector3f1);
        vector3f = stand.getBodyPose();
        f = random.nextFloat() * 10.0F - 5.0F;
        vector3f1 = new Rotations(vector3f.getX(), vector3f.getY() + f, vector3f.getZ());
        stand.setBodyPose(vector3f1);
    }
}
