package net.minecraft.data.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;

public class CommandsReport implements DataProvider {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private final DataGenerator generator;

    public CommandsReport(DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void run(HashCache cache) throws IOException {
        Path path = this.generator.getOutputFolder().resolve("reports/commands.json");
        CommandDispatcher<CommandSourceStack> commandDispatcher = (new Commands(Commands.CommandSelection.ALL)).getDispatcher();
        DataProvider.save(GSON, cache, ArgumentTypes.serializeNodeToJson(commandDispatcher, commandDispatcher.getRoot()), path);
    }

    @Override
    public String getName() {
        return "Command Syntax";
    }
}
