package net.minecraft.world.item.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Iterator;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
// CraftBukkit start
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftShapelessRecipe;
// CraftBukkit end

public class ShapelessRecipe implements CraftingRecipe {

    private final ResourceLocation id;
    final String group;
    final ItemStack result;
    final NonNullList<Ingredient> ingredients;

    public ShapelessRecipe(ResourceLocation id, String group, ItemStack output, NonNullList<Ingredient> input) {
        this.id = id;
        this.group = group;
        this.result = output;
        this.ingredients = input;
    }

    // CraftBukkit start
    @SuppressWarnings("unchecked")
    public org.bukkit.inventory.ShapelessRecipe toBukkitRecipe() {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        CraftShapelessRecipe recipe = new CraftShapelessRecipe(result, this);
        recipe.setGroup(this.group);

        for (Ingredient list : this.ingredients) {
            recipe.addIngredient(CraftRecipe.toBukkit(list));
        }
        return recipe;
    }
    // CraftBukkit end

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.SHAPELESS_RECIPE;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public ItemStack getResultItem() {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    public boolean matches(CraftingContainer inventory, Level world) {
        StackedContents autorecipestackmanager = new StackedContents();
        int i = 0;

        // Paper start
        java.util.List<ItemStack> providedItems = new java.util.ArrayList<>();
        co.aikar.util.Counter<ItemStack> matchedProvided = new co.aikar.util.Counter<>();
        co.aikar.util.Counter<Ingredient> matchedIngredients = new co.aikar.util.Counter<>();
        // Paper end
        for (int j = 0; j < inventory.getContainerSize(); ++j) {
            ItemStack itemstack = inventory.getItem(j);

            if (!itemstack.isEmpty()) {
                // Paper start
                itemstack = itemstack.copy();
                providedItems.add(itemstack);
                for (Ingredient ingredient : ingredients) {
                    if (ingredient.test(itemstack)) {
                        matchedProvided.increment(itemstack);
                        matchedIngredients.increment(ingredient);
                    }
                }
                // Paper end
            }
        }

        // Paper start
        if (matchedProvided.isEmpty() || matchedIngredients.isEmpty()) {
            return false;
        }
        java.util.List<Ingredient> ingredients = new java.util.ArrayList<>(this.ingredients);
        providedItems.sort(java.util.Comparator.comparingInt((ItemStack c) -> (int) matchedProvided.getCount(c)).reversed());
        ingredients.sort(java.util.Comparator.comparingInt((Ingredient c) -> (int) matchedIngredients.getCount(c)));

        PROVIDED:
        for (ItemStack provided : providedItems) {
            for (Iterator<Ingredient> itIngredient = ingredients.iterator(); itIngredient.hasNext(); ) {
                Ingredient ingredient = itIngredient.next();
                if (ingredient.test(provided)) {
                    itIngredient.remove();
                    continue PROVIDED;
                }
            }
            return false;
        }
        return ingredients.isEmpty();
        // Paper end
    }

    public ItemStack assemble(CraftingContainer inventory) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.ingredients.size();
    }

    public static class Serializer implements RecipeSerializer<ShapelessRecipe> {

        public Serializer() {}

        @Override
        public ShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
            String s = GsonHelper.getAsString(json, "group", "");
            NonNullList<Ingredient> nonnulllist = Serializer.itemsFromJson(GsonHelper.getAsJsonArray(json, "ingredients"));

            if (nonnulllist.isEmpty()) {
                throw new JsonParseException("No ingredients for shapeless recipe");
            } else if (nonnulllist.size() > 9) {
                throw new JsonParseException("Too many ingredients for shapeless recipe");
            } else {
                ItemStack itemstack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));

                return new ShapelessRecipe(id, s, itemstack, nonnulllist);
            }
        }

        private static NonNullList<Ingredient> itemsFromJson(JsonArray json) {
            NonNullList<Ingredient> nonnulllist = NonNullList.create();

            for (int i = 0; i < json.size(); ++i) {
                Ingredient recipeitemstack = Ingredient.fromJson(json.get(i));

                if (!recipeitemstack.isEmpty()) {
                    nonnulllist.add(recipeitemstack);
                }
            }

            return nonnulllist;
        }

        @Override
        public ShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String s = buf.readUtf();
            int i = buf.readVarInt();
            NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i, Ingredient.EMPTY);

            for (int j = 0; j < nonnulllist.size(); ++j) {
                nonnulllist.set(j, Ingredient.fromNetwork(buf));
            }

            ItemStack itemstack = buf.readItem();

            return new ShapelessRecipe(id, s, itemstack, nonnulllist);
        }

        public void toNetwork(FriendlyByteBuf buf, ShapelessRecipe recipe) {
            buf.writeUtf(recipe.group);
            buf.writeVarInt(recipe.ingredients.size());
            Iterator iterator = recipe.ingredients.iterator();

            while (iterator.hasNext()) {
                Ingredient recipeitemstack = (Ingredient) iterator.next();

                recipeitemstack.toNetwork(buf);
            }

            buf.writeItem(recipe.result);
        }
    }
}
