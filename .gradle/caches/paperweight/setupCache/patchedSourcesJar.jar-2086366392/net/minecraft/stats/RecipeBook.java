package net.minecraft.stats;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeBook {
    public final Set<ResourceLocation> known = Sets.newHashSet();
    protected final Set<ResourceLocation> highlight = Sets.newHashSet();
    private final RecipeBookSettings bookSettings = new RecipeBookSettings();

    public void copyOverData(RecipeBook book) {
        this.known.clear();
        this.highlight.clear();
        this.bookSettings.replaceFrom(book.bookSettings);
        this.known.addAll(book.known);
        this.highlight.addAll(book.highlight);
    }

    public void add(Recipe<?> recipe) {
        if (!recipe.isSpecial()) {
            this.add(recipe.getId());
        }

    }

    protected void add(ResourceLocation id) {
        this.known.add(id);
    }

    public boolean contains(@Nullable Recipe<?> recipe) {
        return recipe == null ? false : this.known.contains(recipe.getId());
    }

    public boolean contains(ResourceLocation id) {
        return this.known.contains(id);
    }

    public void remove(Recipe<?> recipe) {
        this.remove(recipe.getId());
    }

    protected void remove(ResourceLocation id) {
        this.known.remove(id);
        this.highlight.remove(id);
    }

    public boolean willHighlight(Recipe<?> recipe) {
        return this.highlight.contains(recipe.getId());
    }

    public void removeHighlight(Recipe<?> recipe) {
        this.highlight.remove(recipe.getId());
    }

    public void addHighlight(Recipe<?> recipe) {
        this.addHighlight(recipe.getId());
    }

    protected void addHighlight(ResourceLocation id) {
        this.highlight.add(id);
    }

    public boolean isOpen(RecipeBookType category) {
        return this.bookSettings.isOpen(category);
    }

    public void setOpen(RecipeBookType category, boolean open) {
        this.bookSettings.setOpen(category, open);
    }

    public boolean isFiltering(RecipeBookMenu<?> handler) {
        return this.isFiltering(handler.getRecipeBookType());
    }

    public boolean isFiltering(RecipeBookType category) {
        return this.bookSettings.isFiltering(category);
    }

    public void setFiltering(RecipeBookType category, boolean filteringCraftable) {
        this.bookSettings.setFiltering(category, filteringCraftable);
    }

    public void setBookSettings(RecipeBookSettings options) {
        this.bookSettings.replaceFrom(options);
    }

    public RecipeBookSettings getBookSettings() {
        return this.bookSettings.copy();
    }

    public void setBookSetting(RecipeBookType category, boolean guiOpen, boolean filteringCraftable) {
        this.bookSettings.setOpen(category, guiOpen);
        this.bookSettings.setFiltering(category, filteringCraftable);
    }
}
