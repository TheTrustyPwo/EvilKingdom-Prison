package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class Ingredient implements Predicate<ItemStack> {
    public static final Ingredient EMPTY = new Ingredient(Stream.empty());
    private final Ingredient.Value[] values;
    @Nullable
    public ItemStack[] itemStacks;
    @Nullable
    private IntList stackingIds;

    public Ingredient(Stream<? extends Ingredient.Value> entries) {
        this.values = entries.toArray((i) -> {
            return new Ingredient.Value[i];
        });
    }

    public ItemStack[] getItems() {
        this.dissolve();
        return this.itemStacks;
    }

    public void dissolve() {
        if (this.itemStacks == null) {
            this.itemStacks = Arrays.stream(this.values).flatMap((value) -> {
                return value.getItems().stream();
            }).distinct().toArray((i) -> {
                return new ItemStack[i];
            });
        }

    }

    @Override
    public boolean test(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        } else {
            this.dissolve();
            if (this.itemStacks.length == 0) {
                return itemStack.isEmpty();
            } else {
                for(ItemStack itemStack2 : this.itemStacks) {
                    if (itemStack2.is(itemStack.getItem())) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public IntList getStackingIds() {
        if (this.stackingIds == null) {
            this.dissolve();
            this.stackingIds = new IntArrayList(this.itemStacks.length);

            for(ItemStack itemStack : this.itemStacks) {
                this.stackingIds.add(StackedContents.getStackingIndex(itemStack));
            }

            this.stackingIds.sort(IntComparators.NATURAL_COMPARATOR);
        }

        return this.stackingIds;
    }

    public void toNetwork(FriendlyByteBuf buf) {
        this.dissolve();
        buf.writeCollection(Arrays.asList(this.itemStacks), FriendlyByteBuf::writeItem);
    }

    public JsonElement toJson() {
        if (this.values.length == 1) {
            return this.values[0].serialize();
        } else {
            JsonArray jsonArray = new JsonArray();

            for(Ingredient.Value value : this.values) {
                jsonArray.add(value.serialize());
            }

            return jsonArray;
        }
    }

    public boolean isEmpty() {
        return this.values.length == 0 && (this.itemStacks == null || this.itemStacks.length == 0) && (this.stackingIds == null || this.stackingIds.isEmpty());
    }

    private static Ingredient fromValues(Stream<? extends Ingredient.Value> entries) {
        Ingredient ingredient = new Ingredient(entries);
        return ingredient.values.length == 0 ? EMPTY : ingredient;
    }

    public static Ingredient of() {
        return EMPTY;
    }

    public static Ingredient of(ItemLike... items) {
        return of(Arrays.stream(items).map(ItemStack::new));
    }

    public static Ingredient of(ItemStack... stacks) {
        return of(Arrays.stream(stacks));
    }

    public static Ingredient of(Stream<ItemStack> stacks) {
        return fromValues(stacks.filter((stack) -> {
            return !stack.isEmpty();
        }).map(Ingredient.ItemValue::new));
    }

    public static Ingredient of(TagKey<Item> tag) {
        return fromValues(Stream.of(new Ingredient.TagValue(tag)));
    }

    public static Ingredient fromNetwork(FriendlyByteBuf buf) {
        return fromValues(buf.readList(FriendlyByteBuf::readItem).stream().map(Ingredient.ItemValue::new));
    }

    public static Ingredient fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            if (json.isJsonObject()) {
                return fromValues(Stream.of(valueFromJson(json.getAsJsonObject())));
            } else if (json.isJsonArray()) {
                JsonArray jsonArray = json.getAsJsonArray();
                if (jsonArray.size() == 0) {
                    throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
                } else {
                    return fromValues(StreamSupport.stream(jsonArray.spliterator(), false).map((jsonElement) -> {
                        return valueFromJson(GsonHelper.convertToJsonObject(jsonElement, "item"));
                    }));
                }
            } else {
                throw new JsonSyntaxException("Expected item to be object or array of objects");
            }
        } else {
            throw new JsonSyntaxException("Item cannot be null");
        }
    }

    private static Ingredient.Value valueFromJson(JsonObject json) {
        if (json.has("item") && json.has("tag")) {
            throw new JsonParseException("An ingredient entry is either a tag or an item, not both");
        } else if (json.has("item")) {
            Item item = ShapedRecipe.itemFromJson(json);
            return new Ingredient.ItemValue(new ItemStack(item));
        } else if (json.has("tag")) {
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(json, "tag"));
            TagKey<Item> tagKey = TagKey.create(Registry.ITEM_REGISTRY, resourceLocation);
            return new Ingredient.TagValue(tagKey);
        } else {
            throw new JsonParseException("An ingredient entry needs either a tag or an item");
        }
    }

    public static class ItemValue implements Ingredient.Value {
        private final ItemStack item;

        public ItemValue(ItemStack stack) {
            this.item = stack;
        }

        @Override
        public Collection<ItemStack> getItems() {
            return Collections.singleton(this.item);
        }

        @Override
        public JsonObject serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("item", Registry.ITEM.getKey(this.item.getItem()).toString());
            return jsonObject;
        }
    }

    static class TagValue implements Ingredient.Value {
        private final TagKey<Item> tag;

        TagValue(TagKey<Item> tag) {
            this.tag = tag;
        }

        @Override
        public Collection<ItemStack> getItems() {
            List<ItemStack> list = Lists.newArrayList();

            for(Holder<Item> holder : Registry.ITEM.getTagOrEmpty(this.tag)) {
                list.add(new ItemStack(holder));
            }

            return list;
        }

        @Override
        public JsonObject serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("tag", this.tag.location().toString());
            return jsonObject;
        }
    }

    public interface Value {
        Collection<ItemStack> getItems();

        JsonObject serialize();
    }
}
