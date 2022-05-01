package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

public class LootTable {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final LootTable EMPTY = new LootTable(LootContextParamSets.EMPTY, new LootPool[0], new LootItemFunction[0]);
    public static final LootContextParamSet DEFAULT_PARAM_SET = LootContextParamSets.ALL_PARAMS;
    final LootContextParamSet paramSet;
    final LootPool[] pools;
    final LootItemFunction[] functions;
    private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

    LootTable(LootContextParamSet type, LootPool[] pools, LootItemFunction[] functions) {
        this.paramSet = type;
        this.pools = pools;
        this.functions = functions;
        this.compositeFunction = LootItemFunctions.compose(functions);
    }

    public static Consumer<ItemStack> createStackSplitter(Consumer<ItemStack> lootConsumer) {
        return (stack) -> {
            if (stack.getCount() < stack.getMaxStackSize()) {
                lootConsumer.accept(stack);
            } else {
                int i = stack.getCount();

                while(i > 0) {
                    ItemStack itemStack = stack.copy();
                    itemStack.setCount(Math.min(stack.getMaxStackSize(), i));
                    i -= itemStack.getCount();
                    lootConsumer.accept(itemStack);
                }
            }

        };
    }

    public void getRandomItemsRaw(LootContext context, Consumer<ItemStack> lootConsumer) {
        if (context.addVisitedTable(this)) {
            Consumer<ItemStack> consumer = LootItemFunction.decorate(this.compositeFunction, lootConsumer, context);

            for(LootPool lootPool : this.pools) {
                lootPool.addRandomItems(consumer, context);
            }

            context.removeVisitedTable(this);
        } else {
            LOGGER.warn("Detected infinite loop in loot tables");
        }

    }

    public void getRandomItems(LootContext context, Consumer<ItemStack> lootConsumer) {
        this.getRandomItemsRaw(context, createStackSplitter(lootConsumer));
    }

    public List<ItemStack> getRandomItems(LootContext context) {
        List<ItemStack> list = Lists.newArrayList();
        this.getRandomItems(context, list::add);
        return list;
    }

    public LootContextParamSet getParamSet() {
        return this.paramSet;
    }

    public void validate(ValidationContext reporter) {
        for(int i = 0; i < this.pools.length; ++i) {
            this.pools[i].validate(reporter.forChild(".pools[" + i + "]"));
        }

        for(int j = 0; j < this.functions.length; ++j) {
            this.functions[j].validate(reporter.forChild(".functions[" + j + "]"));
        }

    }

    public void fill(Container inventory, LootContext context) {
        List<ItemStack> list = this.getRandomItems(context);
        Random random = context.getRandom();
        List<Integer> list2 = this.getAvailableSlots(inventory, random);
        this.shuffleAndSplitItems(list, list2.size(), random);

        for(ItemStack itemStack : list) {
            if (list2.isEmpty()) {
                LOGGER.warn("Tried to over-fill a container");
                return;
            }

            if (itemStack.isEmpty()) {
                inventory.setItem(list2.remove(list2.size() - 1), ItemStack.EMPTY);
            } else {
                inventory.setItem(list2.remove(list2.size() - 1), itemStack);
            }
        }

    }

    private void shuffleAndSplitItems(List<ItemStack> drops, int freeSlots, Random random) {
        List<ItemStack> list = Lists.newArrayList();
        Iterator<ItemStack> iterator = drops.iterator();

        while(iterator.hasNext()) {
            ItemStack itemStack = iterator.next();
            if (itemStack.isEmpty()) {
                iterator.remove();
            } else if (itemStack.getCount() > 1) {
                list.add(itemStack);
                iterator.remove();
            }
        }

        while(freeSlots - drops.size() - list.size() > 0 && !list.isEmpty()) {
            ItemStack itemStack2 = list.remove(Mth.nextInt(random, 0, list.size() - 1));
            int i = Mth.nextInt(random, 1, itemStack2.getCount() / 2);
            ItemStack itemStack3 = itemStack2.split(i);
            if (itemStack2.getCount() > 1 && random.nextBoolean()) {
                list.add(itemStack2);
            } else {
                drops.add(itemStack2);
            }

            if (itemStack3.getCount() > 1 && random.nextBoolean()) {
                list.add(itemStack3);
            } else {
                drops.add(itemStack3);
            }
        }

        drops.addAll(list);
        Collections.shuffle(drops, random);
    }

    private List<Integer> getAvailableSlots(Container inventory, Random random) {
        List<Integer> list = Lists.newArrayList();

        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            if (inventory.getItem(i).isEmpty()) {
                list.add(i);
            }
        }

        Collections.shuffle(list, random);
        return list;
    }

    public static LootTable.Builder lootTable() {
        return new LootTable.Builder();
    }

    public static class Builder implements FunctionUserBuilder<LootTable.Builder> {
        private final List<LootPool> pools = Lists.newArrayList();
        private final List<LootItemFunction> functions = Lists.newArrayList();
        private LootContextParamSet paramSet = LootTable.DEFAULT_PARAM_SET;

        public LootTable.Builder withPool(LootPool.Builder poolBuilder) {
            this.pools.add(poolBuilder.build());
            return this;
        }

        public LootTable.Builder setParamSet(LootContextParamSet context) {
            this.paramSet = context;
            return this;
        }

        @Override
        public LootTable.Builder apply(LootItemFunction.Builder builder) {
            this.functions.add(builder.build());
            return this;
        }

        @Override
        public LootTable.Builder unwrap() {
            return this;
        }

        public LootTable build() {
            return new LootTable(this.paramSet, this.pools.toArray(new LootPool[0]), this.functions.toArray(new LootItemFunction[0]));
        }
    }

    public static class Serializer implements JsonDeserializer<LootTable>, JsonSerializer<LootTable> {
        @Override
        public LootTable deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "loot table");
            LootPool[] lootPools = GsonHelper.getAsObject(jsonObject, "pools", new LootPool[0], jsonDeserializationContext, LootPool[].class);
            LootContextParamSet lootContextParamSet = null;
            if (jsonObject.has("type")) {
                String string = GsonHelper.getAsString(jsonObject, "type");
                lootContextParamSet = LootContextParamSets.get(new ResourceLocation(string));
            }

            LootItemFunction[] lootItemFunctions = GsonHelper.getAsObject(jsonObject, "functions", new LootItemFunction[0], jsonDeserializationContext, LootItemFunction[].class);
            return new LootTable(lootContextParamSet != null ? lootContextParamSet : LootContextParamSets.ALL_PARAMS, lootPools, lootItemFunctions);
        }

        @Override
        public JsonElement serialize(LootTable lootTable, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            if (lootTable.paramSet != LootTable.DEFAULT_PARAM_SET) {
                ResourceLocation resourceLocation = LootContextParamSets.getKey(lootTable.paramSet);
                if (resourceLocation != null) {
                    jsonObject.addProperty("type", resourceLocation.toString());
                } else {
                    LootTable.LOGGER.warn("Failed to find id for param set {}", (Object)lootTable.paramSet);
                }
            }

            if (lootTable.pools.length > 0) {
                jsonObject.add("pools", jsonSerializationContext.serialize(lootTable.pools));
            }

            if (!ArrayUtils.isEmpty((Object[])lootTable.functions)) {
                jsonObject.add("functions", jsonSerializationContext.serialize(lootTable.functions));
            }

            return jsonObject;
        }
    }
}
