package net.minecraft.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
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
        return this.server.getCommands().getDispatcher();
    }

    public void tick() {
        this.executeTagFunctions(this.ticking, TICK_FUNCTION_TAG);
        if (this.postReload) {
            this.postReload = false;
            Collection<CommandFunction> collection = this.library.getTag(LOAD_FUNCTION_TAG).getValues();
            this.executeTagFunctions(collection, LOAD_FUNCTION_TAG);
        }

    }

    private void executeTagFunctions(Collection<CommandFunction> functions, ResourceLocation label) {
        this.server.getProfiler().push(label::toString);

        for(CommandFunction commandFunction : functions) {
            this.execute(commandFunction, this.getGameLoopSender());
        }

        this.server.getProfiler().pop();
    }

    public int execute(CommandFunction function, CommandSourceStack source) {
        return this.execute(function, source, (ServerFunctionManager.TraceCallbacks)null);
    }

    public int execute(CommandFunction function, CommandSourceStack source, @Nullable ServerFunctionManager.TraceCallbacks tracer) {
        if (this.context != null) {
            if (tracer != null) {
                this.context.reportError(NO_RECURSIVE_TRACES.getString());
                return 0;
            } else {
                this.context.delayFunctionCall(function, source);
                return 0;
            }
        } else {
            int var4;
            try {
                this.context = new ServerFunctionManager.ExecutionContext(tracer);
                var4 = this.context.runTopCommand(function, source);
            } finally {
                this.context = null;
            }

            return var4;
        }
    }

    public void replaceLibrary(ServerFunctionLibrary loader) {
        this.library = loader;
        this.postReload(loader);
    }

    private void postReload(ServerFunctionLibrary loader) {
        this.ticking = ImmutableList.copyOf(loader.getTag(TICK_FUNCTION_TAG).getValues());
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

    class ExecutionContext {
        private int depth;
        @Nullable
        private final ServerFunctionManager.TraceCallbacks tracer;
        private final Deque<ServerFunctionManager.QueuedCommand> commandQueue = Queues.newArrayDeque();
        private final List<ServerFunctionManager.QueuedCommand> nestedCalls = Lists.newArrayList();

        ExecutionContext(@Nullable ServerFunctionManager.TraceCallbacks tracer) {
            this.tracer = tracer;
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
            CommandFunction.Entry[] entrys = function.getEntries();

            for(int k = entrys.length - 1; k >= 0; --k) {
                this.commandQueue.push(new ServerFunctionManager.QueuedCommand(source, 0, entrys[k]));
            }

            while(!this.commandQueue.isEmpty()) {
                try {
                    ServerFunctionManager.QueuedCommand queuedCommand = this.commandQueue.removeFirst();
                    ServerFunctionManager.this.server.getProfiler().push(queuedCommand::toString);
                    this.depth = queuedCommand.depth;
                    queuedCommand.execute(ServerFunctionManager.this, this.commandQueue, i, this.tracer);
                    if (!this.nestedCalls.isEmpty()) {
                        Lists.reverse(this.nestedCalls).forEach(this.commandQueue::addFirst);
                        this.nestedCalls.clear();
                    }
                } finally {
                    ServerFunctionManager.this.server.getProfiler().pop();
                }

                ++j;
                if (j >= i) {
                    return j;
                }
            }

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
            } catch (CommandSyntaxException var6) {
                if (tracer != null) {
                    tracer.onError(this.depth, var6.getRawMessage().getString());
                }
            } catch (Exception var7) {
                if (tracer != null) {
                    tracer.onError(this.depth, var7.getMessage());
                }
            }

        }

        @Override
        public String toString() {
            return this.entry.toString();
        }
    }

    public interface TraceCallbacks {
        void onCommand(int depth, String command);

        void onReturn(int depth, String command, int result);

        void onError(int depth, String message);

        void onCall(int depth, ResourceLocation function, int size);
    }
}
