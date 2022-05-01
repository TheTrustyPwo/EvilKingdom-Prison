package net.minecraft.world.item;

import com.google.common.collect.Lists;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CrossbowItem extends ProjectileWeaponItem implements Vanishable {
    private static final String TAG_CHARGED = "Charged";
    private static final String TAG_CHARGED_PROJECTILES = "ChargedProjectiles";
    private static final int MAX_CHARGE_DURATION = 25;
    public static final int DEFAULT_RANGE = 8;
    private boolean startSoundPlayed = false;
    private boolean midLoadSoundPlayed = false;
    private static final float START_SOUND_PERCENT = 0.2F;
    private static final float MID_SOUND_PERCENT = 0.5F;
    private static final float ARROW_POWER = 3.15F;
    private static final float FIREWORK_POWER = 1.6F;

    public CrossbowItem(Item.Properties settings) {
        super(settings);
    }

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return ARROW_OR_FIREWORK;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return ARROW_ONLY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (isCharged(itemStack)) {
            performShooting(world, user, hand, itemStack, getShootingPower(itemStack), 1.0F);
            setCharged(itemStack, false);
            return InteractionResultHolder.consume(itemStack);
        } else if (!user.getProjectile(itemStack).isEmpty()) {
            if (!isCharged(itemStack)) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
                user.startUsingItem(hand);
            }

            return InteractionResultHolder.consume(itemStack);
        } else {
            return InteractionResultHolder.fail(itemStack);
        }
    }

    private static float getShootingPower(ItemStack stack) {
        return containsChargedProjectile(stack, Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
        int i = this.getUseDuration(stack) - remainingUseTicks;
        float f = getPowerForTime(i, stack);
        if (f >= 1.0F && !isCharged(stack) && tryLoadProjectiles(user, stack)) {
            setCharged(stack, true);
            SoundSource soundSource = user instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
            world.playSound((Player)null, user.getX(), user.getY(), user.getZ(), SoundEvents.CROSSBOW_LOADING_END, soundSource, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
        }

    }

    private static boolean tryLoadProjectiles(LivingEntity shooter, ItemStack projectile) {
        int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, projectile);
        int j = i == 0 ? 1 : 3;
        boolean bl = shooter instanceof Player && ((Player)shooter).getAbilities().instabuild;
        ItemStack itemStack = shooter.getProjectile(projectile);
        ItemStack itemStack2 = itemStack.copy();

        for(int k = 0; k < j; ++k) {
            if (k > 0) {
                itemStack = itemStack2.copy();
            }

            if (itemStack.isEmpty() && bl) {
                itemStack = new ItemStack(Items.ARROW);
                itemStack2 = itemStack.copy();
            }

            if (!loadProjectile(shooter, projectile, itemStack, k > 0, bl)) {
                return false;
            }
        }

        return true;
    }

    private static boolean loadProjectile(LivingEntity shooter, ItemStack crossbow, ItemStack projectile, boolean simulated, boolean creative) {
        if (projectile.isEmpty()) {
            return false;
        } else {
            boolean bl = creative && projectile.getItem() instanceof ArrowItem;
            ItemStack itemStack;
            if (!bl && !creative && !simulated) {
                itemStack = projectile.split(1);
                if (projectile.isEmpty() && shooter instanceof Player) {
                    ((Player)shooter).getInventory().removeItem(projectile);
                }
            } else {
                itemStack = projectile.copy();
            }

            addChargedProjectile(crossbow, itemStack);
            return true;
        }
    }

    public static boolean isCharged(ItemStack stack) {
        CompoundTag compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.getBoolean("Charged");
    }

    public static void setCharged(ItemStack stack, boolean charged) {
        CompoundTag compoundTag = stack.getOrCreateTag();
        compoundTag.putBoolean("Charged", charged);
    }

    private static void addChargedProjectile(ItemStack crossbow, ItemStack projectile) {
        CompoundTag compoundTag = crossbow.getOrCreateTag();
        ListTag listTag;
        if (compoundTag.contains("ChargedProjectiles", 9)) {
            listTag = compoundTag.getList("ChargedProjectiles", 10);
        } else {
            listTag = new ListTag();
        }

        CompoundTag compoundTag2 = new CompoundTag();
        projectile.save(compoundTag2);
        listTag.add(compoundTag2);
        compoundTag.put("ChargedProjectiles", listTag);
    }

    private static List<ItemStack> getChargedProjectiles(ItemStack crossbow) {
        List<ItemStack> list = Lists.newArrayList();
        CompoundTag compoundTag = crossbow.getTag();
        if (compoundTag != null && compoundTag.contains("ChargedProjectiles", 9)) {
            ListTag listTag = compoundTag.getList("ChargedProjectiles", 10);
            if (listTag != null) {
                for(int i = 0; i < listTag.size(); ++i) {
                    CompoundTag compoundTag2 = listTag.getCompound(i);
                    list.add(ItemStack.of(compoundTag2));
                }
            }
        }

        return list;
    }

    private static void clearChargedProjectiles(ItemStack crossbow) {
        CompoundTag compoundTag = crossbow.getTag();
        if (compoundTag != null) {
            ListTag listTag = compoundTag.getList("ChargedProjectiles", 9);
            listTag.clear();
            compoundTag.put("ChargedProjectiles", listTag);
        }

    }

    public static boolean containsChargedProjectile(ItemStack crossbow, Item projectile) {
        return getChargedProjectiles(crossbow).stream().anyMatch((s) -> {
            return s.is(projectile);
        });
    }

    private static void shootProjectile(Level world, LivingEntity shooter, InteractionHand hand, ItemStack crossbow, ItemStack projectile, float soundPitch, boolean creative, float speed, float divergence, float simulated) {
        if (!world.isClientSide) {
            boolean bl = projectile.is(Items.FIREWORK_ROCKET);
            Projectile projectile2;
            if (bl) {
                projectile2 = new FireworkRocketEntity(world, projectile, shooter, shooter.getX(), shooter.getEyeY() - (double)0.15F, shooter.getZ(), true);
            } else {
                projectile2 = getArrow(world, shooter, crossbow, projectile);
                if (creative || simulated != 0.0F) {
                    ((AbstractArrow)projectile2).pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }
            }

            if (shooter instanceof CrossbowAttackMob) {
                CrossbowAttackMob crossbowAttackMob = (CrossbowAttackMob)shooter;
                crossbowAttackMob.shootCrossbowProjectile(crossbowAttackMob.getTarget(), crossbow, projectile2, simulated);
            } else {
                Vec3 vec3 = shooter.getUpVector(1.0F);
                Quaternion quaternion = new Quaternion(new Vector3f(vec3), simulated, true);
                Vec3 vec32 = shooter.getViewVector(1.0F);
                Vector3f vector3f = new Vector3f(vec32);
                vector3f.transform(quaternion);
                projectile2.shoot((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z(), speed, divergence);
            }

            crossbow.hurtAndBreak(bl ? 3 : 1, shooter, (e) -> {
                e.broadcastBreakEvent(hand);
            });
            world.addFreshEntity(projectile2);
            world.playSound((Player)null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, soundPitch);
        }
    }

    private static AbstractArrow getArrow(Level world, LivingEntity entity, ItemStack crossbow, ItemStack arrow) {
        ArrowItem arrowItem = (ArrowItem)(arrow.getItem() instanceof ArrowItem ? arrow.getItem() : Items.ARROW);
        AbstractArrow abstractArrow = arrowItem.createArrow(world, arrow, entity);
        if (entity instanceof Player) {
            abstractArrow.setCritArrow(true);
        }

        abstractArrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
        abstractArrow.setShotFromCrossbow(true);
        int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, crossbow);
        if (i > 0) {
            abstractArrow.setPierceLevel((byte)i);
        }

        return abstractArrow;
    }

    public static void performShooting(Level world, LivingEntity entity, InteractionHand hand, ItemStack stack, float speed, float divergence) {
        List<ItemStack> list = getChargedProjectiles(stack);
        float[] fs = getShotPitches(entity.getRandom());

        for(int i = 0; i < list.size(); ++i) {
            ItemStack itemStack = list.get(i);
            boolean bl = entity instanceof Player && ((Player)entity).getAbilities().instabuild;
            if (!itemStack.isEmpty()) {
                if (i == 0) {
                    shootProjectile(world, entity, hand, stack, itemStack, fs[i], bl, speed, divergence, 0.0F);
                } else if (i == 1) {
                    shootProjectile(world, entity, hand, stack, itemStack, fs[i], bl, speed, divergence, -10.0F);
                } else if (i == 2) {
                    shootProjectile(world, entity, hand, stack, itemStack, fs[i], bl, speed, divergence, 10.0F);
                }
            }
        }

        onCrossbowShot(world, entity, stack);
    }

    private static float[] getShotPitches(Random random) {
        boolean bl = random.nextBoolean();
        return new float[]{1.0F, getRandomShotPitch(bl, random), getRandomShotPitch(!bl, random)};
    }

    private static float getRandomShotPitch(boolean flag, Random random) {
        float f = flag ? 0.63F : 0.43F;
        return 1.0F / (random.nextFloat() * 0.5F + 1.8F) + f;
    }

    private static void onCrossbowShot(Level world, LivingEntity entity, ItemStack stack) {
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            if (!world.isClientSide) {
                CriteriaTriggers.SHOT_CROSSBOW.trigger(serverPlayer, stack);
            }

            serverPlayer.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        }

        clearChargedProjectiles(stack);
    }

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClientSide) {
            int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
            SoundEvent soundEvent = this.getStartSound(i);
            SoundEvent soundEvent2 = i == 0 ? SoundEvents.CROSSBOW_LOADING_MIDDLE : null;
            float f = (float)(stack.getUseDuration() - remainingUseTicks) / (float)getChargeDuration(stack);
            if (f < 0.2F) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
            }

            if (f >= 0.2F && !this.startSoundPlayed) {
                this.startSoundPlayed = true;
                world.playSound((Player)null, user.getX(), user.getY(), user.getZ(), soundEvent, SoundSource.PLAYERS, 0.5F, 1.0F);
            }

            if (f >= 0.5F && soundEvent2 != null && !this.midLoadSoundPlayed) {
                this.midLoadSoundPlayed = true;
                world.playSound((Player)null, user.getX(), user.getY(), user.getZ(), soundEvent2, SoundSource.PLAYERS, 0.5F, 1.0F);
            }
        }

    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return getChargeDuration(stack) + 3;
    }

    public static int getChargeDuration(ItemStack stack) {
        int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
        return i == 0 ? 25 : 25 - 5 * i;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CROSSBOW;
    }

    private SoundEvent getStartSound(int stage) {
        switch(stage) {
        case 1:
            return SoundEvents.CROSSBOW_QUICK_CHARGE_1;
        case 2:
            return SoundEvents.CROSSBOW_QUICK_CHARGE_2;
        case 3:
            return SoundEvents.CROSSBOW_QUICK_CHARGE_3;
        default:
            return SoundEvents.CROSSBOW_LOADING_START;
        }
    }

    private static float getPowerForTime(int useTicks, ItemStack stack) {
        float f = (float)useTicks / (float)getChargeDuration(stack);
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        List<ItemStack> list = getChargedProjectiles(stack);
        if (isCharged(stack) && !list.isEmpty()) {
            ItemStack itemStack = list.get(0);
            tooltip.add((new TranslatableComponent("item.minecraft.crossbow.projectile")).append(" ").append(itemStack.getDisplayName()));
            if (context.isAdvanced() && itemStack.is(Items.FIREWORK_ROCKET)) {
                List<Component> list2 = Lists.newArrayList();
                Items.FIREWORK_ROCKET.appendHoverText(itemStack, world, list2, context);
                if (!list2.isEmpty()) {
                    for(int i = 0; i < list2.size(); ++i) {
                        list2.set(i, (new TextComponent("  ")).append(list2.get(i)).withStyle(ChatFormatting.GRAY));
                    }

                    tooltip.addAll(list2);
                }
            }

        }
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {
        return stack.is(this);
    }

    @Override
    public int getDefaultProjectileRange() {
        return 8;
    }
}
