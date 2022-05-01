package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
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

public class RecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    public Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes = ImmutableMap.of();
    public Map<ResourceLocation, Recipe<?>> byName = ImmutableMap.of();
    private boolean hasErrors;

    public RecipeManager() {
        super(GSON, "recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
        this.hasErrors = false;
        Map<RecipeType<?>, Builder<ResourceLocation, Recipe<?>>> map = Maps.newHashMap();
        Builder<ResourceLocation, Recipe<?>> builder = ImmutableMap.builder();

        for(Entry<ResourceLocation, JsonElement> entry : prepared.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();

            try {
                Recipe<?> recipe = fromJson(resourceLocation, GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
                map.computeIfAbsent(recipe.getType(), (recipeType) -> {
                    return ImmutableMap.builder();
                }).put(resourceLocation, recipe);
                builder.put(resourceLocation, recipe);
            } catch (IllegalArgumentException | JsonParseException var10) {
                LOGGER.error("Parsing error loading recipe {}", resourceLocation, var10);
            }
        }

        this.recipes = map.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entryx) -> {
            return entryx.getValue().build();
        }));
        this.byName = builder.build();
        LOGGER.info("Loaded {} recipes", (int)map.size());
    }

    public boolean hadErrorsLoading() {
        return this.hasErrors;
    }

    public <C extends Container, T extends Recipe<C>> Optional<T> getRecipeFor(RecipeType<T> type, C inventory, Level world) {
        return this.byType(type).values().stream().flatMap((recipe) -> {
            return type.tryMatch(recipe, world, inventory).stream();
        }).findFirst();
    }

    public <C extends Container, T extends Recipe<C>> List<T> getAllRecipesFor(RecipeType<T> type) {
        return this.byType(type).values().stream().map((recipe) -> {
            return recipe;
        }).collect(Collectors.toList());
    }

    public <C extends Container, T extends Recipe<C>> List<T> getRecipesFor(RecipeType<T> type, C inventory, Level world) {
        return this.byType(type).values().stream().flatMap((recipe) -> {
            return type.tryMatch(recipe, world, inventory).stream();
        }).sorted(Comparator.comparing((recipe) -> {
            return recipe.getResultItem().getDescriptionId();
        })).collect(Collectors.toList());
    }

    private <C extends Container, T extends Recipe<C>> Map<ResourceLocation, Recipe<C>> byType(RecipeType<T> type) {
        return this.recipes.getOrDefault(type, Collections.emptyMap());
    }

    public <C extends Container, T extends Recipe<C>> NonNullList<ItemStack> getRemainingItemsFor(RecipeType<T> type, C inventory, Level world) {
        Optional<T> optional = this.getRecipeFor(type, inventory, world);
        if (optional.isPresent()) {
            return optional.get().getRemainingItems(inventory);
        } else {
            NonNullList<ItemStack> nonNullList = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);

            for(int i = 0; i < nonNullList.size(); ++i) {
                nonNullList.set(i, inventory.getItem(i));
            }

            return nonNullList;
        }
    }

    public Optional<? extends Recipe<?>> byKey(ResourceLocation id) {
        return Optional.ofNullable(this.byName.get(id));
    }

    public Collection<Recipe<?>> getRecipes() {
        return this.recipes.values().stream().flatMap((map) -> {
            return map.values().stream();
        }).collect(Collectors.toSet());
    }

    public Stream<ResourceLocation> getRecipeIds() {
        return this.recipes.values().stream().flatMap((map) -> {
            return map.keySet().stream();
        });
    }

    public static Recipe<?> fromJson(ResourceLocation id, JsonObject json) {
        String string = GsonHelper.getAsString(json, "type");
        return Registry.RECIPE_SERIALIZER.getOptional(new ResourceLocation(string)).orElseThrow(() -> {
            return new JsonSyntaxException("Invalid or unsupported recipe type '" + string + "'");
        }).fromJson(id, json);
    }

    public void replaceRecipes(Iterable<Recipe<?>> recipes) {
        this.hasErrors = false;
        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> map = Maps.newHashMap();
        Builder<ResourceLocation, Recipe<?>> builder = ImmutableMap.builder();
        recipes.forEach((recipe) -> {
            Map<ResourceLocation, Recipe<?>> map2 = map.computeIfAbsent(recipe.getType(), (t) -> {
                return Maps.newHashMap();
            });
            ResourceLocation resourceLocation = recipe.getId();
            Recipe<?> recipe2 = map2.put(resourceLocation, recipe);
            builder.put(resourceLocation, recipe);
            if (recipe2 != null) {
                throw new IllegalStateException("Duplicate recipe ignored with ID " + resourceLocation);
            }
        });
        this.recipes = ImmutableMap.copyOf(map);
        this.byName = builder.build();
    }
}
