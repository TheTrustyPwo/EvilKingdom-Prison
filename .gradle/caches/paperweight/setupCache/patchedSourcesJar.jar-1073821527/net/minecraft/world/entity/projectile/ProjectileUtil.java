package net.minecraft.world.entity.projectile;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ProjectileUtil {
    public static HitResult getHitResult(Entity entity, Predicate<Entity> predicate) {
        Vec3 vec3 = entity.getDeltaMovement();
        Level level = entity.level;
        Vec3 vec32 = entity.position();
        Vec3 vec33 = vec32.add(vec3);
        HitResult hitResult = level.clip(new ClipContext(vec32, vec33, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hitResult.getType() != HitResult.Type.MISS) {
            vec33 = hitResult.getLocation();
        }

        HitResult hitResult2 = getEntityHitResult(level, entity, vec32, vec33, entity.getBoundingBox().expandTowards(entity.getDeltaMovement()).inflate(1.0D), predicate);
        if (hitResult2 != null) {
            hitResult = hitResult2;
        }

        return hitResult;
    }

    @Nullable
    public static EntityHitResult getEntityHitResult(Entity entity, Vec3 min, Vec3 max, AABB box, Predicate<Entity> predicate, double d) {
        Level level = entity.level;
        double e = d;
        Entity entity2 = null;
        Vec3 vec3 = null;

        for(Entity entity3 : level.getEntities(entity, box, predicate)) {
            AABB aABB = entity3.getBoundingBox().inflate((double)entity3.getPickRadius());
            Optional<Vec3> optional = aABB.clip(min, max);
            if (aABB.contains(min)) {
                if (e >= 0.0D) {
                    entity2 = entity3;
                    vec3 = optional.orElse(min);
                    e = 0.0D;
                }
            } else if (optional.isPresent()) {
                Vec3 vec32 = optional.get();
                double f = min.distanceToSqr(vec32);
                if (f < e || e == 0.0D) {
                    if (entity3.getRootVehicle() == entity.getRootVehicle()) {
                        if (e == 0.0D) {
                            entity2 = entity3;
                            vec3 = vec32;
                        }
                    } else {
                        entity2 = entity3;
                        vec3 = vec32;
                        e = f;
                    }
                }
            }
        }

        return entity2 == null ? null : new EntityHitResult(entity2, vec3);
    }

    @Nullable
    public static EntityHitResult getEntityHitResult(Level world, Entity entity, Vec3 min, Vec3 max, AABB box, Predicate<Entity> predicate) {
        return getEntityHitResult(world, entity, min, max, box, predicate, 0.3F);
    }

    @Nullable
    public static EntityHitResult getEntityHitResult(Level world, Entity entity, Vec3 min, Vec3 max, AABB box, Predicate<Entity> predicate, float f) {
        double d = Double.MAX_VALUE;
        Entity entity2 = null;

        for(Entity entity3 : world.getEntities(entity, box, predicate)) {
            AABB aABB = entity3.getBoundingBox().inflate((double)f);
            Optional<Vec3> optional = aABB.clip(min, max);
            if (optional.isPresent()) {
                double e = min.distanceToSqr(optional.get());
                if (e < d) {
                    entity2 = entity3;
                    d = e;
                }
            }
        }

        return entity2 == null ? null : new EntityHitResult(entity2);
    }

    public static void rotateTowardsMovement(Entity entity, float delta) {
        Vec3 vec3 = entity.getDeltaMovement();
        if (vec3.lengthSqr() != 0.0D) {
            double d = vec3.horizontalDistance();
            entity.setYRot((float)(Mth.atan2(vec3.z, vec3.x) * (double)(180F / (float)Math.PI)) + 90.0F);
            entity.setXRot((float)(Mth.atan2(d, vec3.y) * (double)(180F / (float)Math.PI)) - 90.0F);

            while(entity.getXRot() - entity.xRotO < -180.0F) {
                entity.xRotO -= 360.0F;
            }

            while(entity.getXRot() - entity.xRotO >= 180.0F) {
                entity.xRotO += 360.0F;
            }

            while(entity.getYRot() - entity.yRotO < -180.0F) {
                entity.yRotO -= 360.0F;
            }

            while(entity.getYRot() - entity.yRotO >= 180.0F) {
                entity.yRotO += 360.0F;
            }

            entity.setXRot(Mth.lerp(delta, entity.xRotO, entity.getXRot()));
            entity.setYRot(Mth.lerp(delta, entity.yRotO, entity.getYRot()));
        }
    }

    public static InteractionHand getWeaponHoldingHand(LivingEntity entity, Item item) {
        return entity.getMainHandItem().is(item) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static AbstractArrow getMobArrow(LivingEntity entity, ItemStack stack, float damageModifier) {
        ArrowItem arrowItem = (ArrowItem)(stack.getItem() instanceof ArrowItem ? stack.getItem() : Items.ARROW);
        AbstractArrow abstractArrow = arrowItem.createArrow(entity.level, stack, entity);
        abstractArrow.setEnchantmentEffectsFromEntity(entity, damageModifier);
        if (stack.is(Items.TIPPED_ARROW) && abstractArrow instanceof Arrow) {
            ((Arrow)abstractArrow).setEffectsFromItem(stack);
        }

        return abstractArrow;
    }
}
