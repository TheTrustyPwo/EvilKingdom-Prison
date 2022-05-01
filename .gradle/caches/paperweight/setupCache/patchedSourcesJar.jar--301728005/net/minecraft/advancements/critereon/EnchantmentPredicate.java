package net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentPredicate {
    public static final EnchantmentPredicate ANY = new EnchantmentPredicate();
    public static final EnchantmentPredicate[] NONE = new EnchantmentPredicate[0];
    @Nullable
    private final Enchantment enchantment;
    private final MinMaxBounds.Ints level;

    public EnchantmentPredicate() {
        this.enchantment = null;
        this.level = MinMaxBounds.Ints.ANY;
    }

    public EnchantmentPredicate(@Nullable Enchantment enchantment, MinMaxBounds.Ints levels) {
        this.enchantment = enchantment;
        this.level = levels;
    }

    public boolean containedIn(Map<Enchantment, Integer> enchantments) {
        if (this.enchantment != null) {
            if (!enchantments.containsKey(this.enchantment)) {
                return false;
            }

            int i = enchantments.get(this.enchantment);
            if (this.level != MinMaxBounds.Ints.ANY && !this.level.matches(i)) {
                return false;
            }
        } else if (this.level != MinMaxBounds.Ints.ANY) {
            for(Integer integer : enchantments.values()) {
                if (this.level.matches(integer)) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.enchantment != null) {
                jsonObject.addProperty("enchantment", Registry.ENCHANTMENT.getKey(this.enchantment).toString());
            }

            jsonObject.add("levels", this.level.serializeToJson());
            return jsonObject;
        }
    }

    public static EnchantmentPredicate fromJson(@Nullable JsonElement el) {
        if (el != null && !el.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(el, "enchantment");
            Enchantment enchantment = null;
            if (jsonObject.has("enchantment")) {
                ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "enchantment"));
                enchantment = Registry.ENCHANTMENT.getOptional(resourceLocation).orElseThrow(() -> {
                    return new JsonSyntaxException("Unknown enchantment '" + resourceLocation + "'");
                });
            }

            MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject.get("levels"));
            return new EnchantmentPredicate(enchantment, ints);
        } else {
            return ANY;
        }
    }

    public static EnchantmentPredicate[] fromJsonArray(@Nullable JsonElement el) {
        if (el != null && !el.isJsonNull()) {
            JsonArray jsonArray = GsonHelper.convertToJsonArray(el, "enchantments");
            EnchantmentPredicate[] enchantmentPredicates = new EnchantmentPredicate[jsonArray.size()];

            for(int i = 0; i < enchantmentPredicates.length; ++i) {
                enchantmentPredicates[i] = fromJson(jsonArray.get(i));
            }

            return enchantmentPredicates;
        } else {
            return NONE;
        }
    }
}
