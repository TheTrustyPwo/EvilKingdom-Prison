package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class EnchantRandomlyFunction extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    final List<Enchantment> enchantments;

    EnchantRandomlyFunction(LootItemCondition[] conditions, Collection<Enchantment> enchantments) {
        super(conditions);
        this.enchantments = ImmutableList.copyOf(enchantments);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.ENCHANT_RANDOMLY;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        Random random = context.getRandom();
        Enchantment enchantment;
        if (this.enchantments.isEmpty()) {
            boolean bl = stack.is(Items.BOOK);
            List<Enchantment> list = Registry.ENCHANTMENT.stream().filter(Enchantment::isDiscoverable).filter((enchantment) -> {
                return bl || enchantment.canEnchant(stack);
            }).collect(Collectors.toList());
            if (list.isEmpty()) {
                LOGGER.warn("Couldn't find a compatible enchantment for {}", (Object)stack);
                return stack;
            }

            enchantment = list.get(random.nextInt(list.size()));
        } else {
            enchantment = this.enchantments.get(random.nextInt(this.enchantments.size()));
        }

        return enchantItem(stack, enchantment, random);
    }

    private static ItemStack enchantItem(ItemStack stack, Enchantment enchantment, Random random) {
        int i = Mth.nextInt(random, enchantment.getMinLevel(), enchantment.getMaxLevel());
        if (stack.is(Items.BOOK)) {
            stack = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchantment, i));
        } else {
            stack.enchant(enchantment, i);
        }

        return stack;
    }

    public static EnchantRandomlyFunction.Builder randomEnchantment() {
        return new EnchantRandomlyFunction.Builder();
    }

    public static LootItemConditionalFunction.Builder<?> randomApplicableEnchantment() {
        return simpleBuilder((conditions) -> {
            return new EnchantRandomlyFunction(conditions, ImmutableList.of());
        });
    }

    public static class Builder extends LootItemConditionalFunction.Builder<EnchantRandomlyFunction.Builder> {
        private final Set<Enchantment> enchantments = Sets.newHashSet();

        @Override
        protected EnchantRandomlyFunction.Builder getThis() {
            return this;
        }

        public EnchantRandomlyFunction.Builder withEnchantment(Enchantment enchantment) {
            this.enchantments.add(enchantment);
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new EnchantRandomlyFunction(this.getConditions(), this.enchantments);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<EnchantRandomlyFunction> {
        @Override
        public void serialize(JsonObject json, EnchantRandomlyFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            if (!object.enchantments.isEmpty()) {
                JsonArray jsonArray = new JsonArray();

                for(Enchantment enchantment : object.enchantments) {
                    ResourceLocation resourceLocation = Registry.ENCHANTMENT.getKey(enchantment);
                    if (resourceLocation == null) {
                        throw new IllegalArgumentException("Don't know how to serialize enchantment " + enchantment);
                    }

                    jsonArray.add(new JsonPrimitive(resourceLocation.toString()));
                }

                json.add("enchantments", jsonArray);
            }

        }

        @Override
        public EnchantRandomlyFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            List<Enchantment> list = Lists.newArrayList();
            if (jsonObject.has("enchantments")) {
                for(JsonElement jsonElement : GsonHelper.getAsJsonArray(jsonObject, "enchantments")) {
                    String string = GsonHelper.convertToString(jsonElement, "enchantment");
                    Enchantment enchantment = Registry.ENCHANTMENT.getOptional(new ResourceLocation(string)).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown enchantment '" + string + "'");
                    });
                    list.add(enchantment);
                }
            }

            return new EnchantRandomlyFunction(lootItemConditions, list);
        }
    }
}
