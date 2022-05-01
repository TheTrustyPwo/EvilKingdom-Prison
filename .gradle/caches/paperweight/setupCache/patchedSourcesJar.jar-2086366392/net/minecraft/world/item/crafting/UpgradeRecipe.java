package net.minecraft.world.item.crafting;

import com.google.gson.JsonObject;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

// CraftBukkit start
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftSmithingRecipe;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftNamespacedKey;
import org.bukkit.inventory.Recipe;
// CraftBukkit end

public class UpgradeRecipe implements net.minecraft.world.item.crafting.Recipe<Container> {

    final Ingredient base;
    final Ingredient addition;
    final ItemStack result;
    private final ResourceLocation id;
    final boolean copyNbt; // Paper

    public UpgradeRecipe(ResourceLocation id, Ingredient base, Ingredient addition, ItemStack result) {
        // Paper start
        this(id, base, addition, result, true);
    }
    public UpgradeRecipe(ResourceLocation id, Ingredient base, Ingredient addition, ItemStack result, boolean copyNbt) {
        this.copyNbt = copyNbt;
        // Paper end
        this.id = id;
        this.base = base;
        this.addition = addition;
        this.result = result;
    }

    @Override
    public boolean matches(Container inventory, Level world) {
        return this.base.test(inventory.getItem(0)) && this.addition.test(inventory.getItem(1));
    }

    @Override
    public ItemStack assemble(Container inventory) {
        ItemStack itemstack = this.result.copy();
        if (copyNbt) { // Paper - copy nbt conditionally
        CompoundTag nbttagcompound = inventory.getItem(0).getTag();

        if (nbttagcompound != null) {
            itemstack.setTag(nbttagcompound.copy());
        }
        } // Paper

        return itemstack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem() {
        return this.result;
    }

    public boolean isAdditionIngredient(ItemStack stack) {
        return this.addition.test(stack);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.SMITHING_TABLE);
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.SMITHING;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }

    @Override
    public boolean isIncomplete() {
        return Stream.of(this.base, this.addition).anyMatch((recipeitemstack) -> {
            return recipeitemstack.getItems().length == 0;
        });
    }

    // CraftBukkit start
    @Override
    public Recipe toBukkitRecipe() {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);

        CraftSmithingRecipe recipe = new CraftSmithingRecipe(CraftNamespacedKey.fromMinecraft(this.id), result, CraftRecipe.toBukkit(this.base), CraftRecipe.toBukkit(this.addition), this.copyNbt); // Paper

        return recipe;
    }
    // CraftBukkit end

    public static class Serializer implements RecipeSerializer<UpgradeRecipe> {

        public Serializer() {}

        @Override
        public UpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient recipeitemstack = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "base"));
            Ingredient recipeitemstack1 = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "addition"));
            ItemStack itemstack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));

            return new UpgradeRecipe(id, recipeitemstack, recipeitemstack1, itemstack);
        }

        @Override
        public UpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient recipeitemstack = Ingredient.fromNetwork(buf);
            Ingredient recipeitemstack1 = Ingredient.fromNetwork(buf);
            ItemStack itemstack = buf.readItem();

            return new UpgradeRecipe(id, recipeitemstack, recipeitemstack1, itemstack);
        }

        public void toNetwork(FriendlyByteBuf buf, UpgradeRecipe recipe) {
            recipe.base.toNetwork(buf);
            recipe.addition.toNetwork(buf);
            buf.writeItem(recipe.result);
        }
    }
}
