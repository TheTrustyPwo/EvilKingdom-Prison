package net.minecraft.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.GameRules;

public class ServerFunctionManager {

    private static final Component NO_RECURSIVE_TRACES = new TranslatableComponent("commands.debug.function.noRecursion");
    private static final ResourceLocation TICK_FUNCTION_TAG = new ResourceLocation("tick");
    private static final ResourceLocation LOAD_FUNCTION_TAG = new ResourceLocation("load");
    final MinecraftServer server;
    @Nullable
    private ServerFunctionManager.ExecutionContext context;
    private List<CommandFunction> ticking = ImmutableList.of();
    private boolean postReload;
    private ServerFunctionLibrary library;

    public ServerFunctionManager(MinecraftServer server, ServerFunctionLibrary loader) {
        this.server = server;
        this.library = loader;
        this.postReload(loader);
    }

    public int getCommandLimit() {
        return this.server.getGameRules().getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH);
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return this.server.vanillaCommandDispatcher.getDispatcher(); // CraftBukkit
    }

    public void tick() {
        this.executeTagFunctions(this.ticking, ServerFunctionManager.TICK_FUNCTION_TAG);
        if (this.postReload) {
            this.postReload = false;
            Collection<CommandFunction> collection = this.library.getTag(ServerFunctionManager.LOAD_FUNCTION_TAG).getValues();

            this.executeTagFunctions(collection, ServerFunctionManager.LOAD_FUNCTION_TAG);
        }

    }

    private void executeTagFunctions(Collection<CommandFunction> functions, ResourceLocation label) {
        ProfilerFiller gameprofilerfiller = this.server.getProfiler();

        Objects.requireNonNull(label);
        gameprofilerfiller.push(label::toString);
        Iterator iterator = functions.iterator();

        while (iterator.hasNext()) {
            CommandFunction customfunction = (CommandFunction) iterator.next();

            this.execute(customfunction, this.getGameLoopSender());
        }

        this.server.getProfiler().pop();
    }

    public int execute(CommandFunction function, CommandSourceStack source) {
        return this.execute(function, source, (ServerFunctionManager.TraceCallbacks) null);
    }

    public int execute(CommandFunction function, CommandSourceStack source, @Nullable ServerFunctionManager.TraceCallbacks tracer) {
        if (this.context != null) {
            if (tracer != null) {
                this.context.reportError(ServerFunctionManager.NO_RECURSIVE_TRACES.getString());
                return 0;
            } else {
                this.context.delayFunctionCall(function, source);
                return 0;
            }
        } else {
            int i;

            try (co.aikar.timings.Timing timing = function.getTiming().startTiming()) { // Paper
                this.context = new ServerFunctionManager.ExecutionContext(tracer);
                i = this.context.runTopCommand(function, source);
            } finally {
                this.context = null;
            }

            return i;
        }
    }

    public void replaceLibrary(ServerFunctionLibrary loader) {
        this.library = loader;
        this.postReload(loader);
    }

    private void postReload(ServerFunctionLibrary loader) {
        this.ticking = ImmutableList.copyOf(loader.getTag(ServerFunctionManager.TICK_FUNCTION_TAG).getValues());
        this.postReload = true;
    }

    public CommandSourceStack getGameLoopSender() {
        return this.server.createCommandSourceStack().withPermission(2).withSuppressedOutput();
    }

    public Optional<CommandFunction> get(ResourceLocation id) {
        return this.library.getFunction(id);
    }

    public Tag<CommandFunction> getTag(ResourceLocation id) {
        return this.library.getTag(id);
    }

    public Iterable<ResourceLocation> getFunctionNames() {
        return this.library.getFunctions().keySet();
    }

    public Iterable<ResourceLocation> getTagNames() {
        return this.library.getAvailableTags();
    }

    public interface TraceCallbacks {

        void onCommand(int depth, String command);

        void onReturn(int depth, String command, int result);

        void onError(int depth, String message);

        void onCall(int depth, ResourceLocation function, int size);
    }

    private class ExecutionContext {

        private int depth;
        @Nullable
        private final ServerFunctionManager.TraceCallbacks tracer;
        private final Deque<ServerFunctionManager.QueuedCommand> commandQueue = Queues.newArrayDeque();
        private final List<ServerFunctionManager.QueuedCommand> nestedCalls = Lists.newArrayList();

        ExecutionContext(@Nullable ServerFunctionManager.TraceCallbacks customfunctiondata_c) {
            this.tracer = customfunctiondata_c;
        }

        void delayFunctionCall(CommandFunction function, CommandSourceStack source) {
            int i = ServerFunctionManager.this.getCommandLimit();

            if (this.commandQueue.size() + this.nestedCalls.size() < i) {
                this.nestedCalls.add(new ServerFunctionManager.QueuedCommand(source, this.depth, new CommandFunction.FunctionEntry(function)));
            }

        }

        int runTopCommand(CommandFunction function, CommandSourceStack source) {
            int i = ServerFunctionManager.this.getCommandLimit();
            int j = 0;
            CommandFunction.Entry[] acustomfunction_c = function.getEntries();

            for (int k = acustomfunction_c.length - 1; k >= 0; --k) {
                this.commandQueue.push(new ServerFunctionManager.QueuedCommand(source, 0, acustomfunction_c[k]));
            }

            do {
                if (this.commandQueue.isEmpty()) {
                    return j;
                }

                try {
                    ServerFunctionManager.QueuedCommand customfunctiondata_b = (ServerFunctionManager.QueuedCommand) this.commandQueue.removeFirst();
                    ProfilerFiller gameprofilerfiller = ServerFunctionManager.this.server.getProfiler();

                    Objects.requireNonNull(customfunctiondata_b);
                    gameprofilerfiller.push(customfunctiondata_b::toString);
                    this.depth = customfunctiondata_b.depth;
                    customfunctiondata_b.execute(ServerFunctionManager.this, this.commandQueue, i, this.tracer);
                    if (!this.nestedCalls.isEmpty()) {
                        List list = Lists.reverse(this.nestedCalls);
                        Deque deque = this.commandQueue;

                        Objects.requireNonNull(this.commandQueue);
                        list.forEach(deque::addFirst);
                        this.nestedCalls.clear();
                    }
                } finally {
                    ServerFunctionManager.this.server.getProfiler().pop();
                }

                ++j;
            } while (j < i);

            return j;
        }

        public void reportError(String message) {
            if (this.tracer != null) {
                this.tracer.onError(this.depth, message);
            }

        }
    }

    public static class QueuedCommand {

        private final CommandSourceStack sender;
        final int depth;
        private final CommandFunction.Entry entry;

        public QueuedCommand(CommandSourceStack source, int depth, CommandFunction.Entry element) {
            this.sender = source;
            this.depth = depth;
            this.entry = element;
        }

        public void execute(ServerFunctionManager manager, Deque<ServerFunctionManager.QueuedCommand> entries, int maxChainLength, @Nullable ServerFunctionManager.TraceCallbacks tracer) {
            try {
                this.entry.execute(manager, this.sender, entries, maxChainLength, this.depth, tracer);
            } catch (CommandSyntaxException commandsyntaxexception) {
                if (tracer != null) {
                    tracer.onError(this.depth, commandsyntaxexception.getRawMessage().getString());
                }
            } catch (Exception exception) {
                if (tracer != null) {
                    tracer.onError(this.depth, exception.getMessage());
                }
            }

        }

        public String toString() {
            return this.entry.toString();
        }
    }
}
