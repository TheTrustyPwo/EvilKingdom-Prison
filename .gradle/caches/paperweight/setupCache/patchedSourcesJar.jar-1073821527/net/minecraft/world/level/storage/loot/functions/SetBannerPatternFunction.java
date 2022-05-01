package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetBannerPatternFunction extends LootItemConditionalFunction {
    final List<Pair<BannerPattern, DyeColor>> patterns;
    final boolean append;

    SetBannerPatternFunction(LootItemCondition[] conditions, List<Pair<BannerPattern, DyeColor>> patterns, boolean append) {
        super(conditions);
        this.patterns = patterns;
        this.append = append;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        CompoundTag compoundTag = BlockItem.getBlockEntityData(stack);
        if (compoundTag == null) {
            compoundTag = new CompoundTag();
        }

        BannerPattern.Builder builder = new BannerPattern.Builder();
        this.patterns.forEach(builder::addPattern);
        ListTag listTag = builder.toListTag();
        ListTag listTag2;
        if (this.append) {
            listTag2 = compoundTag.getList("Patterns", 10).copy();
            listTag2.addAll(listTag);
        } else {
            listTag2 = listTag;
        }

        compoundTag.put("Patterns", listTag2);
        BlockItem.setBlockEntityData(stack, BlockEntityType.BANNER, compoundTag);
        return stack;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_BANNER_PATTERN;
    }

    public static SetBannerPatternFunction.Builder setBannerPattern(boolean append) {
        return new SetBannerPatternFunction.Builder(append);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<SetBannerPatternFunction.Builder> {
        private final ImmutableList.Builder<Pair<BannerPattern, DyeColor>> patterns = ImmutableList.builder();
        private final boolean append;

        Builder(boolean append) {
            this.append = append;
        }

        @Override
        protected SetBannerPatternFunction.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetBannerPatternFunction(this.getConditions(), this.patterns.build(), this.append);
        }

        public SetBannerPatternFunction.Builder addPattern(BannerPattern pattern, DyeColor color) {
            this.patterns.add(Pair.of(pattern, color));
            return this;
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetBannerPatternFunction> {
        @Override
        public void serialize(JsonObject json, SetBannerPatternFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            JsonArray jsonArray = new JsonArray();
            object.patterns.forEach((pair) -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("pattern", pair.getFirst().getFilename());
                jsonObject.addProperty("color", pair.getSecond().getName());
                jsonArray.add(jsonObject);
            });
            json.add("patterns", jsonArray);
            json.addProperty("append", object.append);
        }

        @Override
        public SetBannerPatternFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            ImmutableList.Builder<Pair<BannerPattern, DyeColor>> builder = ImmutableList.builder();
            JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "patterns");

            for(int i = 0; i < jsonArray.size(); ++i) {
                JsonObject jsonObject2 = GsonHelper.convertToJsonObject(jsonArray.get(i), "pattern[" + i + "]");
                String string = GsonHelper.getAsString(jsonObject2, "pattern");
                BannerPattern bannerPattern = BannerPattern.byFilename(string);
                if (bannerPattern == null) {
                    throw new JsonSyntaxException("Unknown pattern: " + string);
                }

                String string2 = GsonHelper.getAsString(jsonObject2, "color");
                DyeColor dyeColor = DyeColor.byName(string2, (DyeColor)null);
                if (dyeColor == null) {
                    throw new JsonSyntaxException("Unknown color: " + string2);
                }

                builder.add(Pair.of(bannerPattern, dyeColor));
            }

            boolean bl = GsonHelper.getAsBoolean(jsonObject, "append");
            return new SetBannerPatternFunction(lootItemConditions, builder.build(), bl);
        }
    }
}
