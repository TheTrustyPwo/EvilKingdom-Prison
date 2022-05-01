package net.minecraft.world.entity.monster;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public interface CrossbowAttackMob extends RangedAttackMob {
    void setChargingCrossbow(boolean charging);

    void shootCrossbowProjectile(LivingEntity target, ItemStack crossbow, Projectile projectile, float multiShotSpray);

    @Nullable
    LivingEntity getTarget();

    void onCrossbowAttackPerformed();

    default void performCrossbowAttack(LivingEntity entity, float speed) {
        InteractionHand interactionHand = ProjectileUtil.getWeaponHoldingHand(entity, Items.CROSSBOW);
        ItemStack itemStack = entity.getItemInHand(interactionHand);
        if (entity.isHolding(Items.CROSSBOW)) {
            CrossbowItem.performShooting(entity.level, entity, interactionHand, itemStack, speed, (float)(14 - entity.level.getDifficulty().getId() * 4));
        }

        this.onCrossbowAttackPerformed();
    }

    default void shootCrossbowProjectile(LivingEntity entity, LivingEntity target, Projectile projectile, float multishotSpray, float speed) {
        double d = target.getX() - entity.getX();
        double e = target.getZ() - entity.getZ();
        double f = Math.sqrt(d * d + e * e);
        double g = target.getY(0.3333333333333333D) - projectile.getY() + f * (double)0.2F;
        Vector3f vector3f = this.getProjectileShotVector(entity, new Vec3(d, g, e), multishotSpray);
        projectile.shoot((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z(), speed, (float)(14 - entity.level.getDifficulty().getId() * 4));
        entity.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (entity.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    default Vector3f getProjectileShotVector(LivingEntity entity, Vec3 positionDelta, float multishotSpray) {
        Vec3 vec3 = positionDelta.normalize();
        Vec3 vec32 = vec3.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (vec32.lengthSqr() <= 1.0E-7D) {
            vec32 = vec3.cross(entity.getUpVector(1.0F));
        }

        Quaternion quaternion = new Quaternion(new Vector3f(vec32), 90.0F, true);
        Vector3f vector3f = new Vector3f(vec3);
        vector3f.transform(quaternion);
        Quaternion quaternion2 = new Quaternion(vector3f, multishotSpray, true);
        Vector3f vector3f2 = new Vector3f(vec3);
        vector3f2.transform(quaternion2);
        return vector3f2;
    }
}
