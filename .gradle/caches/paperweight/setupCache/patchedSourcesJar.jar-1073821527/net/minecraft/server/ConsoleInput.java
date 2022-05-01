package net.minecraft.server;

import net.minecraft.commands.CommandSourceStack;

public class ConsoleInput {
    public final String msg;
    public final CommandSourceStack source;

    public ConsoleInput(String command, CommandSourceStack commandSource) {
        this.msg = command;
        this.source = commandSource;
    }
}
