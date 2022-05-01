package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;

public class StatePropertiesPredicate {
    public static final StatePropertiesPredicate ANY = new StatePropertiesPredicate(ImmutableList.of());
    private final List<StatePropertiesPredicate.PropertyMatcher> properties;

    private static StatePropertiesPredicate.PropertyMatcher fromJson(String key, JsonElement json) {
        if (json.isJsonPrimitive()) {
            String string = json.getAsString();
            return new StatePropertiesPredicate.ExactPropertyMatcher(key, string);
        } else {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "value");
            String string2 = jsonObject.has("min") ? getStringOrNull(jsonObject.get("min")) : null;
            String string3 = jsonObject.has("max") ? getStringOrNull(jsonObject.get("max")) : null;
            return (StatePropertiesPredicate.PropertyMatcher)(string2 != null && string2.equals(string3) ? new StatePropertiesPredicate.ExactPropertyMatcher(key, string2) : new StatePropertiesPredicate.RangedPropertyMatcher(key, string2, string3));
        }
    }

    @Nullable
    private static String getStringOrNull(JsonElement json) {
        return json.isJsonNull() ? null : json.getAsString();
    }

    StatePropertiesPredicate(List<StatePropertiesPredicate.PropertyMatcher> conditions) {
        this.properties = ImmutableList.copyOf(conditions);
    }

    public <S extends StateHolder<?, S>> boolean matches(StateDefinition<?, S> stateManager, S container) {
        for(StatePropertiesPredicate.PropertyMatcher propertyMatcher : this.properties) {
            if (!propertyMatcher.match(stateManager, container)) {
                return false;
            }
        }

        return true;
    }

    public boolean matches(BlockState state) {
        return this.matches(state.getBlock().getStateDefinition(), state);
    }

    public boolean matches(FluidState state) {
        return this.matches(state.getType().getStateDefinition(), state);
    }

    public void checkState(StateDefinition<?, ?> factory, Consumer<String> reporter) {
        this.properties.forEach((condition) -> {
            condition.checkState(factory, reporter);
        });
    }

    public static StatePropertiesPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "properties");
            List<StatePropertiesPredicate.PropertyMatcher> list = Lists.newArrayList();

            for(Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                list.add(fromJson(entry.getKey(), entry.getValue()));
            }

            return new StatePropertiesPredicate(list);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (!this.properties.isEmpty()) {
                this.properties.forEach((condition) -> {
                    jsonObject.add(condition.getName(), condition.toJson());
                });
            }

            return jsonObject;
        }
    }

    public static class Builder {
        private final List<StatePropertiesPredicate.PropertyMatcher> matchers = Lists.newArrayList();

        private Builder() {
        }

        public static StatePropertiesPredicate.Builder properties() {
            return new StatePropertiesPredicate.Builder();
        }

        public StatePropertiesPredicate.Builder hasProperty(Property<?> property, String valueName) {
            this.matchers.add(new StatePropertiesPredicate.ExactPropertyMatcher(property.getName(), valueName));
            return this;
        }

        public StatePropertiesPredicate.Builder hasProperty(Property<Integer> property, int value) {
            return this.hasProperty(property, Integer.toString(value));
        }

        public StatePropertiesPredicate.Builder hasProperty(Property<Boolean> property, boolean value) {
            return this.hasProperty(property, Boolean.toString(value));
        }

        public <T extends Comparable<T> & StringRepresentable> StatePropertiesPredicate.Builder hasProperty(Property<T> property, T value) {
            return this.hasProperty(property, value.getSerializedName());
        }

        public StatePropertiesPredicate build() {
            return new StatePropertiesPredicate(this.matchers);
        }
    }

    static class ExactPropertyMatcher extends StatePropertiesPredicate.PropertyMatcher {
        private final String value;

        public ExactPropertyMatcher(String key, String value) {
            super(key);
            this.value = value;
        }

        @Override
        protected <T extends Comparable<T>> boolean match(StateHolder<?, ?> state, Property<T> property) {
            T comparable = state.getValue(property);
            Optional<T> optional = property.getValue(this.value);
            return optional.isPresent() && comparable.compareTo(optional.get()) == 0;
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(this.value);
        }
    }

    abstract static class PropertyMatcher {
        private final String name;

        public PropertyMatcher(String key) {
            this.name = key;
        }

        public <S extends StateHolder<?, S>> boolean match(StateDefinition<?, S> stateManager, S state) {
            Property<?> property = stateManager.getProperty(this.name);
            return property == null ? false : this.match(state, property);
        }

        protected abstract <T extends Comparable<T>> boolean match(StateHolder<?, ?> state, Property<T> property);

        public abstract JsonElement toJson();

        public String getName() {
            return this.name;
        }

        public void checkState(StateDefinition<?, ?> factory, Consumer<String> reporter) {
            Property<?> property = factory.getProperty(this.name);
            if (property == null) {
                reporter.accept(this.name);
            }

        }
    }

    static class RangedPropertyMatcher extends StatePropertiesPredicate.PropertyMatcher {
        @Nullable
        private final String minValue;
        @Nullable
        private final String maxValue;

        public RangedPropertyMatcher(String key, @Nullable String min, @Nullable String max) {
            super(key);
            this.minValue = min;
            this.maxValue = max;
        }

        @Override
        protected <T extends Comparable<T>> boolean match(StateHolder<?, ?> state, Property<T> property) {
            T comparable = state.getValue(property);
            if (this.minValue != null) {
                Optional<T> optional = property.getValue(this.minValue);
                if (!optional.isPresent() || comparable.compareTo(optional.get()) < 0) {
                    return false;
                }
            }

            if (this.maxValue != null) {
                Optional<T> optional2 = property.getValue(this.maxValue);
                if (!optional2.isPresent() || comparable.compareTo(optional2.get()) > 0) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public JsonElement toJson() {
            JsonObject jsonObject = new JsonObject();
            if (this.minValue != null) {
                jsonObject.addProperty("min", this.minValue);
            }

            if (this.maxValue != null) {
                jsonObject.addProperty("max", this.maxValue);
            }

            return jsonObject;
        }
    }
}
