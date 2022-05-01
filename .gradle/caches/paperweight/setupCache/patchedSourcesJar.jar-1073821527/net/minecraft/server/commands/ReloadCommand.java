package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;

public class ReloadCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void reloadPacks(Collection<String> dataPacks, CommandSourceStack source) {
        source.getServer().reloadResources(dataPacks).exceptionally((throwable) -> {
            LOGGER.warn("Failed to execute reload", throwable);
            source.sendFailure(new TranslatableComponent("commands.reload.failure"));
            return null;
        });
    }

    private static Collection<String> discoverNewPacks(PackRepository dataPackManager, WorldData saveProperties, Collection<String> enabledDataPacks) {
        dataPackManager.reload();
        Collection<String> collection = Lists.newArrayList(enabledDataPacks);
        Collection<String> collection2 = saveProperties.getDataPackConfig().getDisabled();

        for(String string : dataPackManager.getAvailableIds()) {
            if (!collection2.contains(string) && !collection.contains(string)) {
                collection.add(string);
            }
        }

        return collection;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("reload").requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            CommandSourceStack commandSourceStack = context.getSource();
            MinecraftServer minecraftServer = commandSourceStack.getServer();
            PackRepository packRepository = minecraftServer.getPackRepository();
            WorldData worldData = minecraftServer.getWorldData();
            Collection<String> collection = packRepository.getSelectedIds();
            Collection<String> collection2 = discoverNewPacks(packRepository, worldData, collection);
            commandSourceStack.sendSuccess(new TranslatableComponent("commands.reload.success"), true);
            reloadPacks(collection2, commandSourceStack);
            return 0;
        }));
    }
}
