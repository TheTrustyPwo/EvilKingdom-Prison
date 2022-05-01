package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap; // CraftBukkit

public class RecipeManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    public Map<RecipeType<?>, Object2ObjectLinkedOpenHashMap<ResourceLocation, Recipe<?>>> recipes = ImmutableMap.of(); // CraftBukkit
    public Map<ResourceLocation, Recipe<?>> byName = ImmutableMap.of();
    private boolean hasErrors;

    public RecipeManager() {
        super(RecipeManager.GSON, "recipes");
    }

    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        this.hasErrors = false;
        // CraftBukkit start - SPIGOT-5667 make sure all types are populated and mutable
        Map<RecipeType<?>, Object2ObjectLinkedOpenHashMap<ResourceLocation, Recipe<?>>> map1 = Maps.newHashMap();
        for (RecipeType<?> recipeType : Registry.RECIPE_TYPE) {
            map1.put(recipeType, new Object2ObjectLinkedOpenHashMap<>());
        }
        // CraftBukkit end
        Builder<ResourceLocation, Recipe<?>> builder = ImmutableMap.builder();
        Iterator iterator = prepared.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<ResourceLocation, JsonElement> entry = (Entry) iterator.next();
            ResourceLocation minecraftkey = (ResourceLocation) entry.getKey();

            try {
                Recipe<?> irecipe = RecipeManager.fromJson(minecraftkey, GsonHelper.convertToJsonObject((JsonElement) entry.getValue(), "top element"));

                // CraftBukkit start
                (map1.computeIfAbsent(irecipe.getType(), (recipes) -> {
                    return new Object2ObjectLinkedOpenHashMap<>();
                    // CraftBukkit end
                })).put(minecraftkey, irecipe);
                builder.put(minecraftkey, irecipe);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                RecipeManager.LOGGER.error("Parsing error loading recipe {}", minecraftkey, jsonparseexception);
            }
        }

        this.recipes = (Map) map1.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entry1) -> {
            return entry1.getValue(); // CraftBukkit // Paper - decompile fix - *shrugs internally* // todo: is this needed anymore?
        }));
        this.byName = Maps.newHashMap(builder.build()); // CraftBukkit
        RecipeManager.LOGGER.info("Loaded {} recipes", map1.size());
    }

    // CraftBukkit start
    public void addRecipe(Recipe<?> irecipe) {
        org.spigotmc.AsyncCatcher.catchOp("Recipe Add"); // Spigot
        Object2ObjectLinkedOpenHashMap<ResourceLocation, Recipe<?>> map = this.recipes.get(irecipe.getType()); // CraftBukkit

        if (this.byName.containsKey(irecipe.getId()) || map.containsKey(irecipe.getId())) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + irecipe.getId());
        } else {
            map.putAndMoveToFirst(irecipe.getId(), irecipe); // CraftBukkit - SPIGOT-4638: last recipe gets priority
            this.byName.put(irecipe.getId(), irecipe);
        }
    }
    // CraftBukkit end

    public boolean hadErrorsLoading() {
        return this.hasErrors;
    }

    public <C extends Container, T extends Recipe<C>> Optional<T> getRecipeFor(RecipeType<T> type, C inventory, Level world) {
        // CraftBukkit start
        Optional<T> recipe = this.byType(type).values().stream().flatMap((irecipe) -> {
            return type.tryMatch(irecipe, world, inventory).stream();
        }).findFirst();
        inventory.setCurrentRecipe(recipe.orElse(null)); // CraftBukkit - Clear recipe when no recipe is found
        // CraftBukkit end
        return recipe;
    }

    public <C extends Container, T extends Recipe<C>> List<T> getAllRecipesFor(RecipeType<T> type) {
        return (List) this.byType(type).values().stream().map((irecipe) -> {
            return irecipe;
        }).collect(Collectors.toList());
    }

    public <C extends Container, T extends Recipe<C>> List<T> getRecipesFor(RecipeType<T> type, C inventory, Level world) {
        return (List) this.byType(type).values().stream().flatMap((irecipe) -> {
            return type.tryMatch(irecipe, world, inventory).stream();
        }).sorted(Comparator.comparing((irecipe) -> {
            return irecipe.getResultItem().getDescriptionId();
        })).collect(Collectors.toList());
    }

    private <C extends Container, T extends Recipe<C>> Map<ResourceLocation, Recipe<C>> byType(RecipeType<T> type) {
        return (Map) this.recipes.getOrDefault(type, new Object2ObjectLinkedOpenHashMap<>()); // CraftBukkit
    }

    public <C extends Container, T extends Recipe<C>> NonNullList<ItemStack> getRemainingItemsFor(RecipeType<T> type, C inventory, Level world) {
        Optional<T> optional = this.getRecipeFor(type, inventory, world);

        if (optional.isPresent()) {
            return ((Recipe) optional.get()).getRemainingItems(inventory);
        } else {
            NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);

            for (int i = 0; i < nonnulllist.size(); ++i) {
                nonnulllist.set(i, inventory.getItem(i));
            }

            return nonnulllist;
        }
    }

    public Optional<? extends Recipe<?>> byKey(ResourceLocation id) {
        return Optional.ofNullable(this.byName.get(id)); // CraftBukkit - decompile error
    }

    public Collection<Recipe<?>> getRecipes() {
        return (Collection) this.recipes.values().stream().flatMap((map) -> {
            return map.values().stream();
        }).collect(Collectors.toSet());
    }

    public Stream<ResourceLocation> getRecipeIds() {
        return this.recipes.values().stream().flatMap((map) -> {
            return map.keySet().stream();
        });
    }

    public static Recipe<?> fromJson(ResourceLocation id, JsonObject json) {
        String s = GsonHelper.getAsString(json, "type");

        return ((RecipeSerializer) Registry.RECIPE_SERIALIZER.getOptional(new ResourceLocation(s)).orElseThrow(() -> {
            return new JsonSyntaxException("Invalid or unsupported recipe type '" + s + "'");
        })).fromJson(id, json);
    }

    public void replaceRecipes(Iterable<Recipe<?>> recipes) {
        this.hasErrors = false;
        Map<RecipeType<?>, Object2ObjectLinkedOpenHashMap<ResourceLocation, Recipe<?>>> map = Maps.newHashMap(); // CraftBukkit
        Builder<ResourceLocation, Recipe<?>> builder = ImmutableMap.builder();

        recipes.forEach((irecipe) -> {
            Map<ResourceLocation, Recipe<?>> map1 = (Map) map.computeIfAbsent(irecipe.getType(), (recipes_) -> { // Paper - remap fix
                return new Object2ObjectLinkedOpenHashMap<>(); // CraftBukkit
            });
            ResourceLocation minecraftkey = irecipe.getId();
            Recipe<?> irecipe1 = (Recipe) map1.put(minecraftkey, irecipe);

            builder.put(minecraftkey, irecipe);
            if (irecipe1 != null) {
                throw new IllegalStateException("Duplicate recipe ignored with ID " + minecraftkey);
            }
        });
        this.recipes = ImmutableMap.copyOf(map);
        this.byName = Maps.newHashMap(builder.build()); // CraftBukkit
    }

    // CraftBukkit start
    public boolean removeRecipe(ResourceLocation mcKey) {
        for (Object2ObjectLinkedOpenHashMap<ResourceLocation, Recipe<?>> recipes : this.recipes.values()) {
            recipes.remove(mcKey);
        }

        return this.byName.remove(mcKey) != null;
    }

    public void clearRecipes() {
        this.recipes = Maps.newHashMap();

        for (RecipeType<?> recipeType : Registry.RECIPE_TYPE) {
            this.recipes.put(recipeType, new Object2ObjectLinkedOpenHashMap<>());
        }

        this.byName = Maps.newHashMap();
    }
    // CraftBukkit end
}
