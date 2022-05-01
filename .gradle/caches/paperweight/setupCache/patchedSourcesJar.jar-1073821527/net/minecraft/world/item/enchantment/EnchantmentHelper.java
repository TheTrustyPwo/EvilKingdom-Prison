package net.minecraft.world.item.enchantment;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;

public class EnchantmentHelper {
    private static final String TAG_ENCH_ID = "id";
    private static final String TAG_ENCH_LEVEL = "lvl";

    public static CompoundTag storeEnchantment(@Nullable ResourceLocation id, int lvl) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("id", String.valueOf((Object)id));
        compoundTag.putShort("lvl", (short)lvl);
        return compoundTag;
    }

    public static void setEnchantmentLevel(CompoundTag nbt, int lvl) {
        nbt.putShort("lvl", (short)lvl);
    }

    public static int getEnchantmentLevel(CompoundTag nbt) {
        return Mth.clamp(nbt.getInt("lvl"), 0, 255);
    }

    @Nullable
    public static ResourceLocation getEnchantmentId(CompoundTag nbt) {
        return ResourceLocation.tryParse(nbt.getString("id"));
    }

    @Nullable
    public static ResourceLocation getEnchantmentId(Enchantment enchantment) {
        return Registry.ENCHANTMENT.getKey(enchantment);
    }

    public static int getItemEnchantmentLevel(Enchantment enchantment, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        } else {
            ResourceLocation resourceLocation = getEnchantmentId(enchantment);
            ListTag listTag = stack.getEnchantmentTags();

            for(int i = 0; i < listTag.size(); ++i) {
                CompoundTag compoundTag = listTag.getCompound(i);
                ResourceLocation resourceLocation2 = getEnchantmentId(compoundTag);
                if (resourceLocation2 != null && resourceLocation2.equals(resourceLocation)) {
                    return getEnchantmentLevel(compoundTag);
                }
            }

            return 0;
        }
    }

    public static Map<Enchantment, Integer> getEnchantments(ItemStack stack) {
        ListTag listTag = stack.is(Items.ENCHANTED_BOOK) ? EnchantedBookItem.getEnchantments(stack) : stack.getEnchantmentTags();
        return deserializeEnchantments(listTag);
    }

    public static Map<Enchantment, Integer> deserializeEnchantments(ListTag list) {
        Map<Enchantment, Integer> map = Maps.newLinkedHashMap();

        for(int i = 0; i < list.size(); ++i) {
            CompoundTag compoundTag = list.getCompound(i);
            Registry.ENCHANTMENT.getOptional(getEnchantmentId(compoundTag)).ifPresent((enchantment) -> {
                map.put(enchantment, getEnchantmentLevel(compoundTag));
            });
        }

        return map;
    }

    public static void setEnchantments(Map<Enchantment, Integer> enchantments, ItemStack stack) {
        ListTag listTag = new ListTag();

        for(Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment != null) {
                int i = entry.getValue();
                listTag.add(storeEnchantment(getEnchantmentId(enchantment), i));
                if (stack.is(Items.ENCHANTED_BOOK)) {
                    EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchantment, i));
                }
            }
        }

        if (listTag.isEmpty()) {
            stack.removeTagKey("Enchantments");
        } else if (!stack.is(Items.ENCHANTED_BOOK)) {
            stack.addTagElement("Enchantments", listTag);
        }

    }

    private static void runIterationOnItem(EnchantmentHelper.EnchantmentVisitor consumer, ItemStack stack) {
        if (!stack.isEmpty()) {
            ListTag listTag = stack.getEnchantmentTags();

            for(int i = 0; i < listTag.size(); ++i) {
                CompoundTag compoundTag = listTag.getCompound(i);
                Registry.ENCHANTMENT.getOptional(getEnchantmentId(compoundTag)).ifPresent((enchantment) -> {
                    consumer.accept(enchantment, getEnchantmentLevel(compoundTag));
                });
            }

        }
    }

    private static void runIterationOnInventory(EnchantmentHelper.EnchantmentVisitor consumer, Iterable<ItemStack> stacks) {
        for(ItemStack itemStack : stacks) {
            runIterationOnItem(consumer, itemStack);
        }

    }

    public static int getDamageProtection(Iterable<ItemStack> equipment, DamageSource source) {
        MutableInt mutableInt = new MutableInt();
        runIterationOnInventory((enchantment, level) -> {
            mutableInt.add(enchantment.getDamageProtection(level, source));
        }, equipment);
        return mutableInt.intValue();
    }

    public static float getDamageBonus(ItemStack stack, MobType group) {
        MutableFloat mutableFloat = new MutableFloat();
        runIterationOnItem((enchantment, level) -> {
            mutableFloat.add(enchantment.getDamageBonus(level, group));
        }, stack);
        return mutableFloat.floatValue();
    }

    public static float getSweepingDamageRatio(LivingEntity entity) {
        int i = getEnchantmentLevel(Enchantments.SWEEPING_EDGE, entity);
        return i > 0 ? SweepingEdgeEnchantment.getSweepingDamageRatio(i) : 0.0F;
    }

    public static void doPostHurtEffects(LivingEntity user, Entity attacker) {
        EnchantmentHelper.EnchantmentVisitor enchantmentVisitor = (enchantment, level) -> {
            enchantment.doPostHurt(user, attacker, level);
        };
        if (user != null) {
            runIterationOnInventory(enchantmentVisitor, user.getAllSlots());
        }

        if (attacker instanceof Player) {
            runIterationOnItem(enchantmentVisitor, user.getMainHandItem());
        }

    }

    public static void doPostDamageEffects(LivingEntity user, Entity target) {
        EnchantmentHelper.EnchantmentVisitor enchantmentVisitor = (enchantment, level) -> {
            enchantment.doPostAttack(user, target, level);
        };
        if (user != null) {
            runIterationOnInventory(enchantmentVisitor, user.getAllSlots());
        }

        if (user instanceof Player) {
            runIterationOnItem(enchantmentVisitor, user.getMainHandItem());
        }

    }

    public static int getEnchantmentLevel(Enchantment enchantment, LivingEntity entity) {
        Iterable<ItemStack> iterable = enchantment.getSlotItems(entity).values();
        if (iterable == null) {
            return 0;
        } else {
            int i = 0;

            for(ItemStack itemStack : iterable) {
                int j = getItemEnchantmentLevel(enchantment, itemStack);
                if (j > i) {
                    i = j;
                }
            }

            return i;
        }
    }

    public static int getKnockbackBonus(LivingEntity entity) {
        return getEnchantmentLevel(Enchantments.KNOCKBACK, entity);
    }

    public static int getFireAspect(LivingEntity entity) {
        return getEnchantmentLevel(Enchantments.FIRE_ASPECT, entity);
    }

    public static int getRespiration(LivingEntity entity) {
        return getEnchantmentLevel(Enchantments.RESPIRATION, entity);
    }

    public static int getDepthStrider(LivingEntity entity) {
        return getEnchantmentLevel(Enchantments.DEPTH_STRIDER, entity);
    }

    public static int getBlockEfficiency(LivingEntity entity) {
        return getEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, entity);
    }

    public static int getFishingLuckBonus(ItemStack stack) {
        return getItemEnchantmentLevel(Enchantments.FISHING_LUCK, stack);
    }

    public static int getFishingSpeedBonus(ItemStack stack) {
        return getItemEnchantmentLevel(Enchantments.FISHING_SPEED, stack);
    }

    public static int getMobLooting(LivingEntity entity) {
        return getEnchantmentLevel(Enchantments.MOB_LOOTING, entity);
    }

    public static boolean hasAquaAffinity(LivingEntity entity) {
        return getEnchantmentLevel(Enchantments.AQUA_AFFINITY, entity) > 0;
    }

    public static boolean hasFrostWalker(LivingEntity entity) {
        return getEnchantmentLevel(Enchantments.FROST_WALKER, entity) > 0;
    }

    public static boolean hasSoulSpeed(LivingEntity entity) {
        return getEnchantmentLevel(Enchantments.SOUL_SPEED, entity) > 0;
    }

    public static boolean hasBindingCurse(ItemStack stack) {
        return getItemEnchantmentLevel(Enchantments.BINDING_CURSE, stack) > 0;
    }

    public static boolean hasVanishingCurse(ItemStack stack) {
        return getItemEnchantmentLevel(Enchantments.VANISHING_CURSE, stack) > 0;
    }

    public static int getLoyalty(ItemStack stack) {
        return getItemEnchantmentLevel(Enchantments.LOYALTY, stack);
    }

    public static int getRiptide(ItemStack stack) {
        return getItemEnchantmentLevel(Enchantments.RIPTIDE, stack);
    }

    public static boolean hasChanneling(ItemStack stack) {
        return getItemEnchantmentLevel(Enchantments.CHANNELING, stack) > 0;
    }

    @Nullable
    public static Entry<EquipmentSlot, ItemStack> getRandomItemWith(Enchantment enchantment, LivingEntity entity) {
        return getRandomItemWith(enchantment, entity, (stack) -> {
            return true;
        });
    }

    @Nullable
    public static Entry<EquipmentSlot, ItemStack> getRandomItemWith(Enchantment enchantment, LivingEntity entity, Predicate<ItemStack> condition) {
        Map<EquipmentSlot, ItemStack> map = enchantment.getSlotItems(entity);
        if (map.isEmpty()) {
            return null;
        } else {
            List<Entry<EquipmentSlot, ItemStack>> list = Lists.newArrayList();

            for(Entry<EquipmentSlot, ItemStack> entry : map.entrySet()) {
                ItemStack itemStack = entry.getValue();
                if (!itemStack.isEmpty() && getItemEnchantmentLevel(enchantment, itemStack) > 0 && condition.test(itemStack)) {
                    list.add(entry);
                }
            }

            return list.isEmpty() ? null : list.get(entity.getRandom().nextInt(list.size()));
        }
    }

    public static int getEnchantmentCost(Random random, int slotIndex, int bookshelfCount, ItemStack stack) {
        Item item = stack.getItem();
        int i = item.getEnchantmentValue();
        if (i <= 0) {
            return 0;
        } else {
            if (bookshelfCount > 15) {
                bookshelfCount = 15;
            }

            int j = random.nextInt(8) + 1 + (bookshelfCount >> 1) + random.nextInt(bookshelfCount + 1);
            if (slotIndex == 0) {
                return Math.max(j / 3, 1);
            } else {
                return slotIndex == 1 ? j * 2 / 3 + 1 : Math.max(j, bookshelfCount * 2);
            }
        }
    }

    public static ItemStack enchantItem(Random random, ItemStack target, int level, boolean treasureAllowed) {
        List<EnchantmentInstance> list = selectEnchantment(random, target, level, treasureAllowed);
        boolean bl = target.is(Items.BOOK);
        if (bl) {
            target = new ItemStack(Items.ENCHANTED_BOOK);
        }

        for(EnchantmentInstance enchantmentInstance : list) {
            if (bl) {
                EnchantedBookItem.addEnchantment(target, enchantmentInstance);
            } else {
                target.enchant(enchantmentInstance.enchantment, enchantmentInstance.level);
            }
        }

        return target;
    }

    public static List<EnchantmentInstance> selectEnchantment(Random random, ItemStack stack, int level, boolean treasureAllowed) {
        List<EnchantmentInstance> list = Lists.newArrayList();
        Item item = stack.getItem();
        int i = item.getEnchantmentValue();
        if (i <= 0) {
            return list;
        } else {
            level += 1 + random.nextInt(i / 4 + 1) + random.nextInt(i / 4 + 1);
            float f = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
            level = Mth.clamp(Math.round((float)level + (float)level * f), 1, Integer.MAX_VALUE);
            List<EnchantmentInstance> list2 = getAvailableEnchantmentResults(level, stack, treasureAllowed);
            if (!list2.isEmpty()) {
                WeightedRandom.getRandomItem(random, list2).ifPresent(list::add);

                while(random.nextInt(50) <= level) {
                    if (!list.isEmpty()) {
                        filterCompatibleEnchantments(list2, Util.lastOf(list));
                    }

                    if (list2.isEmpty()) {
                        break;
                    }

                    WeightedRandom.getRandomItem(random, list2).ifPresent(list::add);
                    level /= 2;
                }
            }

            return list;
        }
    }

    public static void filterCompatibleEnchantments(List<EnchantmentInstance> possibleEntries, EnchantmentInstance pickedEntry) {
        Iterator<EnchantmentInstance> iterator = possibleEntries.iterator();

        while(iterator.hasNext()) {
            if (!pickedEntry.enchantment.isCompatibleWith((iterator.next()).enchantment)) {
                iterator.remove();
            }
        }

    }

    public static boolean isEnchantmentCompatible(Collection<Enchantment> existing, Enchantment candidate) {
        for(Enchantment enchantment : existing) {
            if (!enchantment.isCompatibleWith(candidate)) {
                return false;
            }
        }

        return true;
    }

    public static List<EnchantmentInstance> getAvailableEnchantmentResults(int power, ItemStack stack, boolean treasureAllowed) {
        List<EnchantmentInstance> list = Lists.newArrayList();
        Item item = stack.getItem();
        boolean bl = stack.is(Items.BOOK);

        for(Enchantment enchantment : Registry.ENCHANTMENT) {
            if ((!enchantment.isTreasureOnly() || treasureAllowed) && enchantment.isDiscoverable() && (enchantment.category.canEnchant(item) || bl)) {
                for(int i = enchantment.getMaxLevel(); i > enchantment.getMinLevel() - 1; --i) {
                    if (power >= enchantment.getMinCost(i) && power <= enchantment.getMaxCost(i)) {
                        list.add(new EnchantmentInstance(enchantment, i));
                        break;
                    }
                }
            }
        }

        return list;
    }

    @FunctionalInterface
    interface EnchantmentVisitor {
        void accept(Enchantment enchantment, int level);
    }
}
