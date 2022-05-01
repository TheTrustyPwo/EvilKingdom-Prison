package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class FireworkStarFadeRecipe extends CustomRecipe {
    private static final Ingredient STAR_INGREDIENT = Ingredient.of(Items.FIREWORK_STAR);

    public FireworkStarFadeRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level world) {
        boolean bl = false;
        boolean bl2 = false;

        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                if (itemStack.getItem() instanceof DyeItem) {
                    bl = true;
                } else {
                    if (!STAR_INGREDIENT.test(itemStack)) {
                        return false;
                    }

                    if (bl2) {
                        return false;
                    }

                    bl2 = true;
                }
            }
        }

        return bl2 && bl;
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory) {
        List<Integer> list = Lists.newArrayList();
        ItemStack itemStack = null;

        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemStack2 = inventory.getItem(i);
            Item item = itemStack2.getItem();
            if (item instanceof DyeItem) {
                list.add(((DyeItem)item).getDyeColor().getFireworkColor());
            } else if (STAR_INGREDIENT.test(itemStack2)) {
                itemStack = itemStack2.copy();
                itemStack.setCount(1);
            }
        }

        if (itemStack != null && !list.isEmpty()) {
            itemStack.getOrCreateTagElement("Explosion").putIntArray("FadeColors", list);
            return itemStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.FIREWORK_STAR_FADE;
    }
}
