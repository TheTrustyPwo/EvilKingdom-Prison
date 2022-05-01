package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ApplyBonusCount extends LootItemConditionalFunction {
    static final Map<ResourceLocation, ApplyBonusCount.FormulaDeserializer> FORMULAS = Maps.newHashMap();
    final Enchantment enchantment;
    final ApplyBonusCount.Formula formula;

    ApplyBonusCount(LootItemCondition[] conditions, Enchantment enchantment, ApplyBonusCount.Formula formula) {
        super(conditions);
        this.enchantment = enchantment;
        this.formula = formula;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.APPLY_BONUS;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.TOOL);
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        ItemStack itemStack = context.getParamOrNull(LootContextParams.TOOL);
        if (itemStack != null) {
            int i = EnchantmentHelper.getItemEnchantmentLevel(this.enchantment, itemStack);
            int j = this.formula.calculateNewCount(context.getRandom(), stack.getCount(), i);
            stack.setCount(j);
        }

        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> addBonusBinomialDistributionCount(Enchantment enchantment, float probability, int extra) {
        return simpleBuilder((conditions) -> {
            return new ApplyBonusCount(conditions, enchantment, new ApplyBonusCount.BinomialWithBonusCount(extra, probability));
        });
    }

    public static LootItemConditionalFunction.Builder<?> addOreBonusCount(Enchantment enchantment) {
        return simpleBuilder((conditions) -> {
            return new ApplyBonusCount(conditions, enchantment, new ApplyBonusCount.OreDrops());
        });
    }

    public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Enchantment enchantment) {
        return simpleBuilder((conditions) -> {
            return new ApplyBonusCount(conditions, enchantment, new ApplyBonusCount.UniformBonusCount(1));
        });
    }

    public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Enchantment enchantment, int bonusMultiplier) {
        return simpleBuilder((conditions) -> {
            return new ApplyBonusCount(conditions, enchantment, new ApplyBonusCount.UniformBonusCount(bonusMultiplier));
        });
    }

    static {
        FORMULAS.put(ApplyBonusCount.BinomialWithBonusCount.TYPE, ApplyBonusCount.BinomialWithBonusCount::deserialize);
        FORMULAS.put(ApplyBonusCount.OreDrops.TYPE, ApplyBonusCount.OreDrops::deserialize);
        FORMULAS.put(ApplyBonusCount.UniformBonusCount.TYPE, ApplyBonusCount.UniformBonusCount::deserialize);
    }

    static final class BinomialWithBonusCount implements ApplyBonusCount.Formula {
        public static final ResourceLocation TYPE = new ResourceLocation("binomial_with_bonus_count");
        private final int extraRounds;
        private final float probability;

        public BinomialWithBonusCount(int extra, float probability) {
            this.extraRounds = extra;
            this.probability = probability;
        }

        @Override
        public int calculateNewCount(Random random, int initialCount, int enchantmentLevel) {
            for(int i = 0; i < enchantmentLevel + this.extraRounds; ++i) {
                if (random.nextFloat() < this.probability) {
                    ++initialCount;
                }
            }

            return initialCount;
        }

        @Override
        public void serializeParams(JsonObject json, JsonSerializationContext context) {
            json.addProperty("extra", this.extraRounds);
            json.addProperty("probability", this.probability);
        }

        public static ApplyBonusCount.Formula deserialize(JsonObject json, JsonDeserializationContext context) {
            int i = GsonHelper.getAsInt(json, "extra");
            float f = GsonHelper.getAsFloat(json, "probability");
            return new ApplyBonusCount.BinomialWithBonusCount(i, f);
        }

        @Override
        public ResourceLocation getType() {
            return TYPE;
        }
    }

    interface Formula {
        int calculateNewCount(Random random, int initialCount, int enchantmentLevel);

        void serializeParams(JsonObject json, JsonSerializationContext context);

        ResourceLocation getType();
    }

    interface FormulaDeserializer {
        ApplyBonusCount.Formula deserialize(JsonObject functionJson, JsonDeserializationContext context);
    }

    static final class OreDrops implements ApplyBonusCount.Formula {
        public static final ResourceLocation TYPE = new ResourceLocation("ore_drops");

        @Override
        public int calculateNewCount(Random random, int initialCount, int enchantmentLevel) {
            if (enchantmentLevel > 0) {
                int i = random.nextInt(enchantmentLevel + 2) - 1;
                if (i < 0) {
                    i = 0;
                }

                return initialCount * (i + 1);
            } else {
                return initialCount;
            }
        }

        @Override
        public void serializeParams(JsonObject json, JsonSerializationContext context) {
        }

        public static ApplyBonusCount.Formula deserialize(JsonObject json, JsonDeserializationContext context) {
            return new ApplyBonusCount.OreDrops();
        }

        @Override
        public ResourceLocation getType() {
            return TYPE;
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<ApplyBonusCount> {
        @Override
        public void serialize(JsonObject json, ApplyBonusCount object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("enchantment", Registry.ENCHANTMENT.getKey(object.enchantment).toString());
            json.addProperty("formula", object.formula.getType().toString());
            JsonObject jsonObject = new JsonObject();
            object.formula.serializeParams(jsonObject, context);
            if (jsonObject.size() > 0) {
                json.add("parameters", jsonObject);
            }

        }

        @Override
        public ApplyBonusCount deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "enchantment"));
            Enchantment enchantment = Registry.ENCHANTMENT.getOptional(resourceLocation).orElseThrow(() -> {
                return new JsonParseException("Invalid enchantment id: " + resourceLocation);
            });
            ResourceLocation resourceLocation2 = new ResourceLocation(GsonHelper.getAsString(jsonObject, "formula"));
            ApplyBonusCount.FormulaDeserializer formulaDeserializer = ApplyBonusCount.FORMULAS.get(resourceLocation2);
            if (formulaDeserializer == null) {
                throw new JsonParseException("Invalid formula id: " + resourceLocation2);
            } else {
                ApplyBonusCount.Formula formula;
                if (jsonObject.has("parameters")) {
                    formula = formulaDeserializer.deserialize(GsonHelper.getAsJsonObject(jsonObject, "parameters"), jsonDeserializationContext);
                } else {
                    formula = formulaDeserializer.deserialize(new JsonObject(), jsonDeserializationContext);
                }

                return new ApplyBonusCount(lootItemConditions, enchantment, formula);
            }
        }
    }

    static final class UniformBonusCount implements ApplyBonusCount.Formula {
        public static final ResourceLocation TYPE = new ResourceLocation("uniform_bonus_count");
        private final int bonusMultiplier;

        public UniformBonusCount(int bonusMultiplier) {
            this.bonusMultiplier = bonusMultiplier;
        }

        @Override
        public int calculateNewCount(Random random, int initialCount, int enchantmentLevel) {
            return initialCount + random.nextInt(this.bonusMultiplier * enchantmentLevel + 1);
        }

        @Override
        public void serializeParams(JsonObject json, JsonSerializationContext context) {
            json.addProperty("bonusMultiplier", this.bonusMultiplier);
        }

        public static ApplyBonusCount.Formula deserialize(JsonObject json, JsonDeserializationContext context) {
            int i = GsonHelper.getAsInt(json, "bonusMultiplier");
            return new ApplyBonusCount.UniformBonusCount(i);
        }

        @Override
        public ResourceLocation getType() {
            return TYPE;
        }
    }
}
