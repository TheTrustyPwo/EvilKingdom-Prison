package net.minecraft.world.inventory;

import java.util.Collections;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public interface RecipeHolder {
    void setRecipeUsed(@Nullable Recipe<?> recipe);

    @Nullable
    Recipe<?> getRecipeUsed();

    default void awardUsedRecipes(Player player) {
        Recipe<?> recipe = this.getRecipeUsed();
        if (recipe != null && !recipe.isSpecial()) {
            player.awardRecipes(Collections.singleton(recipe));
            this.setRecipeUsed((Recipe<?>)null);
        }

    }

    default boolean setRecipeUsed(Level world, ServerPlayer player, Recipe<?> recipe) {
        if (!recipe.isSpecial() && world.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING) && !player.getRecipeBook().contains(recipe)) {
            return false;
        } else {
            this.setRecipeUsed(recipe);
            return true;
        }
    }
}
