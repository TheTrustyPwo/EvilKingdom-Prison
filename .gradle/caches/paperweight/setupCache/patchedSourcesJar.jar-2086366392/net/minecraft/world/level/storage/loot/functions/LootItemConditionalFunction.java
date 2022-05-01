package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import org.apache.commons.lang3.ArrayUtils;

public abstract class LootItemConditionalFunction implements LootItemFunction {
    protected final LootItemCondition[] predicates;
    private final Predicate<LootContext> compositePredicates;

    protected LootItemConditionalFunction(LootItemCondition[] conditions) {
        this.predicates = conditions;
        this.compositePredicates = LootItemConditions.andConditions(conditions);
    }

    @Override
    public final ItemStack apply(ItemStack itemStack, LootContext lootContext) {
        return this.compositePredicates.test(lootContext) ? this.run(itemStack, lootContext) : itemStack;
    }

    protected abstract ItemStack run(ItemStack stack, LootContext context);

    @Override
    public void validate(ValidationContext reporter) {
        LootItemFunction.super.validate(reporter);

        for(int i = 0; i < this.predicates.length; ++i) {
            this.predicates[i].validate(reporter.forChild(".conditions[" + i + "]"));
        }

    }

    protected static LootItemConditionalFunction.Builder<?> simpleBuilder(Function<LootItemCondition[], LootItemFunction> joiner) {
        return new LootItemConditionalFunction.DummyBuilder(joiner);
    }

    public abstract static class Builder<T extends LootItemConditionalFunction.Builder<T>> implements LootItemFunction.Builder, ConditionUserBuilder<T> {
        private final List<LootItemCondition> conditions = Lists.newArrayList();

        @Override
        public T when(LootItemCondition.Builder builder) {
            this.conditions.add(builder.build());
            return this.getThis();
        }

        @Override
        public final T unwrap() {
            return this.getThis();
        }

        protected abstract T getThis();

        protected LootItemCondition[] getConditions() {
            return this.conditions.toArray(new LootItemCondition[0]);
        }
    }

    static final class DummyBuilder extends LootItemConditionalFunction.Builder<LootItemConditionalFunction.DummyBuilder> {
        private final Function<LootItemCondition[], LootItemFunction> constructor;

        public DummyBuilder(Function<LootItemCondition[], LootItemFunction> joiner) {
            this.constructor = joiner;
        }

        @Override
        protected LootItemConditionalFunction.DummyBuilder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return this.constructor.apply(this.getConditions());
        }
    }

    public abstract static class Serializer<T extends LootItemConditionalFunction> implements net.minecraft.world.level.storage.loot.Serializer<T> {
        @Override
        public void serialize(JsonObject json, T object, JsonSerializationContext context) {
            if (!ArrayUtils.isEmpty((Object[])object.predicates)) {
                json.add("conditions", context.serialize(object.predicates));
            }

        }

        @Override
        public final T deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            LootItemCondition[] lootItemConditions = GsonHelper.getAsObject(jsonObject, "conditions", new LootItemCondition[0], jsonDeserializationContext, LootItemCondition[].class);
            return this.deserialize(jsonObject, jsonDeserializationContext, lootItemConditions);
        }

        public abstract T deserialize(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions);
    }
}
