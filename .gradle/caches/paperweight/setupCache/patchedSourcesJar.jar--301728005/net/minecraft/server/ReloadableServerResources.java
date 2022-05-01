package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.ItemModifierManager;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.PredicateManager;
import org.slf4j.Logger;

public class ReloadableServerResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CompletableFuture<Unit> DATA_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
    public Commands commands;
    private final RecipeManager recipes = new RecipeManager();
    private final TagManager tagManager;
    private final PredicateManager predicateManager = new PredicateManager();
    private final LootTables lootTables = new LootTables(this.predicateManager);
    private final ItemModifierManager itemModifierManager = new ItemModifierManager(this.predicateManager, this.lootTables);
    private final ServerAdvancementManager advancements = new ServerAdvancementManager(this.predicateManager);
    private final ServerFunctionLibrary functionLibrary;

    public ReloadableServerResources(RegistryAccess.Frozen dynamicRegistryManager, Commands.CommandSelection commandEnvironment, int functionPermissionLevel) {
        this.tagManager = new TagManager(dynamicRegistryManager);
        this.commands = new Commands(commandEnvironment);
        this.functionLibrary = new ServerFunctionLibrary(functionPermissionLevel, this.commands.getDispatcher());
    }

    public ServerFunctionLibrary getFunctionLibrary() {
        return this.functionLibrary;
    }

    public PredicateManager getPredicateManager() {
        return this.predicateManager;
    }

    public LootTables getLootTables() {
        return this.lootTables;
    }

    public ItemModifierManager getItemModifierManager() {
        return this.itemModifierManager;
    }

    public RecipeManager getRecipeManager() {
        return this.recipes;
    }

    public Commands getCommands() {
        return this.commands;
    }

    public ServerAdvancementManager getAdvancements() {
        return this.advancements;
    }

    public List<PreparableReloadListener> listeners() {
        return List.of(this.tagManager, this.predicateManager, this.recipes, this.lootTables, this.itemModifierManager, this.functionLibrary, this.advancements);
    }

    public static CompletableFuture<ReloadableServerResources> loadResources(ResourceManager manager, RegistryAccess.Frozen dynamicRegistryManager, Commands.CommandSelection commandEnvironment, int functionPermissionLevel, Executor prepareExecutor, Executor applyExecutor) {
        ReloadableServerResources reloadableServerResources = new ReloadableServerResources(dynamicRegistryManager, commandEnvironment, functionPermissionLevel);
        return SimpleReloadInstance.create(manager, reloadableServerResources.listeners(), prepareExecutor, applyExecutor, DATA_RELOAD_INITIAL_TASK, LOGGER.isDebugEnabled()).done().thenApply((object) -> {
            return reloadableServerResources;
        });
    }

    public void updateRegistryTags(RegistryAccess dynamicRegistryManager) {
        this.tagManager.getResult().forEach((tags) -> {
            updateRegistryTags(dynamicRegistryManager, tags);
        });
        Blocks.rebuildCache();
    }

    private static <T> void updateRegistryTags(RegistryAccess dynamicRegistryManager, TagManager.LoadResult<T> tags) {
        ResourceKey<? extends Registry<T>> resourceKey = tags.key();
        Map<TagKey<T>, List<Holder<T>>> map = tags.tags().entrySet().stream().collect(Collectors.toUnmodifiableMap((entry) -> {
            return TagKey.create(resourceKey, entry.getKey());
        }, (entry) -> {
            return entry.getValue().getValues();
        }));
        dynamicRegistryManager.registryOrThrow(resourceKey).bindTags(map);
    }
}
