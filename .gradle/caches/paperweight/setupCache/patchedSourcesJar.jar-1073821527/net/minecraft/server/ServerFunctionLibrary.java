package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class ServerFunctionLibrary implements PreparableReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_EXTENSION = ".mcfunction";
    private static final int PATH_PREFIX_LENGTH = "functions/".length();
    private static final int PATH_SUFFIX_LENGTH = ".mcfunction".length();
    private volatile Map<ResourceLocation, CommandFunction> functions = ImmutableMap.of();
    private final TagLoader<CommandFunction> tagsLoader = new TagLoader<>(this::getFunction, "tags/functions");
    private volatile Map<ResourceLocation, Tag<CommandFunction>> tags = Map.of();
    private final int functionCompilationLevel;
    private final CommandDispatcher<CommandSourceStack> dispatcher;

    public Optional<CommandFunction> getFunction(ResourceLocation id) {
        return Optional.ofNullable(this.functions.get(id));
    }

    public Map<ResourceLocation, CommandFunction> getFunctions() {
        return this.functions;
    }

    public Tag<CommandFunction> getTag(ResourceLocation id) {
        return this.tags.getOrDefault(id, Tag.empty());
    }

    public Iterable<ResourceLocation> getAvailableTags() {
        return this.tags.keySet();
    }

    public ServerFunctionLibrary(int level, CommandDispatcher<CommandSourceStack> commandDispatcher) {
        this.functionCompilationLevel = level;
        this.dispatcher = commandDispatcher;
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier synchronizer, ResourceManager manager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        CompletableFuture<Map<ResourceLocation, Tag.Builder>> completableFuture = CompletableFuture.supplyAsync(() -> {
            return this.tagsLoader.load(manager);
        }, prepareExecutor);
        CompletableFuture<Map<ResourceLocation, CompletableFuture<CommandFunction>>> completableFuture2 = CompletableFuture.supplyAsync(() -> {
            return manager.listResources("functions", (path) -> {
                return path.endsWith(".mcfunction");
            });
        }, prepareExecutor).thenCompose((ids) -> {
            Map<ResourceLocation, CompletableFuture<CommandFunction>> map = Maps.newHashMap();
            CommandSourceStack commandSourceStack = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, (ServerLevel)null, this.functionCompilationLevel, "", TextComponent.EMPTY, (MinecraftServer)null, (Entity)null);

            for(ResourceLocation resourceLocation : ids) {
                String string = resourceLocation.getPath();
                ResourceLocation resourceLocation2 = new ResourceLocation(resourceLocation.getNamespace(), string.substring(PATH_PREFIX_LENGTH, string.length() - PATH_SUFFIX_LENGTH));
                map.put(resourceLocation2, CompletableFuture.supplyAsync(() -> {
                    List<String> list = readLines(manager, resourceLocation);
                    return CommandFunction.fromLines(resourceLocation2, this.dispatcher, commandSourceStack, list);
                }, prepareExecutor));
            }

            CompletableFuture<?>[] completableFutures = map.values().toArray(new CompletableFuture[0]);
            return CompletableFuture.allOf(completableFutures).handle((unused, ex) -> {
                return map;
            });
        });
        return completableFuture.thenCombine(completableFuture2, Pair::of).thenCompose(synchronizer::wait).thenAcceptAsync((intermediate) -> {
            Map<ResourceLocation, CompletableFuture<CommandFunction>> map = (Map)intermediate.getSecond();
            Builder<ResourceLocation, CommandFunction> builder = ImmutableMap.builder();
            map.forEach((id, functionFuture) -> {
                functionFuture.handle((function, ex) -> {
                    if (ex != null) {
                        LOGGER.error("Failed to load function {}", id, ex);
                    } else {
                        builder.put(id, function);
                    }

                    return null;
                }).join();
            });
            this.functions = builder.build();
            this.tags = this.tagsLoader.build((Map)intermediate.getFirst());
        }, applyExecutor);
    }

    private static List<String> readLines(ResourceManager resourceManager, ResourceLocation id) {
        try {
            Resource resource = resourceManager.getResource(id);

            List var3;
            try {
                var3 = IOUtils.readLines(resource.getInputStream(), StandardCharsets.UTF_8);
            } catch (Throwable var6) {
                if (resource != null) {
                    try {
                        resource.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (resource != null) {
                resource.close();
            }

            return var3;
        } catch (IOException var7) {
            throw new CompletionException(var7);
        }
    }
}
