package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootContext {
    private final Random random;
    private final float luck;
    private final ServerLevel level;
    private final Function<ResourceLocation, LootTable> lootTables;
    private final Set<LootTable> visitedTables = Sets.newLinkedHashSet();
    private final Function<ResourceLocation, LootItemCondition> conditions;
    private final Set<LootItemCondition> visitedConditions = Sets.newLinkedHashSet();
    private final Map<LootContextParam<?>, Object> params;
    private final Map<ResourceLocation, LootContext.DynamicDrop> dynamicDrops;

    LootContext(Random random, float luck, ServerLevel world, Function<ResourceLocation, LootTable> tableGetter, Function<ResourceLocation, LootItemCondition> conditionGetter, Map<LootContextParam<?>, Object> parameters, Map<ResourceLocation, LootContext.DynamicDrop> drops) {
        this.random = random;
        this.luck = luck;
        this.level = world;
        this.lootTables = tableGetter;
        this.conditions = conditionGetter;
        this.params = ImmutableMap.copyOf(parameters);
        this.dynamicDrops = ImmutableMap.copyOf(drops);
    }

    public boolean hasParam(LootContextParam<?> parameter) {
        return this.params.containsKey(parameter);
    }

    public <T> T getParam(LootContextParam<T> parameter) {
        T object = (T)this.params.get(parameter);
        if (object == null) {
            throw new NoSuchElementException(parameter.getName().toString());
        } else {
            return object;
        }
    }

    public void addDynamicDrops(ResourceLocation id, Consumer<ItemStack> lootConsumer) {
        LootContext.DynamicDrop dynamicDrop = this.dynamicDrops.get(id);
        if (dynamicDrop != null) {
            dynamicDrop.add(this, lootConsumer);
        }

    }

    @Nullable
    public <T> T getParamOrNull(LootContextParam<T> parameter) {
        return (T)this.params.get(parameter);
    }

    public boolean addVisitedTable(LootTable table) {
        return this.visitedTables.add(table);
    }

    public void removeVisitedTable(LootTable table) {
        this.visitedTables.remove(table);
    }

    public boolean addVisitedCondition(LootItemCondition condition) {
        return this.visitedConditions.add(condition);
    }

    public void removeVisitedCondition(LootItemCondition condition) {
        this.visitedConditions.remove(condition);
    }

    public LootTable getLootTable(ResourceLocation id) {
        return this.lootTables.apply(id);
    }

    public LootItemCondition getCondition(ResourceLocation id) {
        return this.conditions.apply(id);
    }

    public Random getRandom() {
        return this.random;
    }

    public float getLuck() {
        return this.luck;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public static class Builder {
        private final ServerLevel level;
        private final Map<LootContextParam<?>, Object> params = Maps.newIdentityHashMap();
        private final Map<ResourceLocation, LootContext.DynamicDrop> dynamicDrops = Maps.newHashMap();
        private Random random;
        private float luck;

        public Builder(ServerLevel world) {
            this.level = world;
        }

        public LootContext.Builder withRandom(Random random) {
            this.random = random;
            return this;
        }

        public LootContext.Builder withOptionalRandomSeed(long seed) {
            if (seed != 0L) {
                this.random = new Random(seed);
            }

            return this;
        }

        public LootContext.Builder withOptionalRandomSeed(long seed, Random random) {
            if (seed == 0L) {
                this.random = random;
            } else {
                this.random = new Random(seed);
            }

            return this;
        }

        public LootContext.Builder withLuck(float luck) {
            this.luck = luck;
            return this;
        }

        public <T> LootContext.Builder withParameter(LootContextParam<T> key, T value) {
            this.params.put(key, value);
            return this;
        }

        public <T> LootContext.Builder withOptionalParameter(LootContextParam<T> key, @Nullable T value) {
            if (value == null) {
                this.params.remove(key);
            } else {
                this.params.put(key, value);
            }

            return this;
        }

        public LootContext.Builder withDynamicDrop(ResourceLocation id, LootContext.DynamicDrop value) {
            LootContext.DynamicDrop dynamicDrop = this.dynamicDrops.put(id, value);
            if (dynamicDrop != null) {
                throw new IllegalStateException("Duplicated dynamic drop '" + this.dynamicDrops + "'");
            } else {
                return this;
            }
        }

        public ServerLevel getLevel() {
            return this.level;
        }

        public <T> T getParameter(LootContextParam<T> parameter) {
            T object = (T)this.params.get(parameter);
            if (object == null) {
                throw new IllegalArgumentException("No parameter " + parameter);
            } else {
                return object;
            }
        }

        @Nullable
        public <T> T getOptionalParameter(LootContextParam<T> parameter) {
            return (T)this.params.get(parameter);
        }

        public LootContext create(LootContextParamSet type) {
            Set<LootContextParam<?>> set = Sets.difference(this.params.keySet(), type.getAllowed());
            if (!set.isEmpty()) {
                throw new IllegalArgumentException("Parameters not allowed in this parameter set: " + set);
            } else {
                Set<LootContextParam<?>> set2 = Sets.difference(type.getRequired(), this.params.keySet());
                if (!set2.isEmpty()) {
                    throw new IllegalArgumentException("Missing required parameters: " + set2);
                } else {
                    Random random = this.random;
                    if (random == null) {
                        random = new Random();
                    }

                    MinecraftServer minecraftServer = this.level.getServer();
                    return new LootContext(random, this.luck, this.level, minecraftServer.getLootTables()::get, minecraftServer.getPredicateManager()::get, this.params, this.dynamicDrops);
                }
            }
        }
    }

    @FunctionalInterface
    public interface DynamicDrop {
        void add(LootContext context, Consumer<ItemStack> consumer);
    }

    public static enum EntityTarget {
        THIS("this", LootContextParams.THIS_ENTITY),
        KILLER("killer", LootContextParams.KILLER_ENTITY),
        DIRECT_KILLER("direct_killer", LootContextParams.DIRECT_KILLER_ENTITY),
        KILLER_PLAYER("killer_player", LootContextParams.LAST_DAMAGE_PLAYER);

        final String name;
        private final LootContextParam<? extends Entity> param;

        private EntityTarget(String type, LootContextParam<? extends Entity> parameter) {
            this.name = type;
            this.param = parameter;
        }

        public LootContextParam<? extends Entity> getParam() {
            return this.param;
        }

        public static LootContext.EntityTarget getByName(String type) {
            for(LootContext.EntityTarget entityTarget : values()) {
                if (entityTarget.name.equals(type)) {
                    return entityTarget;
                }
            }

            throw new IllegalArgumentException("Invalid entity target " + type);
        }

        public static class Serializer extends TypeAdapter<LootContext.EntityTarget> {
            @Override
            public void write(JsonWriter jsonWriter, LootContext.EntityTarget entityTarget) throws IOException {
                jsonWriter.value(entityTarget.name);
            }

            @Override
            public LootContext.EntityTarget read(JsonReader jsonReader) throws IOException {
                return LootContext.EntityTarget.getByName(jsonReader.nextString());
            }
        }
    }
}
