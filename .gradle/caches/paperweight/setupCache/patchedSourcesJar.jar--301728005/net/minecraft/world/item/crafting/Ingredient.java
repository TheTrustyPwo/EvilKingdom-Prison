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
import java.util.Iterator;
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
    public boolean exact; // CraftBukkit

    public Ingredient(Stream<? extends Ingredient.Value> entries) {
        this.values = (Ingredient.Value[]) entries.toArray((i) -> {
            return new Ingredient.Value[i];
        });
    }

    public ItemStack[] getItems() {
        this.dissolve();
        return this.itemStacks;
    }

    public void dissolve() {
        if (this.itemStacks == null) {
            this.itemStacks = (ItemStack[]) Arrays.stream(this.values).flatMap((recipeitemstack_provider) -> {
                return recipeitemstack_provider.getItems().stream();
            }).distinct().toArray((i) -> {
                return new ItemStack[i];
            });
        }

    }

    public boolean test(@Nullable ItemStack itemstack) {
        if (itemstack == null) {
            return false;
        } else {
            this.dissolve();
            if (this.itemStacks.length == 0) {
                return itemstack.isEmpty();
            } else {
                ItemStack[] aitemstack = this.itemStacks;
                int i = aitemstack.length;

                for (int j = 0; j < i; ++j) {
                    ItemStack itemstack1 = aitemstack[j];

                    // CraftBukkit start
                    if (this.exact) {
                        if (itemstack1.getItem() == itemstack.getItem() && ItemStack.tagMatches(itemstack, itemstack1)) {
                            return true;
                        }

                        continue;
                    }
                    // CraftBukkit end
                    if (itemstack1.is(itemstack.getItem())) {
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
            ItemStack[] aitemstack = this.itemStacks;
            int i = aitemstack.length;

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = aitemstack[j];

                this.stackingIds.add(StackedContents.getStackingIndex(itemstack));
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
            JsonArray jsonarray = new JsonArray();
            Ingredient.Value[] arecipeitemstack_provider = this.values;
            int i = arecipeitemstack_provider.length;

            for (int j = 0; j < i; ++j) {
                Ingredient.Value recipeitemstack_provider = arecipeitemstack_provider[j];

                jsonarray.add(recipeitemstack_provider.serialize());
            }

            return jsonarray;
        }
    }

    public boolean isEmpty() {
        return this.values.length == 0 && (this.itemStacks == null || this.itemStacks.length == 0) && (this.stackingIds == null || this.stackingIds.isEmpty());
    }

    private static Ingredient fromValues(Stream<? extends Ingredient.Value> entries) {
        Ingredient recipeitemstack = new Ingredient(entries);

        return recipeitemstack.values.length == 0 ? Ingredient.EMPTY : recipeitemstack;
    }

    public static Ingredient of() {
        return Ingredient.EMPTY;
    }

    public static Ingredient of(ItemLike... items) {
        return Ingredient.of(Arrays.stream(items).map(ItemStack::new));
    }

    public static Ingredient of(ItemStack... stacks) {
        return Ingredient.of(Arrays.stream(stacks));
    }

    public static Ingredient of(Stream<ItemStack> stacks) {
        return Ingredient.fromValues(stacks.filter((itemstack) -> {
            return !itemstack.isEmpty();
        }).map(Ingredient.ItemValue::new));
    }

    public static Ingredient of(TagKey<Item> tag) {
        return Ingredient.fromValues(Stream.of(new Ingredient.TagValue(tag)));
    }

    public static Ingredient fromNetwork(FriendlyByteBuf buf) {
        return Ingredient.fromValues(buf.readList(FriendlyByteBuf::readItem).stream().map(Ingredient.ItemValue::new));
    }

    public static Ingredient fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            if (json.isJsonObject()) {
                return Ingredient.fromValues(Stream.of(Ingredient.valueFromJson(json.getAsJsonObject())));
            } else if (json.isJsonArray()) {
                JsonArray jsonarray = json.getAsJsonArray();

                if (jsonarray.size() == 0) {
                    throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
                } else {
                    return Ingredient.fromValues(StreamSupport.stream(jsonarray.spliterator(), false).map((jsonelement1) -> {
                        return Ingredient.valueFromJson(GsonHelper.convertToJsonObject(jsonelement1, "item"));
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
            ResourceLocation minecraftkey = new ResourceLocation(GsonHelper.getAsString(json, "tag"));
            TagKey<Item> tagkey = TagKey.create(Registry.ITEM_REGISTRY, minecraftkey);

            return new Ingredient.TagValue(tagkey);
        } else {
            throw new JsonParseException("An ingredient entry needs either a tag or an item");
        }
    }

    public interface Value {

        Collection<ItemStack> getItems();

        JsonObject serialize();
    }

    private static class TagValue implements Ingredient.Value {

        private final TagKey<Item> tag;

        TagValue(TagKey<Item> tag) {
            this.tag = tag;
        }

        @Override
        public Collection<ItemStack> getItems() {
            List<ItemStack> list = Lists.newArrayList();
            Iterator iterator = Registry.ITEM.getTagOrEmpty(this.tag).iterator();

            while (iterator.hasNext()) {
                Holder<Item> holder = (Holder) iterator.next();

                list.add(new ItemStack(holder));
            }

            return list;
        }

        @Override
        public JsonObject serialize() {
            JsonObject jsonobject = new JsonObject();

            jsonobject.addProperty("tag", this.tag.location().toString());
            return jsonobject;
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
            JsonObject jsonobject = new JsonObject();

            jsonobject.addProperty("item", Registry.ITEM.getKey(this.item.getItem()).toString());
            return jsonobject;
        }
    }
}
