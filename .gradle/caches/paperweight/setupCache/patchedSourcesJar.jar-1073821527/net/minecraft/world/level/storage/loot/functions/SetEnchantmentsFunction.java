package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class SetEnchantmentsFunction extends LootItemConditionalFunction {
    final Map<Enchantment, NumberProvider> enchantments;
    final boolean add;

    SetEnchantmentsFunction(LootItemCondition[] conditions, Map<Enchantment, NumberProvider> enchantments, boolean add) {
        super(conditions);
        this.enchantments = ImmutableMap.copyOf(enchantments);
        this.add = add;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_ENCHANTMENTS;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.enchantments.values().stream().flatMap((numberProvider) -> {
            return numberProvider.getReferencedContextParams().stream();
        }).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        Object2IntMap<Enchantment> object2IntMap = new Object2IntOpenHashMap<>();
        this.enchantments.forEach((enchantment, numberProvider) -> {
            object2IntMap.put(enchantment, numberProvider.getInt(context));
        });
        if (stack.getItem() == Items.BOOK) {
            ItemStack itemStack = new ItemStack(Items.ENCHANTED_BOOK);
            object2IntMap.forEach((enchantment, level) -> {
                EnchantedBookItem.addEnchantment(itemStack, new EnchantmentInstance(enchantment, level));
            });
            return itemStack;
        } else {
            Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
            if (this.add) {
                object2IntMap.forEach((enchantment, level) -> {
                    updateEnchantment(map, enchantment, Math.max(map.getOrDefault(enchantment, 0) + level, 0));
                });
            } else {
                object2IntMap.forEach((enchantment, level) -> {
                    updateEnchantment(map, enchantment, Math.max(level, 0));
                });
            }

            EnchantmentHelper.setEnchantments(map, stack);
            return stack;
        }
    }

    private static void updateEnchantment(Map<Enchantment, Integer> map, Enchantment enchantment, int level) {
        if (level == 0) {
            map.remove(enchantment);
        } else {
            map.put(enchantment, level);
        }

    }

    public static class Builder extends LootItemConditionalFunction.Builder<SetEnchantmentsFunction.Builder> {
        private final Map<Enchantment, NumberProvider> enchantments = Maps.newHashMap();
        private final boolean add;

        public Builder() {
            this(false);
        }

        public Builder(boolean add) {
            this.add = add;
        }

        @Override
        protected SetEnchantmentsFunction.Builder getThis() {
            return this;
        }

        public SetEnchantmentsFunction.Builder withEnchantment(Enchantment enchantment, NumberProvider level) {
            this.enchantments.put(enchantment, level);
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetEnchantmentsFunction(this.getConditions(), this.enchantments, this.add);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetEnchantmentsFunction> {
        @Override
        public void serialize(JsonObject json, SetEnchantmentsFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            JsonObject jsonObject = new JsonObject();
            object.enchantments.forEach((enchantment, numberProvider) -> {
                ResourceLocation resourceLocation = Registry.ENCHANTMENT.getKey(enchantment);
                if (resourceLocation == null) {
                    throw new IllegalArgumentException("Don't know how to serialize enchantment " + enchantment);
                } else {
                    jsonObject.add(resourceLocation.toString(), context.serialize(numberProvider));
                }
            });
            json.add("enchantments", jsonObject);
            json.addProperty("add", object.add);
        }

        @Override
        public SetEnchantmentsFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            Map<Enchantment, NumberProvider> map = Maps.newHashMap();
            if (jsonObject.has("enchantments")) {
                JsonObject jsonObject2 = GsonHelper.getAsJsonObject(jsonObject, "enchantments");

                for(Entry<String, JsonElement> entry : jsonObject2.entrySet()) {
                    String string = entry.getKey();
                    JsonElement jsonElement = entry.getValue();
                    Enchantment enchantment = Registry.ENCHANTMENT.getOptional(new ResourceLocation(string)).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown enchantment '" + string + "'");
                    });
                    NumberProvider numberProvider = jsonDeserializationContext.deserialize(jsonElement, NumberProvider.class);
                    map.put(enchantment, numberProvider);
                }
            }

            boolean bl = GsonHelper.getAsBoolean(jsonObject, "add", false);
            return new SetEnchantmentsFunction(lootItemConditions, map, bl);
        }
    }
}
