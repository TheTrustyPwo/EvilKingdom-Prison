package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.ProfileResults;
import org.slf4j.Logger;

public class DebugCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType ERROR_NOT_RUNNING = new SimpleCommandExceptionType(new TranslatableComponent("commands.debug.notRunning"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_RUNNING = new SimpleCommandExceptionType(new TranslatableComponent("commands.debug.alreadyRunning"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("debug").requires((source) -> {
            return source.hasPermission(3);
        }).then(Commands.literal("start").executes((context) -> {
            return start(context.getSource());
        })).then(Commands.literal("stop").executes((context) -> {
            return stop(context.getSource());
        })).then(Commands.literal("function").requires((commandSourceStack) -> {
            return commandSourceStack.hasPermission(3);
        }).then(Commands.argument("name", FunctionArgument.functions()).suggests(FunctionCommand.SUGGEST_FUNCTION).executes((context) -> {
            return traceFunction(context.getSource(), FunctionArgument.getFunctions(context, "name"));
        }))));
    }

    private static int start(CommandSourceStack source) throws CommandSyntaxException {
        MinecraftServer minecraftServer = source.getServer();
        if (minecraftServer.isTimeProfilerRunning()) {
            throw ERROR_ALREADY_RUNNING.create();
        } else {
            minecraftServer.startTimeProfiler();
            source.sendSuccess(new TranslatableComponent("commands.debug.started"), true);
            return 0;
        }
    }

    private static int stop(CommandSourceStack source) throws CommandSyntaxException {
        MinecraftServer minecraftServer = source.getServer();
        if (!minecraftServer.isTimeProfilerRunning()) {
            throw ERROR_NOT_RUNNING.create();
        } else {
            ProfileResults profileResults = minecraftServer.stopTimeProfiler();
            double d = (double)profileResults.getNanoDuration() / (double)TimeUtil.NANOSECONDS_PER_SECOND;
            double e = (double)profileResults.getTickDuration() / d;
            source.sendSuccess(new TranslatableComponent("commands.debug.stopped", String.format(Locale.ROOT, "%.2f", d), profileResults.getTickDuration(), String.format("%.2f", e)), true);
            return (int)e;
        }
    }

    private static int traceFunction(CommandSourceStack source, Collection<CommandFunction> functions) {
        int i = 0;
        MinecraftServer minecraftServer = source.getServer();
        String string = "debug-trace-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + ".txt";

        try {
            Path path = minecraftServer.getFile("debug").toPath();
            Files.createDirectories(path);
            Writer writer = Files.newBufferedWriter(path.resolve(string), StandardCharsets.UTF_8);

            try {
                PrintWriter printWriter = new PrintWriter(writer);

                for(CommandFunction commandFunction : functions) {
                    printWriter.println((Object)commandFunction.getId());
                    DebugCommand.Tracer tracer = new DebugCommand.Tracer(printWriter);
                    i += source.getServer().getFunctions().execute(commandFunction, source.withSource(tracer).withMaximumPermission(2), tracer);
                }
            } catch (Throwable var12) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                    }
                }

                throw var12;
            }

            if (writer != null) {
                writer.close();
            }
        } catch (IOException | UncheckedIOException var13) {
            LOGGER.warn("Tracing failed", (Throwable)var13);
            source.sendFailure(new TranslatableComponent("commands.debug.function.traceFailed"));
        }

        if (functions.size() == 1) {
            source.sendSuccess(new TranslatableComponent("commands.debug.function.success.single", i, functions.iterator().next().getId(), string), true);
        } else {
            source.sendSuccess(new TranslatableComponent("commands.debug.function.success.multiple", i, functions.size(), string), true);
        }

        return i;
    }

    static class Tracer implements CommandSource, ServerFunctionManager.TraceCallbacks {
        public static final int INDENT_OFFSET = 1;
        private final PrintWriter output;
        private int lastIndent;
        private boolean waitingForResult;

        Tracer(PrintWriter writer) {
            this.output = writer;
        }

        private void indentAndSave(int width) {
            this.printIndent(width);
            this.lastIndent = width;
        }

        private void printIndent(int width) {
            for(int i = 0; i < width + 1; ++i) {
                this.output.write("    ");
            }

        }

        private void newLine() {
            if (this.waitingForResult) {
                this.output.println();
                this.waitingForResult = false;
            }

        }

        @Override
        public void onCommand(int depth, String command) {
            this.newLine();
            this.indentAndSave(depth);
            this.output.print("[C] ");
            this.output.print(command);
            this.waitingForResult = true;
        }

        @Override
        public void onReturn(int depth, String command, int result) {
            if (this.waitingForResult) {
                this.output.print(" -> ");
                this.output.println(result);
                this.waitingForResult = false;
            } else {
                this.indentAndSave(depth);
                this.output.print("[R = ");
                this.output.print(result);
                this.output.print("] ");
                this.output.println(command);
            }

        }

        @Override
        public void onCall(int depth, ResourceLocation function, int size) {
            this.newLine();
            this.indentAndSave(depth);
            this.output.print("[F] ");
            this.output.print((Object)function);
            this.output.print(" size=");
            this.output.println(size);
        }

        @Override
        public void onError(int depth, String message) {
            this.newLine();
            this.indentAndSave(depth + 1);
            this.output.print("[E] ");
            this.output.print(message);
        }

        @Override
        public void sendMessage(Component message, UUID sender) {
            this.newLine();
            this.printIndent(this.lastIndent + 1);
            this.output.print("[M] ");
            if (sender != Util.NIL_UUID) {
                this.output.print((Object)sender);
                this.output.print(": ");
            }

            this.output.println(message.getString());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }

        @Override
        public boolean alwaysAccepts() {
            return true;
        }
    }
}
