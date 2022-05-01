package net.minecraft.world.level;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicLike;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public class GameRules {
    public static final int DEFAULT_RANDOM_TICK_SPEED = 3;
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<GameRules.Key<?>, GameRules.Type<?>> GAME_RULE_TYPES = Maps.newTreeMap(Comparator.comparing((key) -> {
        return key.id;
    }));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOFIRETICK = register("doFireTick", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_MOBGRIEFING = register("mobGriefing", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_KEEPINVENTORY = register("keepInventory", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOMOBSPAWNING = register("doMobSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOMOBLOOT = register("doMobLoot", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOBLOCKDROPS = register("doTileDrops", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOENTITYDROPS = register("doEntityDrops", GameRules.Category.DROPS, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_COMMANDBLOCKOUTPUT = register("commandBlockOutput", GameRules.Category.CHAT, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_NATURAL_REGENERATION = register("naturalRegeneration", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DAYLIGHT = register("doDaylightCycle", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_LOGADMINCOMMANDS = register("logAdminCommands", GameRules.Category.CHAT, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_SHOWDEATHMESSAGES = register("showDeathMessages", GameRules.Category.CHAT, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.IntegerValue> RULE_RANDOMTICKING = register("randomTickSpeed", GameRules.Category.UPDATES, GameRules.IntegerValue.create(3));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_SENDCOMMANDFEEDBACK = register("sendCommandFeedback", GameRules.Category.CHAT, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_REDUCEDDEBUGINFO = register("reducedDebugInfo", GameRules.Category.MISC, GameRules.BooleanValue.create(false, (server, rule) -> {
        byte b = (byte)(rule.get() ? 22 : 23);

        for(ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            serverPlayer.connection.send(new ClientboundEntityEventPacket(serverPlayer, b));
        }

    }));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_SPECTATORSGENERATECHUNKS = register("spectatorsGenerateChunks", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.IntegerValue> RULE_SPAWN_RADIUS = register("spawnRadius", GameRules.Category.PLAYER, GameRules.IntegerValue.create(10));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DISABLE_ELYTRA_MOVEMENT_CHECK = register("disableElytraMovementCheck", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.IntegerValue> RULE_MAX_ENTITY_CRAMMING = register("maxEntityCramming", GameRules.Category.MOBS, GameRules.IntegerValue.create(24));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_WEATHER_CYCLE = register("doWeatherCycle", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_LIMITED_CRAFTING = register("doLimitedCrafting", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.IntegerValue> RULE_MAX_COMMAND_CHAIN_LENGTH = register("maxCommandChainLength", GameRules.Category.MISC, GameRules.IntegerValue.create(65536));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_ANNOUNCE_ADVANCEMENTS = register("announceAdvancements", GameRules.Category.CHAT, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DISABLE_RAIDS = register("disableRaids", GameRules.Category.MOBS, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DOINSOMNIA = register("doInsomnia", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_IMMEDIATE_RESPAWN = register("doImmediateRespawn", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false, (server, rule) -> {
        for(ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
            serverPlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN, rule.get() ? 1.0F : 0.0F));
        }

    }));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DROWNING_DAMAGE = register("drowningDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FALL_DAMAGE = register("fallDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FIRE_DAMAGE = register("fireDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FREEZE_DAMAGE = register("freezeDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_PATROL_SPAWNING = register("doPatrolSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_DO_TRADER_SPAWNING = register("doTraderSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_FORGIVE_DEAD_PLAYERS = register("forgiveDeadPlayers", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> RULE_UNIVERSAL_ANGER = register("universalAnger", GameRules.Category.MOBS, GameRules.BooleanValue.create(false));
    public static final GameRules.Key<GameRules.IntegerValue> RULE_PLAYERS_SLEEPING_PERCENTAGE = register("playersSleepingPercentage", GameRules.Category.PLAYER, GameRules.IntegerValue.create(100));
    private final Map<GameRules.Key<?>, GameRules.Value<?>> rules;

    private static <T extends GameRules.Value<T>> GameRules.Key<T> register(String name, GameRules.Category category, GameRules.Type<T> type) {
        GameRules.Key<T> key = new GameRules.Key<>(name, category);
        GameRules.Type<?> type2 = GAME_RULE_TYPES.put(key, type);
        if (type2 != null) {
            throw new IllegalStateException("Duplicate game rule registration for " + name);
        } else {
            return key;
        }
    }

    public GameRules(DynamicLike<?> dynamicLike) {
        this();
        this.loadFromTag(dynamicLike);
    }

    public GameRules() {
        this.rules = GAME_RULE_TYPES.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (e) -> {
            return e.getValue().createRule();
        }));
    }

    private GameRules(Map<GameRules.Key<?>, GameRules.Value<?>> rules) {
        this.rules = rules;
    }

    public <T extends GameRules.Value<T>> T getRule(GameRules.Key<T> key) {
        return (T)(this.rules.get(key));
    }

    public CompoundTag createTag() {
        CompoundTag compoundTag = new CompoundTag();
        this.rules.forEach((key, rule) -> {
            compoundTag.putString(key.id, rule.serialize());
        });
        return compoundTag;
    }

    private void loadFromTag(DynamicLike<?> dynamicLike) {
        this.rules.forEach((key, rule) -> {
            dynamicLike.get(key.id).asString().result().ifPresent(rule::deserialize);
        });
    }

    public GameRules copy() {
        return new GameRules(this.rules.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entry) -> {
            return entry.getValue().copy();
        })));
    }

    public static void visitGameRuleTypes(GameRules.GameRuleTypeVisitor visitor) {
        GAME_RULE_TYPES.forEach((key, type) -> {
            callVisitorCap(visitor, key, type);
        });
    }

    private static <T extends GameRules.Value<T>> void callVisitorCap(GameRules.GameRuleTypeVisitor consumer, GameRules.Key<?> key, GameRules.Type<?> type) {
        consumer.visit(key, type);
        type.callVisitor(consumer, key);
    }

    public void assignFrom(GameRules rules, @Nullable MinecraftServer server) {
        rules.rules.keySet().forEach((key) -> {
            this.assignCap(key, rules, server);
        });
    }

    private <T extends GameRules.Value<T>> void assignCap(GameRules.Key<T> key, GameRules rules, @Nullable MinecraftServer server) {
        T value = rules.getRule(key);
        this.<T>getRule(key).setFrom(value, server);
    }

    public boolean getBoolean(GameRules.Key<GameRules.BooleanValue> rule) {
        return this.getRule(rule).get();
    }

    public int getInt(GameRules.Key<GameRules.IntegerValue> rule) {
        return this.getRule(rule).get();
    }

    public static class BooleanValue extends GameRules.Value<GameRules.BooleanValue> {
        private boolean value;

        static GameRules.Type<GameRules.BooleanValue> create(boolean initialValue, BiConsumer<MinecraftServer, GameRules.BooleanValue> changeCallback) {
            return new GameRules.Type<>(BoolArgumentType::bool, (type) -> {
                return new GameRules.BooleanValue(type, initialValue);
            }, changeCallback, GameRules.GameRuleTypeVisitor::visitBoolean);
        }

        static GameRules.Type<GameRules.BooleanValue> create(boolean initialValue) {
            return create(initialValue, (server, rule) -> {
            });
        }

        public BooleanValue(GameRules.Type<GameRules.BooleanValue> type, boolean initialValue) {
            super(type);
            this.value = initialValue;
        }

        @Override
        protected void updateFromArgument(CommandContext<CommandSourceStack> context, String name) {
            this.value = BoolArgumentType.getBool(context, name);
        }

        public boolean get() {
            return this.value;
        }

        public void set(boolean value, @Nullable MinecraftServer server) {
            this.value = value;
            this.onChanged(server);
        }

        @Override
        public String serialize() {
            return Boolean.toString(this.value);
        }

        @Override
        protected void deserialize(String value) {
            this.value = Boolean.parseBoolean(value);
        }

        @Override
        public int getCommandResult() {
            return this.value ? 1 : 0;
        }

        @Override
        protected GameRules.BooleanValue getSelf() {
            return this;
        }

        @Override
        protected GameRules.BooleanValue copy() {
            return new GameRules.BooleanValue(this.type, this.value);
        }

        @Override
        public void setFrom(GameRules.BooleanValue rule, @Nullable MinecraftServer server) {
            this.value = rule.value;
            this.onChanged(server);
        }
    }

    public static enum Category {
        PLAYER("gamerule.category.player"),
        MOBS("gamerule.category.mobs"),
        SPAWNING("gamerule.category.spawning"),
        DROPS("gamerule.category.drops"),
        UPDATES("gamerule.category.updates"),
        CHAT("gamerule.category.chat"),
        MISC("gamerule.category.misc");

        private final String descriptionId;

        private Category(String category) {
            this.descriptionId = category;
        }

        public String getDescriptionId() {
            return this.descriptionId;
        }
    }

    public interface GameRuleTypeVisitor {
        default <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
        }

        default void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
        }

        default void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
        }
    }

    public static class IntegerValue extends GameRules.Value<GameRules.IntegerValue> {
        private int value;

        private static GameRules.Type<GameRules.IntegerValue> create(int initialValue, BiConsumer<MinecraftServer, GameRules.IntegerValue> changeCallback) {
            return new GameRules.Type<>(IntegerArgumentType::integer, (type) -> {
                return new GameRules.IntegerValue(type, initialValue);
            }, changeCallback, GameRules.GameRuleTypeVisitor::visitInteger);
        }

        static GameRules.Type<GameRules.IntegerValue> create(int initialValue) {
            return create(initialValue, (server, rule) -> {
            });
        }

        public IntegerValue(GameRules.Type<GameRules.IntegerValue> rule, int initialValue) {
            super(rule);
            this.value = initialValue;
        }

        @Override
        protected void updateFromArgument(CommandContext<CommandSourceStack> context, String name) {
            this.value = IntegerArgumentType.getInteger(context, name);
        }

        public int get() {
            return this.value;
        }

        public void set(int value, @Nullable MinecraftServer server) {
            this.value = value;
            this.onChanged(server);
        }

        @Override
        public String serialize() {
            return Integer.toString(this.value);
        }

        @Override
        protected void deserialize(String value) {
            this.value = safeParse(value);
        }

        public boolean tryDeserialize(String input) {
            try {
                this.value = Integer.parseInt(input);
                return true;
            } catch (NumberFormatException var3) {
                return false;
            }
        }

        private static int safeParse(String input) {
            if (!input.isEmpty()) {
                try {
                    return Integer.parseInt(input);
                } catch (NumberFormatException var2) {
                    GameRules.LOGGER.warn("Failed to parse integer {}", (Object)input);
                }
            }

            return 0;
        }

        @Override
        public int getCommandResult() {
            return this.value;
        }

        @Override
        protected GameRules.IntegerValue getSelf() {
            return this;
        }

        @Override
        protected GameRules.IntegerValue copy() {
            return new GameRules.IntegerValue(this.type, this.value);
        }

        @Override
        public void setFrom(GameRules.IntegerValue rule, @Nullable MinecraftServer server) {
            this.value = rule.value;
            this.onChanged(server);
        }
    }

    public static final class Key<T extends GameRules.Value<T>> {
        final String id;
        private final GameRules.Category category;

        public Key(String name, GameRules.Category category) {
            this.id = name;
            this.category = category;
        }

        @Override
        public String toString() {
            return this.id;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else {
                return object instanceof GameRules.Key && ((GameRules.Key)object).id.equals(this.id);
            }
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        public String getId() {
            return this.id;
        }

        public String getDescriptionId() {
            return "gamerule." + this.id;
        }

        public GameRules.Category getCategory() {
            return this.category;
        }
    }

    public static class Type<T extends GameRules.Value<T>> {
        private final Supplier<ArgumentType<?>> argument;
        private final Function<GameRules.Type<T>, T> constructor;
        final BiConsumer<MinecraftServer, T> callback;
        private final GameRules.VisitorCaller<T> visitorCaller;

        Type(Supplier<ArgumentType<?>> argumentType, Function<GameRules.Type<T>, T> ruleFactory, BiConsumer<MinecraftServer, T> changeCallback, GameRules.VisitorCaller<T> ruleAcceptor) {
            this.argument = argumentType;
            this.constructor = ruleFactory;
            this.callback = changeCallback;
            this.visitorCaller = ruleAcceptor;
        }

        public RequiredArgumentBuilder<CommandSourceStack, ?> createArgument(String name) {
            return Commands.argument(name, this.argument.get());
        }

        public T createRule() {
            return this.constructor.apply(this);
        }

        public void callVisitor(GameRules.GameRuleTypeVisitor consumer, GameRules.Key<T> key) {
            this.visitorCaller.call(consumer, key, this);
        }
    }

    public abstract static class Value<T extends GameRules.Value<T>> {
        protected final GameRules.Type<T> type;

        public Value(GameRules.Type<T> type) {
            this.type = type;
        }

        protected abstract void updateFromArgument(CommandContext<CommandSourceStack> context, String name);

        public void setFromArgument(CommandContext<CommandSourceStack> context, String name) {
            this.updateFromArgument(context, name);
            this.onChanged(context.getSource().getServer());
        }

        public void onChanged(@Nullable MinecraftServer server) {
            if (server != null) {
                this.type.callback.accept(server, this.getSelf());
            }

        }

        protected abstract void deserialize(String value);

        public abstract String serialize();

        @Override
        public String toString() {
            return this.serialize();
        }

        public abstract int getCommandResult();

        protected abstract T getSelf();

        protected abstract T copy();

        public abstract void setFrom(T rule, @Nullable MinecraftServer server);
    }

    interface VisitorCaller<T extends GameRules.Value<T>> {
        void call(GameRules.GameRuleTypeVisitor consumer, GameRules.Key<T> key, GameRules.Type<T> type);
    }
}
