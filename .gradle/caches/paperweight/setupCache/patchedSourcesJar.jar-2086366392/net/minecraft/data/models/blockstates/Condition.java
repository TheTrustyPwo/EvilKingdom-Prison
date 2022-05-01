package net.minecraft.data.models.blockstates;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public interface Condition extends Supplier<JsonElement> {
    void validate(StateDefinition<?, ?> stateManager);

    static Condition.TerminalCondition condition() {
        return new Condition.TerminalCondition();
    }

    static Condition and(Condition... conditions) {
        return new Condition.CompositeCondition(Condition.Operation.AND, Arrays.asList(conditions));
    }

    static Condition or(Condition... conditions) {
        return new Condition.CompositeCondition(Condition.Operation.OR, Arrays.asList(conditions));
    }

    public static class CompositeCondition implements Condition {
        private final Condition.Operation operation;
        private final List<Condition> subconditions;

        CompositeCondition(Condition.Operation operator, List<Condition> components) {
            this.operation = operator;
            this.subconditions = components;
        }

        @Override
        public void validate(StateDefinition<?, ?> stateManager) {
            this.subconditions.forEach((component) -> {
                component.validate(stateManager);
            });
        }

        @Override
        public JsonElement get() {
            JsonArray jsonArray = new JsonArray();
            this.subconditions.stream().map(Supplier::get).forEach(jsonArray::add);
            JsonObject jsonObject = new JsonObject();
            jsonObject.add(this.operation.id, jsonArray);
            return jsonObject;
        }
    }

    public static enum Operation {
        AND("AND"),
        OR("OR");

        final String id;

        private Operation(String name) {
            this.id = name;
        }
    }

    public static class TerminalCondition implements Condition {
        private final Map<Property<?>, String> terms = Maps.newHashMap();

        private static <T extends Comparable<T>> String joinValues(Property<T> property, Stream<T> valueStream) {
            return valueStream.map(property::getName).collect(Collectors.joining("|"));
        }

        private static <T extends Comparable<T>> String getTerm(Property<T> property, T value, T[] otherValues) {
            return joinValues(property, Stream.concat(Stream.of(value), Stream.of(otherValues)));
        }

        private <T extends Comparable<T>> void putValue(Property<T> property, String value) {
            String string = this.terms.put(property, value);
            if (string != null) {
                throw new IllegalStateException("Tried to replace " + property + " value from " + string + " to " + value);
            }
        }

        public final <T extends Comparable<T>> Condition.TerminalCondition term(Property<T> property, T value) {
            this.putValue(property, property.getName(value));
            return this;
        }

        @SafeVarargs
        public final <T extends Comparable<T>> Condition.TerminalCondition term(Property<T> property, T value, T... otherValues) {
            this.putValue(property, getTerm(property, value, otherValues));
            return this;
        }

        public final <T extends Comparable<T>> Condition.TerminalCondition negatedTerm(Property<T> property, T value) {
            this.putValue(property, "!" + property.getName(value));
            return this;
        }

        @SafeVarargs
        public final <T extends Comparable<T>> Condition.TerminalCondition negatedTerm(Property<T> property, T value, T... otherValues) {
            this.putValue(property, "!" + getTerm(property, value, otherValues));
            return this;
        }

        @Override
        public JsonElement get() {
            JsonObject jsonObject = new JsonObject();
            this.terms.forEach((property, value) -> {
                jsonObject.addProperty(property.getName(), value);
            });
            return jsonObject;
        }

        @Override
        public void validate(StateDefinition<?, ?> stateManager) {
            List<Property<?>> list = this.terms.keySet().stream().filter((property) -> {
                return stateManager.getProperty(property.getName()) != property;
            }).collect(Collectors.toList());
            if (!list.isEmpty()) {
                throw new IllegalStateException("Properties " + list + " are missing from " + stateManager);
            }
        }
    }
}
