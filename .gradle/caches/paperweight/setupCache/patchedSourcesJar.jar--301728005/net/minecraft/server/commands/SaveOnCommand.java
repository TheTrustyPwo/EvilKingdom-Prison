package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;

public class SaveOnCommand {
    private static final SimpleCommandExceptionType ERROR_ALREADY_ON = new SimpleCommandExceptionType(new TranslatableComponent("commands.save.alreadyOn"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("save-on").requires((source) -> {
            return source.hasPermission(4);
        }).executes((context) -> {
            CommandSourceStack commandSourceStack = context.getSource();
            boolean bl = false;

            for(ServerLevel serverLevel : commandSourceStack.getServer().getAllLevels()) {
                if (serverLevel != null && serverLevel.noSave) {
                    serverLevel.noSave = false;
                    bl = true;
                }
            }

            if (!bl) {
                throw ERROR_ALREADY_ON.create();
            } else {
                commandSourceStack.sendSuccess(new TranslatableComponent("commands.save.enabled"), true);
                return 1;
            }
        }));
    }
}
