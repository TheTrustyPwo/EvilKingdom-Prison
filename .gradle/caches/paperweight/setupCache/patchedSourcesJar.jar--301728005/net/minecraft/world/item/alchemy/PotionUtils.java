package net.minecraft.world.item.alchemy;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public class PotionUtils {
    public static final String TAG_CUSTOM_POTION_EFFECTS = "CustomPotionEffects";
    public static final String TAG_CUSTOM_POTION_COLOR = "CustomPotionColor";
    public static final String TAG_POTION = "Potion";
    private static final int EMPTY_COLOR = 16253176;
    private static final Component NO_EFFECT = (new TranslatableComponent("effect.none")).withStyle(ChatFormatting.GRAY);

    public static List<MobEffectInstance> getMobEffects(ItemStack stack) {
        return getAllEffects(stack.getTag());
    }

    public static List<MobEffectInstance> getAllEffects(Potion potion, Collection<MobEffectInstance> custom) {
        List<MobEffectInstance> list = Lists.newArrayList();
        list.addAll(potion.getEffects());
        list.addAll(custom);
        return list;
    }

    public static List<MobEffectInstance> getAllEffects(@Nullable CompoundTag nbt) {
        List<MobEffectInstance> list = Lists.newArrayList();
        list.addAll(getPotion(nbt).getEffects());
        getCustomEffects(nbt, list);
        return list;
    }

    public static List<MobEffectInstance> getCustomEffects(ItemStack stack) {
        return getCustomEffects(stack.getTag());
    }

    public static List<MobEffectInstance> getCustomEffects(@Nullable CompoundTag nbt) {
        List<MobEffectInstance> list = Lists.newArrayList();
        getCustomEffects(nbt, list);
        return list;
    }

    public static void getCustomEffects(@Nullable CompoundTag nbt, List<MobEffectInstance> list) {
        if (nbt != null && nbt.contains("CustomPotionEffects", 9)) {
            ListTag listTag = nbt.getList("CustomPotionEffects", 10);

            for(int i = 0; i < listTag.size(); ++i) {
                CompoundTag compoundTag = listTag.getCompound(i);
                MobEffectInstance mobEffectInstance = MobEffectInstance.load(compoundTag);
                if (mobEffectInstance != null) {
                    list.add(mobEffectInstance);
                }
            }
        }

    }

    public static int getColor(ItemStack stack) {
        CompoundTag compoundTag = stack.getTag();
        if (compoundTag != null && compoundTag.contains("CustomPotionColor", 99)) {
            return compoundTag.getInt("CustomPotionColor");
        } else {
            return getPotion(stack) == Potions.EMPTY ? 16253176 : getColor(getMobEffects(stack));
        }
    }

    public static int getColor(Potion potion) {
        return potion == Potions.EMPTY ? 16253176 : getColor(potion.getEffects());
    }

    public static int getColor(Collection<MobEffectInstance> effects) {
        int i = 3694022;
        if (effects.isEmpty()) {
            return 3694022;
        } else {
            float f = 0.0F;
            float g = 0.0F;
            float h = 0.0F;
            int j = 0;

            for(MobEffectInstance mobEffectInstance : effects) {
                if (mobEffectInstance.isVisible()) {
                    int k = mobEffectInstance.getEffect().getColor();
                    int l = mobEffectInstance.getAmplifier() + 1;
                    f += (float)(l * (k >> 16 & 255)) / 255.0F;
                    g += (float)(l * (k >> 8 & 255)) / 255.0F;
                    h += (float)(l * (k >> 0 & 255)) / 255.0F;
                    j += l;
                }
            }

            if (j == 0) {
                return 0;
            } else {
                f = f / (float)j * 255.0F;
                g = g / (float)j * 255.0F;
                h = h / (float)j * 255.0F;
                return (int)f << 16 | (int)g << 8 | (int)h;
            }
        }
    }

    public static Potion getPotion(ItemStack stack) {
        return getPotion(stack.getTag());
    }

    public static Potion getPotion(@Nullable CompoundTag compound) {
        return compound == null ? Potions.EMPTY : Potion.byName(compound.getString("Potion"));
    }

    public static ItemStack setPotion(ItemStack stack, Potion potion) {
        ResourceLocation resourceLocation = Registry.POTION.getKey(potion);
        if (potion == Potions.EMPTY) {
            stack.removeTagKey("Potion");
        } else {
            stack.getOrCreateTag().putString("Potion", resourceLocation.toString());
        }

        return stack;
    }

    public static ItemStack setCustomEffects(ItemStack stack, Collection<MobEffectInstance> effects) {
        if (effects.isEmpty()) {
            return stack;
        } else {
            CompoundTag compoundTag = stack.getOrCreateTag();
            ListTag listTag = compoundTag.getList("CustomPotionEffects", 9);

            for(MobEffectInstance mobEffectInstance : effects) {
                listTag.add(mobEffectInstance.save(new CompoundTag()));
            }

            compoundTag.put("CustomPotionEffects", listTag);
            return stack;
        }
    }

    public static void addPotionTooltip(ItemStack stack, List<Component> list, float durationMultiplier) {
        List<MobEffectInstance> list2 = getMobEffects(stack);
        List<Pair<Attribute, AttributeModifier>> list3 = Lists.newArrayList();
        if (list2.isEmpty()) {
            list.add(NO_EFFECT);
        } else {
            for(MobEffectInstance mobEffectInstance : list2) {
                MutableComponent mutableComponent = new TranslatableComponent(mobEffectInstance.getDescriptionId());
                MobEffect mobEffect = mobEffectInstance.getEffect();
                Map<Attribute, AttributeModifier> map = mobEffect.getAttributeModifiers();
                if (!map.isEmpty()) {
                    for(Entry<Attribute, AttributeModifier> entry : map.entrySet()) {
                        AttributeModifier attributeModifier = entry.getValue();
                        AttributeModifier attributeModifier2 = new AttributeModifier(attributeModifier.getName(), mobEffect.getAttributeModifierValue(mobEffectInstance.getAmplifier(), attributeModifier), attributeModifier.getOperation());
                        list3.add(new Pair<>(entry.getKey(), attributeModifier2));
                    }
                }

                if (mobEffectInstance.getAmplifier() > 0) {
                    mutableComponent = new TranslatableComponent("potion.withAmplifier", mutableComponent, new TranslatableComponent("potion.potency." + mobEffectInstance.getAmplifier()));
                }

                if (mobEffectInstance.getDuration() > 20) {
                    mutableComponent = new TranslatableComponent("potion.withDuration", mutableComponent, MobEffectUtil.formatDuration(mobEffectInstance, durationMultiplier));
                }

                list.add(mutableComponent.withStyle(mobEffect.getCategory().getTooltipFormatting()));
            }
        }

        if (!list3.isEmpty()) {
            list.add(TextComponent.EMPTY);
            list.add((new TranslatableComponent("potion.whenDrank")).withStyle(ChatFormatting.DARK_PURPLE));

            for(Pair<Attribute, AttributeModifier> pair : list3) {
                AttributeModifier attributeModifier3 = pair.getSecond();
                double d = attributeModifier3.getAmount();
                double f;
                if (attributeModifier3.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributeModifier3.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
                    f = attributeModifier3.getAmount();
                } else {
                    f = attributeModifier3.getAmount() * 100.0D;
                }

                if (d > 0.0D) {
                    list.add((new TranslatableComponent("attribute.modifier.plus." + attributeModifier3.getOperation().toValue(), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(f), new TranslatableComponent(pair.getFirst().getDescriptionId()))).withStyle(ChatFormatting.BLUE));
                } else if (d < 0.0D) {
                    f *= -1.0D;
                    list.add((new TranslatableComponent("attribute.modifier.take." + attributeModifier3.getOperation().toValue(), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(f), new TranslatableComponent(pair.getFirst().getDescriptionId()))).withStyle(ChatFormatting.RED));
                }
            }
        }

    }
}
