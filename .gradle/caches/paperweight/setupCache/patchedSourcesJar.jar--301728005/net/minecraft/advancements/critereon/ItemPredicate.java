package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ItemLike;

public class ItemPredicate {
    public static final ItemPredicate ANY = new ItemPredicate();
    @Nullable
    private final TagKey<Item> tag;
    @Nullable
    private final Set<Item> items;
    private final MinMaxBounds.Ints count;
    private final MinMaxBounds.Ints durability;
    private final EnchantmentPredicate[] enchantments;
    private final EnchantmentPredicate[] storedEnchantments;
    @Nullable
    private final Potion potion;
    private final NbtPredicate nbt;

    public ItemPredicate() {
        this.tag = null;
        this.items = null;
        this.potion = null;
        this.count = MinMaxBounds.Ints.ANY;
        this.durability = MinMaxBounds.Ints.ANY;
        this.enchantments = EnchantmentPredicate.NONE;
        this.storedEnchantments = EnchantmentPredicate.NONE;
        this.nbt = NbtPredicate.ANY;
    }

    public ItemPredicate(@Nullable TagKey<Item> tag, @Nullable Set<Item> items, MinMaxBounds.Ints count, MinMaxBounds.Ints durability, EnchantmentPredicate[] enchantments, EnchantmentPredicate[] storedEnchantments, @Nullable Potion potion, NbtPredicate nbt) {
        this.tag = tag;
        this.items = items;
        this.count = count;
        this.durability = durability;
        this.enchantments = enchantments;
        this.storedEnchantments = storedEnchantments;
        this.potion = potion;
        this.nbt = nbt;
    }

    public boolean matches(ItemStack stack) {
        if (this == ANY) {
            return true;
        } else if (this.tag != null && !stack.is(this.tag)) {
            return false;
        } else if (this.items != null && !this.items.contains(stack.getItem())) {
            return false;
        } else if (!this.count.matches(stack.getCount())) {
            return false;
        } else if (!this.durability.isAny() && !stack.isDamageableItem()) {
            return false;
        } else if (!this.durability.matches(stack.getMaxDamage() - stack.getDamageValue())) {
            return false;
        } else if (!this.nbt.matches(stack)) {
            return false;
        } else {
            if (this.enchantments.length > 0) {
                Map<Enchantment, Integer> map = EnchantmentHelper.deserializeEnchantments(stack.getEnchantmentTags());

                for(EnchantmentPredicate enchantmentPredicate : this.enchantments) {
                    if (!enchantmentPredicate.containedIn(map)) {
                        return false;
                    }
                }
            }

            if (this.storedEnchantments.length > 0) {
                Map<Enchantment, Integer> map2 = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack));

                for(EnchantmentPredicate enchantmentPredicate2 : this.storedEnchantments) {
                    if (!enchantmentPredicate2.containedIn(map2)) {
                        return false;
                    }
                }
            }

            Potion potion = PotionUtils.getPotion(stack);
            return this.potion == null || this.potion == potion;
        }
    }

    public static ItemPredicate fromJson(@Nullable JsonElement el) {
        if (el != null && !el.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(el, "item");
            MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject.get("count"));
            MinMaxBounds.Ints ints2 = MinMaxBounds.Ints.fromJson(jsonObject.get("durability"));
            if (jsonObject.has("data")) {
                throw new JsonParseException("Disallowed data tag found");
            } else {
                NbtPredicate nbtPredicate = NbtPredicate.fromJson(jsonObject.get("nbt"));
                Set<Item> set = null;
                JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "items", (JsonArray)null);
                if (jsonArray != null) {
                    ImmutableSet.Builder<Item> builder = ImmutableSet.builder();

                    for(JsonElement jsonElement : jsonArray) {
                        ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.convertToString(jsonElement, "item"));
                        builder.add(Registry.ITEM.getOptional(resourceLocation).orElseThrow(() -> {
                            return new JsonSyntaxException("Unknown item id '" + resourceLocation + "'");
                        }));
                    }

                    set = builder.build();
                }

                TagKey<Item> tagKey = null;
                if (jsonObject.has("tag")) {
                    ResourceLocation resourceLocation2 = new ResourceLocation(GsonHelper.getAsString(jsonObject, "tag"));
                    tagKey = TagKey.create(Registry.ITEM_REGISTRY, resourceLocation2);
                }

                Potion potion = null;
                if (jsonObject.has("potion")) {
                    ResourceLocation resourceLocation3 = new ResourceLocation(GsonHelper.getAsString(jsonObject, "potion"));
                    potion = Registry.POTION.getOptional(resourceLocation3).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown potion '" + resourceLocation3 + "'");
                    });
                }

                EnchantmentPredicate[] enchantmentPredicates = EnchantmentPredicate.fromJsonArray(jsonObject.get("enchantments"));
                EnchantmentPredicate[] enchantmentPredicates2 = EnchantmentPredicate.fromJsonArray(jsonObject.get("stored_enchantments"));
                return new ItemPredicate(tagKey, set, ints, ints2, enchantmentPredicates, enchantmentPredicates2, potion, nbtPredicate);
            }
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.items != null) {
                JsonArray jsonArray = new JsonArray();

                for(Item item : this.items) {
                    jsonArray.add(Registry.ITEM.getKey(item).toString());
                }

                jsonObject.add("items", jsonArray);
            }

            if (this.tag != null) {
                jsonObject.addProperty("tag", this.tag.location().toString());
            }

            jsonObject.add("count", this.count.serializeToJson());
            jsonObject.add("durability", this.durability.serializeToJson());
            jsonObject.add("nbt", this.nbt.serializeToJson());
            if (this.enchantments.length > 0) {
                JsonArray jsonArray2 = new JsonArray();

                for(EnchantmentPredicate enchantmentPredicate : this.enchantments) {
                    jsonArray2.add(enchantmentPredicate.serializeToJson());
                }

                jsonObject.add("enchantments", jsonArray2);
            }

            if (this.storedEnchantments.length > 0) {
                JsonArray jsonArray3 = new JsonArray();

                for(EnchantmentPredicate enchantmentPredicate2 : this.storedEnchantments) {
                    jsonArray3.add(enchantmentPredicate2.serializeToJson());
                }

                jsonObject.add("stored_enchantments", jsonArray3);
            }

            if (this.potion != null) {
                jsonObject.addProperty("potion", Registry.POTION.getKey(this.potion).toString());
            }

            return jsonObject;
        }
    }

    public static ItemPredicate[] fromJsonArray(@Nullable JsonElement el) {
        if (el != null && !el.isJsonNull()) {
            JsonArray jsonArray = GsonHelper.convertToJsonArray(el, "items");
            ItemPredicate[] itemPredicates = new ItemPredicate[jsonArray.size()];

            for(int i = 0; i < itemPredicates.length; ++i) {
                itemPredicates[i] = fromJson(jsonArray.get(i));
            }

            return itemPredicates;
        } else {
            return new ItemPredicate[0];
        }
    }

    public static class Builder {
        private final List<EnchantmentPredicate> enchantments = Lists.newArrayList();
        private final List<EnchantmentPredicate> storedEnchantments = Lists.newArrayList();
        @Nullable
        private Set<Item> items;
        @Nullable
        private TagKey<Item> tag;
        private MinMaxBounds.Ints count = MinMaxBounds.Ints.ANY;
        private MinMaxBounds.Ints durability = MinMaxBounds.Ints.ANY;
        @Nullable
        private Potion potion;
        private NbtPredicate nbt = NbtPredicate.ANY;

        private Builder() {
        }

        public static ItemPredicate.Builder item() {
            return new ItemPredicate.Builder();
        }

        public ItemPredicate.Builder of(ItemLike... items) {
            this.items = Stream.of(items).map(ItemLike::asItem).collect(ImmutableSet.toImmutableSet());
            return this;
        }

        public ItemPredicate.Builder of(TagKey<Item> tag) {
            this.tag = tag;
            return this;
        }

        public ItemPredicate.Builder withCount(MinMaxBounds.Ints count) {
            this.count = count;
            return this;
        }

        public ItemPredicate.Builder hasDurability(MinMaxBounds.Ints durability) {
            this.durability = durability;
            return this;
        }

        public ItemPredicate.Builder isPotion(Potion potion) {
            this.potion = potion;
            return this;
        }

        public ItemPredicate.Builder hasNbt(CompoundTag nbt) {
            this.nbt = new NbtPredicate(nbt);
            return this;
        }

        public ItemPredicate.Builder hasEnchantment(EnchantmentPredicate enchantment) {
            this.enchantments.add(enchantment);
            return this;
        }

        public ItemPredicate.Builder hasStoredEnchantment(EnchantmentPredicate enchantment) {
            this.storedEnchantments.add(enchantment);
            return this;
        }

        public ItemPredicate build() {
            return new ItemPredicate(this.tag, this.items, this.count, this.durability, this.enchantments.toArray(EnchantmentPredicate.NONE), this.storedEnchantments.toArray(EnchantmentPredicate.NONE), this.potion, this.nbt);
        }
    }
}
