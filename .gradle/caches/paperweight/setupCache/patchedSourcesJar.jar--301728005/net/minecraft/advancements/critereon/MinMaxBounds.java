package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.GsonHelper;

public abstract class MinMaxBounds<T extends Number> {
    public static final SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(new TranslatableComponent("argument.range.empty"));
    public static final SimpleCommandExceptionType ERROR_SWAPPED = new SimpleCommandExceptionType(new TranslatableComponent("argument.range.swapped"));
    @Nullable
    protected final T min;
    @Nullable
    protected final T max;

    protected MinMaxBounds(@Nullable T min, @Nullable T max) {
        this.min = min;
        this.max = max;
    }

    @Nullable
    public T getMin() {
        return this.min;
    }

    @Nullable
    public T getMax() {
        return this.max;
    }

    public boolean isAny() {
        return this.min == null && this.max == null;
    }

    public JsonElement serializeToJson() {
        if (this.isAny()) {
            return JsonNull.INSTANCE;
        } else if (this.min != null && this.min.equals(this.max)) {
            return new JsonPrimitive(this.min);
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.min != null) {
                jsonObject.addProperty("min", this.min);
            }

            if (this.max != null) {
                jsonObject.addProperty("max", this.max);
            }

            return jsonObject;
        }
    }

    protected static <T extends Number, R extends MinMaxBounds<T>> R fromJson(@Nullable JsonElement json, R fallback, BiFunction<JsonElement, String, T> asNumber, MinMaxBounds.BoundsFactory<T, R> factory) {
        if (json != null && !json.isJsonNull()) {
            if (GsonHelper.isNumberValue(json)) {
                T number = asNumber.apply(json, "value");
                return factory.create(number, number);
            } else {
                JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "value");
                T number2 = jsonObject.has("min") ? asNumber.apply(jsonObject.get("min"), "min") : null;
                T number3 = jsonObject.has("max") ? asNumber.apply(jsonObject.get("max"), "max") : null;
                return factory.create(number2, number3);
            }
        } else {
            return fallback;
        }
    }

    protected static <T extends Number, R extends MinMaxBounds<T>> R fromReader(StringReader commandReader, MinMaxBounds.BoundsFromReaderFactory<T, R> commandFactory, Function<String, T> converter, Supplier<DynamicCommandExceptionType> exceptionTypeSupplier, Function<T, T> mapper) throws CommandSyntaxException {
        if (!commandReader.canRead()) {
            throw ERROR_EMPTY.createWithContext(commandReader);
        } else {
            int i = commandReader.getCursor();

            try {
                T number = optionallyFormat(readNumber(commandReader, converter, exceptionTypeSupplier), mapper);
                T number2;
                if (commandReader.canRead(2) && commandReader.peek() == '.' && commandReader.peek(1) == '.') {
                    commandReader.skip();
                    commandReader.skip();
                    number2 = optionallyFormat(readNumber(commandReader, converter, exceptionTypeSupplier), mapper);
                    if (number == null && number2 == null) {
                        throw ERROR_EMPTY.createWithContext(commandReader);
                    }
                } else {
                    number2 = number;
                }

                if (number == null && number2 == null) {
                    throw ERROR_EMPTY.createWithContext(commandReader);
                } else {
                    return commandFactory.create(commandReader, number, number2);
                }
            } catch (CommandSyntaxException var8) {
                commandReader.setCursor(i);
                throw new CommandSyntaxException(var8.getType(), var8.getRawMessage(), var8.getInput(), i);
            }
        }
    }

    @Nullable
    private static <T extends Number> T readNumber(StringReader reader, Function<String, T> converter, Supplier<DynamicCommandExceptionType> exceptionTypeSupplier) throws CommandSyntaxException {
        int i = reader.getCursor();

        while(reader.canRead() && isAllowedInputChat(reader)) {
            reader.skip();
        }

        String string = reader.getString().substring(i, reader.getCursor());
        if (string.isEmpty()) {
            return (T)null;
        } else {
            try {
                return converter.apply(string);
            } catch (NumberFormatException var6) {
                throw exceptionTypeSupplier.get().createWithContext(reader, string);
            }
        }
    }

    private static boolean isAllowedInputChat(StringReader reader) {
        char c = reader.peek();
        if ((c < '0' || c > '9') && c != '-') {
            if (c != '.') {
                return false;
            } else {
                return !reader.canRead(2) || reader.peek(1) != '.';
            }
        } else {
            return true;
        }
    }

    @Nullable
    private static <T> T optionallyFormat(@Nullable T object, Function<T, T> function) {
        return (T)(object == null ? null : function.apply(object));
    }

    @FunctionalInterface
    protected interface BoundsFactory<T extends Number, R extends MinMaxBounds<T>> {
        R create(@Nullable T min, @Nullable T max);
    }

    @FunctionalInterface
    protected interface BoundsFromReaderFactory<T extends Number, R extends MinMaxBounds<T>> {
        R create(StringReader reader, @Nullable T min, @Nullable T max) throws CommandSyntaxException;
    }

    public static class Doubles extends MinMaxBounds<Double> {
        public static final MinMaxBounds.Doubles ANY = new MinMaxBounds.Doubles((Double)null, (Double)null);
        @Nullable
        private final Double minSq;
        @Nullable
        private final Double maxSq;

        private static MinMaxBounds.Doubles create(StringReader reader, @Nullable Double min, @Nullable Double max) throws CommandSyntaxException {
            if (min != null && max != null && min > max) {
                throw ERROR_SWAPPED.createWithContext(reader);
            } else {
                return new MinMaxBounds.Doubles(min, max);
            }
        }

        @Nullable
        private static Double squareOpt(@Nullable Double value) {
            return value == null ? null : value * value;
        }

        private Doubles(@Nullable Double min, @Nullable Double max) {
            super(min, max);
            this.minSq = squareOpt(min);
            this.maxSq = squareOpt(max);
        }

        public static MinMaxBounds.Doubles exactly(double value) {
            return new MinMaxBounds.Doubles(value, value);
        }

        public static MinMaxBounds.Doubles between(double min, double max) {
            return new MinMaxBounds.Doubles(min, max);
        }

        public static MinMaxBounds.Doubles atLeast(double value) {
            return new MinMaxBounds.Doubles(value, (Double)null);
        }

        public static MinMaxBounds.Doubles atMost(double value) {
            return new MinMaxBounds.Doubles((Double)null, value);
        }

        public boolean matches(double value) {
            if (this.min != null && this.min > value) {
                return false;
            } else {
                return this.max == null || !(this.max < value);
            }
        }

        public boolean matchesSqr(double value) {
            if (this.minSq != null && this.minSq > value) {
                return false;
            } else {
                return this.maxSq == null || !(this.maxSq < value);
            }
        }

        public static MinMaxBounds.Doubles fromJson(@Nullable JsonElement element) {
            return fromJson(element, ANY, GsonHelper::convertToDouble, MinMaxBounds.Doubles::new);
        }

        public static MinMaxBounds.Doubles fromReader(StringReader reader) throws CommandSyntaxException {
            return fromReader(reader, (value) -> {
                return value;
            });
        }

        public static MinMaxBounds.Doubles fromReader(StringReader reader, Function<Double, Double> mapper) throws CommandSyntaxException {
            return fromReader(reader, MinMaxBounds.Doubles::create, Double::parseDouble, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble, mapper);
        }
    }

    public static class Ints extends MinMaxBounds<Integer> {
        public static final MinMaxBounds.Ints ANY = new MinMaxBounds.Ints((Integer)null, (Integer)null);
        @Nullable
        private final Long minSq;
        @Nullable
        private final Long maxSq;

        private static MinMaxBounds.Ints create(StringReader reader, @Nullable Integer min, @Nullable Integer max) throws CommandSyntaxException {
            if (min != null && max != null && min > max) {
                throw ERROR_SWAPPED.createWithContext(reader);
            } else {
                return new MinMaxBounds.Ints(min, max);
            }
        }

        @Nullable
        private static Long squareOpt(@Nullable Integer value) {
            return value == null ? null : value.longValue() * value.longValue();
        }

        private Ints(@Nullable Integer min, @Nullable Integer max) {
            super(min, max);
            this.minSq = squareOpt(min);
            this.maxSq = squareOpt(max);
        }

        public static MinMaxBounds.Ints exactly(int value) {
            return new MinMaxBounds.Ints(value, value);
        }

        public static MinMaxBounds.Ints between(int min, int max) {
            return new MinMaxBounds.Ints(min, max);
        }

        public static MinMaxBounds.Ints atLeast(int value) {
            return new MinMaxBounds.Ints(value, (Integer)null);
        }

        public static MinMaxBounds.Ints atMost(int value) {
            return new MinMaxBounds.Ints((Integer)null, value);
        }

        public boolean matches(int value) {
            if (this.min != null && this.min > value) {
                return false;
            } else {
                return this.max == null || this.max >= value;
            }
        }

        public boolean matchesSqr(long value) {
            if (this.minSq != null && this.minSq > value) {
                return false;
            } else {
                return this.maxSq == null || this.maxSq >= value;
            }
        }

        public static MinMaxBounds.Ints fromJson(@Nullable JsonElement element) {
            return fromJson(element, ANY, GsonHelper::convertToInt, MinMaxBounds.Ints::new);
        }

        public static MinMaxBounds.Ints fromReader(StringReader reader) throws CommandSyntaxException {
            return fromReader(reader, (value) -> {
                return value;
            });
        }

        public static MinMaxBounds.Ints fromReader(StringReader reader, Function<Integer, Integer> converter) throws CommandSyntaxException {
            return fromReader(reader, MinMaxBounds.Ints::create, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt, converter);
        }
    }
}
