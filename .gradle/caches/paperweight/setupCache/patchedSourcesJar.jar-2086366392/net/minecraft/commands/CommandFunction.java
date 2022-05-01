package net.minecraft.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionManager;

public class CommandFunction {
    private final CommandFunction.Entry[] entries;
    final ResourceLocation id;
    // Paper start
    public co.aikar.timings.Timing timing;
    public co.aikar.timings.Timing getTiming() {
        if (timing == null) {
            timing = co.aikar.timings.MinecraftTimings.getCommandFunctionTiming(this);
        }
        return timing;
    }
    // Paper end

    public CommandFunction(ResourceLocation id, CommandFunction.Entry[] elements) {
        this.id = id;
        this.entries = elements;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public CommandFunction.Entry[] getEntries() {
        return this.entries;
    }

    public static CommandFunction fromLines(ResourceLocation id, CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, List<String> lines) {
        List<CommandFunction.Entry> list = Lists.newArrayListWithCapacity(lines.size());

        for(int i = 0; i < lines.size(); ++i) {
            int j = i + 1;
            String string = lines.get(i).trim();
            StringReader stringReader = new StringReader(string);
            if (stringReader.canRead() && stringReader.peek() != '#') {
                if (stringReader.peek() == '/') {
                    stringReader.skip();
                    if (stringReader.peek() == '/') {
                        throw new IllegalArgumentException("Unknown or invalid command '" + string + "' on line " + j + " (if you intended to make a comment, use '#' not '//')");
                    }

                    String string2 = stringReader.readUnquotedString();
                    throw new IllegalArgumentException("Unknown or invalid command '" + string + "' on line " + j + " (did you mean '" + string2 + "'? Do not use a preceding forwards slash.)");
                }

                try {
                    ParseResults<CommandSourceStack> parseResults = dispatcher.parse(stringReader, source);
                    if (parseResults.getReader().canRead()) {
                        throw Commands.getParseException(parseResults);
                    }

                    list.add(new CommandFunction.CommandEntry(parseResults));
                } catch (CommandSyntaxException var10) {
                    throw new IllegalArgumentException("Whilst parsing command on line " + j + ": " + var10.getMessage());
                }
            }
        }

        return new CommandFunction(id, list.toArray(new CommandFunction.Entry[0]));
    }

    public static class CacheableFunction {
        public static final CommandFunction.CacheableFunction NONE = new CommandFunction.CacheableFunction((ResourceLocation)null);
        @Nullable
        private final ResourceLocation id;
        private boolean resolved;
        private Optional<CommandFunction> function = Optional.empty();

        public CacheableFunction(@Nullable ResourceLocation id) {
            this.id = id;
        }

        public CacheableFunction(CommandFunction function) {
            this.resolved = true;
            this.id = null;
            this.function = Optional.of(function);
        }

        public Optional<CommandFunction> get(ServerFunctionManager manager) {
            if (!this.resolved) {
                if (this.id != null) {
                    this.function = manager.get(this.id);
                }

                this.resolved = true;
            }

            return this.function;
        }

        @Nullable
        public ResourceLocation getId() {
            return this.function.map((f) -> {
                return f.id;
            }).orElse(this.id);
        }
    }

    public static class CommandEntry implements CommandFunction.Entry {
        private final ParseResults<CommandSourceStack> parse;

        public CommandEntry(ParseResults<CommandSourceStack> parsed) {
            this.parse = parsed;
        }

        @Override
        public void execute(ServerFunctionManager manager, CommandSourceStack source, Deque<ServerFunctionManager.QueuedCommand> entries, int maxChainLength, int depth, @Nullable ServerFunctionManager.TraceCallbacks tracer) throws CommandSyntaxException {
            if (tracer != null) {
                String string = this.parse.getReader().getString();
                tracer.onCommand(depth, string);
                int i = this.execute(manager, source);
                tracer.onReturn(depth, string, i);
            } else {
                this.execute(manager, source);
            }

        }

        private int execute(ServerFunctionManager manager, CommandSourceStack source) throws CommandSyntaxException {
            return manager.getDispatcher().execute(new ParseResults<>(this.parse.getContext().withSource(source), this.parse.getReader(), this.parse.getExceptions()));
        }

        @Override
        public String toString() {
            return this.parse.getReader().getString();
        }
    }

    @FunctionalInterface
    public interface Entry {
        void execute(ServerFunctionManager manager, CommandSourceStack source, Deque<ServerFunctionManager.QueuedCommand> entries, int maxChainLength, int depth, @Nullable ServerFunctionManager.TraceCallbacks tracer) throws CommandSyntaxException;
    }

    public static class FunctionEntry implements CommandFunction.Entry {
        private final CommandFunction.CacheableFunction function;

        public FunctionEntry(CommandFunction function) {
            this.function = new CommandFunction.CacheableFunction(function);
        }

        @Override
        public void execute(ServerFunctionManager manager, CommandSourceStack source, Deque<ServerFunctionManager.QueuedCommand> entries, int maxChainLength, int depth, @Nullable ServerFunctionManager.TraceCallbacks tracer) {
            Util.ifElse(this.function.get(manager), (f) -> {
                CommandFunction.Entry[] entrys = f.getEntries();
                if (tracer != null) {
                    tracer.onCall(depth, f.getId(), entrys.length);
                }

                int k = maxChainLength - entries.size();
                int l = Math.min(entrys.length, k);

                for(int m = l - 1; m >= 0; --m) {
                    entries.addFirst(new ServerFunctionManager.QueuedCommand(source, depth + 1, entrys[m]));
                }

            }, () -> {
                if (tracer != null) {
                    tracer.onCall(depth, this.function.getId(), -1);
                }

            });
        }

        @Override
        public String toString() {
            return "function " + this.function.getId();
        }
    }
}
