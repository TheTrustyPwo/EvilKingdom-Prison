package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class TagEntry extends LootPoolSingletonContainer {
    final TagKey<Item> tag;
    final boolean expand;

    TagEntry(TagKey<Item> name, boolean expand, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions) {
        super(weight, quality, conditions, functions);
        this.tag = name;
        this.expand = expand;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.TAG;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> lootConsumer, LootContext context) {
        Registry.ITEM.getTagOrEmpty(this.tag).forEach((entry) -> {
            lootConsumer.accept(new ItemStack(entry));
        });
    }

    private boolean expandTag(LootContext context, Consumer<LootPoolEntry> lootChoiceExpander) {
        if (!this.canRun(context)) {
            return false;
        } else {
            for(final Holder<Item> holder : Registry.ITEM.getTagOrEmpty(this.tag)) {
                lootChoiceExpander.accept(new LootPoolSingletonContainer.EntryBase() {
                    @Override
                    public void createItemStack(Consumer<ItemStack> lootConsumer, LootContext context) {
                        lootConsumer.accept(new ItemStack(holder));
                    }
                });
            }

            return true;
        }
    }

    @Override
    public boolean expand(LootContext context, Consumer<LootPoolEntry> choiceConsumer) {
        return this.expand ? this.expandTag(context, choiceConsumer) : super.expand(context, choiceConsumer);
    }

    public static LootPoolSingletonContainer.Builder<?> tagContents(TagKey<Item> name) {
        return simpleBuilder((weight, quality, conditions, functions) -> {
            return new TagEntry(name, false, weight, quality, conditions, functions);
        });
    }

    public static LootPoolSingletonContainer.Builder<?> expandTag(TagKey<Item> name) {
        return simpleBuilder((weight, quality, conditions, functions) -> {
            return new TagEntry(name, true, weight, quality, conditions, functions);
        });
    }

    public static class Serializer extends LootPoolSingletonContainer.Serializer<TagEntry> {
        @Override
        public void serializeCustom(JsonObject json, TagEntry entry, JsonSerializationContext context) {
            super.serializeCustom(json, entry, context);
            json.addProperty("name", entry.tag.location().toString());
            json.addProperty("expand", entry.expand);
        }

        @Override
        protected TagEntry deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, int i, int j, LootItemCondition[] lootItemConditions, LootItemFunction[] lootItemFunctions) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "name"));
            TagKey<Item> tagKey = TagKey.create(Registry.ITEM_REGISTRY, resourceLocation);
            boolean bl = GsonHelper.getAsBoolean(jsonObject, "expand");
            return new TagEntry(tagKey, bl, i, j, lootItemConditions, lootItemFunctions);
        }
    }
}
