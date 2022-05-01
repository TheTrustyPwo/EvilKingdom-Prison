package net.minecraft.world.entity.projectile;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
// CraftBukkit start
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.entity.LivingEntity;
// CraftBukkit end

public class ThrownPotion extends ThrowableItemProjectile implements ItemSupplier {

    public static final double SPLASH_RANGE = 4.0D;
    private static final double SPLASH_RANGE_SQ = 16.0D;
    public static final Predicate<net.minecraft.world.entity.LivingEntity> WATER_SENSITIVE = net.minecraft.world.entity.LivingEntity::isSensitiveToWater;

    public ThrownPotion(EntityType<? extends ThrownPotion> type, Level world) {
        super(type, world);
    }

    public ThrownPotion(Level world, net.minecraft.world.entity.LivingEntity owner) {
        super(EntityType.POTION, owner, world);
    }

    public ThrownPotion(Level world, double x, double y, double z) {
        super(EntityType.POTION, x, y, z, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SPLASH_POTION;
    }

    @Override
    protected float getGravity() {
        return 0.05F;
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (!this.level.isClientSide) {
            ItemStack itemstack = this.getItem();
            Potion potionregistry = PotionUtils.getPotion(itemstack);
            List<MobEffectInstance> list = PotionUtils.getMobEffects(itemstack);
            boolean flag = potionregistry == Potions.WATER && list.isEmpty();
            Direction enumdirection = blockHitResult.getDirection();
            BlockPos blockposition = blockHitResult.getBlockPos();
            BlockPos blockposition1 = blockposition.relative(enumdirection);

            if (flag) {
                this.dowseFire(blockposition1);
                this.dowseFire(blockposition1.relative(enumdirection.getOpposite()));
                Iterator iterator = Direction.Plane.HORIZONTAL.iterator();

                while (iterator.hasNext()) {
                    Direction enumdirection1 = (Direction) iterator.next();

                    this.dowseFire(blockposition1.relative(enumdirection1));
                }
            }

        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level.isClientSide) {
            ItemStack itemstack = this.getItem();
            Potion potionregistry = PotionUtils.getPotion(itemstack);
            List<MobEffectInstance> list = PotionUtils.getMobEffects(itemstack);
            boolean flag = potionregistry == Potions.WATER && list.isEmpty();

            if (flag) {
                this.applyWater();
            } else if (true || !list.isEmpty()) { // CraftBukkit - Call event even if no effects to apply
                if (this.isLingering()) {
                    this.makeAreaOfEffectCloud(itemstack, potionregistry);
                } else {
                    this.applySplash(list, hitResult.getType() == HitResult.Type.ENTITY ? ((EntityHitResult) hitResult).getEntity() : null);
                }
            }

            int i = potionregistry.hasInstantEffects() ? 2007 : 2002;

            this.level.levelEvent(i, this.blockPosition(), PotionUtils.getColor(itemstack));
            this.discard();
        }
    }

    private static final Predicate<net.minecraft.world.entity.LivingEntity> APPLY_WATER_GET_ENTITIES_PREDICATE = ThrownPotion.WATER_SENSITIVE.or(Axolotl.class::isInstance); // Paper
    private void applyWater() {
        AABB axisalignedbb = this.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
        List<net.minecraft.world.entity.LivingEntity> list = this.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, axisalignedbb, ThrownPotion.APPLY_WATER_GET_ENTITIES_PREDICATE); // Paper
        Map<LivingEntity, Double> affected = new HashMap<>(); // Paper

        if (!list.isEmpty()) {
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                net.minecraft.world.entity.LivingEntity entityliving = (net.minecraft.world.entity.LivingEntity) iterator.next();
                // Paper start - Change into single getEntities for axolotls & water sensitive
                if (entityliving instanceof Axolotl axolotl) {
                    affected.put(axolotl.getBukkitLivingEntity(), 1.0);
                    continue;
                }
                // Paper end
                double d0 = this.distanceToSqr((Entity) entityliving);

                if (d0 < 16.0D && entityliving.isSensitiveToWater()) {
                    // Paper start
                    double intensity = 1.0D - Math.sqrt(d0) / 4.0D;
                    affected.put(entityliving.getBukkitLivingEntity(), intensity);
                    // entityliving.hurt(DamageSource.indirectMagic(this, this.getOwner()), 1.0F); // Paper - moved down
                    // Paper end
                }
            }
        }

        // Paper start
        org.bukkit.event.entity.PotionSplashEvent event = CraftEventFactory.callPotionSplashEvent(this, affected);
        if (!event.isCancelled()) {
            for (LivingEntity affectedEntity : event.getAffectedEntities()) {
                net.minecraft.world.entity.LivingEntity entityliving = ((CraftLivingEntity) affectedEntity).getHandle();
                if (entityliving instanceof Axolotl axolotl && event.getIntensity(affectedEntity) > 0) {
                    axolotl.rehydrate();
                } else {
                    entityliving.hurt(DamageSource.indirectMagic(this, this.getOwner()), 1.0F);
                }
            }
            // Paper end
        }

    }

    private void applySplash(List<MobEffectInstance> statusEffects, @Nullable Entity entity) {
        AABB axisalignedbb = this.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
        List<net.minecraft.world.entity.LivingEntity> list1 = this.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, axisalignedbb);
        Map<LivingEntity, Double> affected = new HashMap<LivingEntity, Double>(); // CraftBukkit

        if (!list1.isEmpty()) {
            Entity entity1 = this.getEffectSource();
            Iterator iterator = list1.iterator();

            while (iterator.hasNext()) {
                net.minecraft.world.entity.LivingEntity entityliving = (net.minecraft.world.entity.LivingEntity) iterator.next();

                if (entityliving.isAffectedByPotions()) {
                    double d0 = this.distanceToSqr((Entity) entityliving);

                    if (d0 < 16.0D) {
                        // Paper - diff on change, used when calling the splash event for water splash potions
                        double d1 = 1.0D - Math.sqrt(d0) / 4.0D;

                        if (entityliving == entity) {
                            d1 = 1.0D;
                        }

                        // CraftBukkit start
                        affected.put((LivingEntity) entityliving.getBukkitEntity(), d1);
                    }
                }
            }
        }

        org.bukkit.event.entity.PotionSplashEvent event = org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callPotionSplashEvent(this, affected);
        if (!event.isCancelled() && statusEffects != null && !statusEffects.isEmpty()) { // do not process effects if there are no effects to process
            Entity entity1 = this.getEffectSource();
            for (LivingEntity victim : event.getAffectedEntities()) {
                if (!(victim instanceof CraftLivingEntity)) {
                    continue;
                }

                net.minecraft.world.entity.LivingEntity entityliving = ((CraftLivingEntity) victim).getHandle();
                double d1 = event.getIntensity(victim);
                // CraftBukkit end

                Iterator iterator1 = statusEffects.iterator();

                while (iterator1.hasNext()) {
                    MobEffectInstance mobeffect = (MobEffectInstance) iterator1.next();
                    MobEffect mobeffectlist = mobeffect.getEffect();
                    // CraftBukkit start - Abide by PVP settings - for players only!
                    if (!this.level.pvpMode && this.getOwner() instanceof ServerPlayer && entityliving instanceof ServerPlayer && entityliving != this.getOwner()) {
                        int i = MobEffect.getId(mobeffectlist);
                        // Block SLOWER_MOVEMENT, SLOWER_DIG, HARM, BLINDNESS, HUNGER, WEAKNESS and POISON potions
                        if (i == 2 || i == 4 || i == 7 || i == 15 || i == 17 || i == 18 || i == 19) {
                            continue;
                        }
                    }
                    // CraftBukkit end

                    if (mobeffectlist.isInstantenous()) {
                        mobeffectlist.applyInstantenousEffect(this, this.getOwner(), entityliving, mobeffect.getAmplifier(), d1);
                    } else {
                        int i = (int) (d1 * (double) mobeffect.getDuration() + 0.5D);

                        if (i > 20) {
                            entityliving.addEffect(new MobEffectInstance(mobeffectlist, i, mobeffect.getAmplifier(), mobeffect.isAmbient(), mobeffect.isVisible()), entity1, org.bukkit.event.entity.EntityPotionEffectEvent.Cause.POTION_SPLASH); // CraftBukkit
                        }
                    }
                }
            }
        }

    }

    private void makeAreaOfEffectCloud(ItemStack stack, Potion potion) {
        AreaEffectCloud entityareaeffectcloud = new AreaEffectCloud(this.level, this.getX(), this.getY(), this.getZ());
        Entity entity = this.getOwner();

        if (entity instanceof net.minecraft.world.entity.LivingEntity) {
            entityareaeffectcloud.setOwner((net.minecraft.world.entity.LivingEntity) entity);
        }

        entityareaeffectcloud.setRadius(3.0F);
        entityareaeffectcloud.setRadiusOnUse(-0.5F);
        entityareaeffectcloud.setWaitTime(10);
        entityareaeffectcloud.setRadiusPerTick(-entityareaeffectcloud.getRadius() / (float) entityareaeffectcloud.getDuration());
        entityareaeffectcloud.setPotion(potion);
        Iterator iterator = PotionUtils.getCustomEffects(stack).iterator();

        while (iterator.hasNext()) {
            MobEffectInstance mobeffect = (MobEffectInstance) iterator.next();

            entityareaeffectcloud.addEffect(new MobEffectInstance(mobeffect));
        }

        CompoundTag nbttagcompound = stack.getTag();

        if (nbttagcompound != null && nbttagcompound.contains("CustomPotionColor", 99)) {
            entityareaeffectcloud.setFixedColor(nbttagcompound.getInt("CustomPotionColor"));
        }

        // CraftBukkit start
        org.bukkit.event.entity.LingeringPotionSplashEvent event = org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory.callLingeringPotionSplashEvent(this, entityareaeffectcloud);
        if (!(event.isCancelled() || entityareaeffectcloud.isRemoved())) {
            this.level.addFreshEntity(entityareaeffectcloud);
        } else {
            entityareaeffectcloud.discard();
        }
        // CraftBukkit end
    }

    public boolean isLingering() {
        return this.getItem().is(Items.LINGERING_POTION);
    }

    private void dowseFire(BlockPos pos) {
        BlockState iblockdata = this.level.getBlockState(pos);

        if (iblockdata.is(BlockTags.FIRE)) {
            // CraftBukkit start
            if (!CraftEventFactory.callEntityChangeBlockEvent(this, pos, Blocks.AIR.defaultBlockState()).isCancelled()) {
                this.level.removeBlock(pos, false);
            }
            // CraftBukkit end
        } else if (AbstractCandleBlock.isLit(iblockdata)) {
            // CraftBukkit start
            if (!CraftEventFactory.callEntityChangeBlockEvent(this, pos, iblockdata.setValue(AbstractCandleBlock.LIT, false)).isCancelled()) {
                AbstractCandleBlock.extinguish((Player) null, iblockdata, this.level, pos);
            }
            // CraftBukkit end
        } else if (CampfireBlock.isLitCampfire(iblockdata)) {
            // CraftBukkit start
            if (!CraftEventFactory.callEntityChangeBlockEvent(this, pos, iblockdata.setValue(CampfireBlock.LIT, false)).isCancelled()) {
                this.level.levelEvent((Player) null, 1009, pos, 0);
                CampfireBlock.dowse(this.getOwner(), this.level, pos, iblockdata);
                this.level.setBlockAndUpdate(pos, (BlockState) iblockdata.setValue(CampfireBlock.LIT, false));
            }
            // CraftBukkit end
        }

    }
}
