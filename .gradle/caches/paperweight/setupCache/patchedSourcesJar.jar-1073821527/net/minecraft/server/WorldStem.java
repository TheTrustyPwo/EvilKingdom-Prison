package net.minecraft.server;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;

public record WorldStem(CloseableResourceManager resourceManager, ReloadableServerResources dataPackResources, RegistryAccess.Frozen registryAccess, WorldData worldData) implements AutoCloseable {
    public static CompletableFuture<WorldStem> load(WorldStem.InitConfig functionLoaderConfig, WorldStem.DataPackConfigSupplier dataPackSettingsSupplier, WorldStem.WorldDataSupplier savePropertiesSupplier, Executor prepareExecutor, Executor applyExecutor) {
        try {
            DataPackConfig dataPackConfig = dataPackSettingsSupplier.get();
            DataPackConfig dataPackConfig2 = MinecraftServer.configurePackRepository(functionLoaderConfig.packRepository(), dataPackConfig, functionLoaderConfig.safeMode());
            List<PackResources> list = functionLoaderConfig.packRepository().openAllSelected();
            CloseableResourceManager closeableResourceManager = new MultiPackResourceManager(PackType.SERVER_DATA, list);
            Pair<WorldData, RegistryAccess.Frozen> pair = savePropertiesSupplier.get(closeableResourceManager, dataPackConfig2);
            WorldData worldData = pair.getFirst();
            RegistryAccess.Frozen frozen = pair.getSecond();
            return ReloadableServerResources.loadResources(closeableResourceManager, frozen, functionLoaderConfig.commandSelection(), functionLoaderConfig.functionCompilationLevel(), prepareExecutor, applyExecutor).whenComplete((dataPackContents, throwable) -> {
                if (throwable != null) {
                    closeableResourceManager.close();
                }

            }).thenApply((dataPackContents) -> {
                return new WorldStem(closeableResourceManager, dataPackContents, frozen, worldData);
            });
        } catch (Exception var12) {
            return CompletableFuture.failedFuture(var12);
        }
    }

    @Override
    public void close() {
        this.resourceManager.close();
    }

    public void updateGlobals() {
        this.dataPackResources.updateRegistryTags(this.registryAccess);
    }

    @FunctionalInterface
    public interface DataPackConfigSupplier extends Supplier<DataPackConfig> {
        static WorldStem.DataPackConfigSupplier loadFromWorld(LevelStorageSource.LevelStorageAccess session) {
            return () -> {
                DataPackConfig dataPackConfig = session.getDataPacks();
                if (dataPackConfig == null) {
                    throw new IllegalStateException("Failed to load data pack config");
                } else {
                    return dataPackConfig;
                }
            };
        }
    }

    public static record InitConfig(PackRepository packRepository, Commands.CommandSelection commandSelection, int functionCompilationLevel, boolean safeMode) {
    }

    @FunctionalInterface
    public interface WorldDataSupplier {
        Pair<WorldData, RegistryAccess.Frozen> get(ResourceManager resourceManager, DataPackConfig dataPackSettings);

        static WorldStem.WorldDataSupplier loadFromWorld(LevelStorageSource.LevelStorageAccess session) {
            return (resourceManager, dataPackSettings) -> {
                RegistryAccess.Writable writable = RegistryAccess.builtinCopy();
                DynamicOps<Tag> dynamicOps = RegistryOps.createAndLoad(NbtOps.INSTANCE, writable, resourceManager);
                WorldData worldData = session.getDataTag(dynamicOps, dataPackSettings, writable.allElementsLifecycle());
                if (worldData == null) {
                    throw new IllegalStateException("Failed to load world");
                } else {
                    return Pair.of(worldData, writable.freeze());
                }
            };
        }
    }
}
