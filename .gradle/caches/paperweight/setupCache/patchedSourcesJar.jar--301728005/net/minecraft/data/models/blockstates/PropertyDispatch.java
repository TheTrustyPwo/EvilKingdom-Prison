package net.minecraft.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class PropertyDispatch {
    private final Map<Selector, List<Variant>> values = Maps.newHashMap();

    protected void putValue(Selector condition, List<Variant> possibleVariants) {
        List<Variant> list = this.values.put(condition, possibleVariants);
        if (list != null) {
            throw new IllegalStateException("Value " + condition + " is already defined");
        }
    }

    Map<Selector, List<Variant>> getEntries() {
        this.verifyComplete();
        return ImmutableMap.copyOf(this.values);
    }

    private void verifyComplete() {
        List<Property<?>> list = this.getDefinedProperties();
        Stream<Selector> stream = Stream.of(Selector.empty());

        for(Property<?> property : list) {
            stream = stream.flatMap((selector) -> {
                return property.getAllValues().map(selector::extend);
            });
        }

        List<Selector> list2 = stream.filter((selector) -> {
            return !this.values.containsKey(selector);
        }).collect(Collectors.toList());
        if (!list2.isEmpty()) {
            throw new IllegalStateException("Missing definition for properties: " + list2);
        }
    }

    abstract List<Property<?>> getDefinedProperties();

    public static <T1 extends Comparable<T1>> PropertyDispatch.C1<T1> property(Property<T1> property) {
        return new PropertyDispatch.C1<>(property);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<T1, T2> properties(Property<T1> first, Property<T2> second) {
        return new PropertyDispatch.C2<>(first, second);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<T1, T2, T3> properties(Property<T1> first, Property<T2> second, Property<T3> third) {
        return new PropertyDispatch.C3<>(first, second, third);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<T1, T2, T3, T4> properties(Property<T1> first, Property<T2> second, Property<T3> third, Property<T4> fourth) {
        return new PropertyDispatch.C4<>(first, second, third, fourth);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<T1, T2, T3, T4, T5> properties(Property<T1> first, Property<T2> second, Property<T3> third, Property<T4> fourth, Property<T5> fifth) {
        return new PropertyDispatch.C5<>(first, second, third, fourth, fifth);
    }

    public static class C1<T1 extends Comparable<T1>> extends PropertyDispatch {
        private final Property<T1> property1;

        C1(Property<T1> property) {
            this.property1 = property;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return ImmutableList.of(this.property1);
        }

        public PropertyDispatch.C1<T1> select(T1 value, List<Variant> variants) {
            Selector selector = Selector.of(this.property1.value(value));
            this.putValue(selector, variants);
            return this;
        }

        public PropertyDispatch.C1<T1> select(T1 value, Variant variant) {
            return this.select(value, Collections.singletonList(variant));
        }

        public PropertyDispatch generate(Function<T1, Variant> variantFactory) {
            this.property1.getPossibleValues().forEach((value) -> {
                this.select(value, variantFactory.apply(value));
            });
            return this;
        }

        public PropertyDispatch generateList(Function<T1, List<Variant>> variantFactory) {
            this.property1.getPossibleValues().forEach((value) -> {
                this.select(value, variantFactory.apply(value));
            });
            return this;
        }
    }

    public static class C2<T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends PropertyDispatch {
        private final Property<T1> property1;
        private final Property<T2> property2;

        C2(Property<T1> first, Property<T2> second) {
            this.property1 = first;
            this.property2 = second;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return ImmutableList.of(this.property1, this.property2);
        }

        public PropertyDispatch.C2<T1, T2> select(T1 firstValue, T2 secondValue, List<Variant> variants) {
            Selector selector = Selector.of(this.property1.value(firstValue), this.property2.value(secondValue));
            this.putValue(selector, variants);
            return this;
        }

        public PropertyDispatch.C2<T1, T2> select(T1 firstValue, T2 secondValue, Variant variant) {
            return this.select(firstValue, secondValue, Collections.singletonList(variant));
        }

        public PropertyDispatch generate(BiFunction<T1, T2, Variant> variantFactory) {
            this.property1.getPossibleValues().forEach((firstValue) -> {
                this.property2.getPossibleValues().forEach((secondValue) -> {
                    this.select((T1)firstValue, secondValue, variantFactory.apply((T1)firstValue, secondValue));
                });
            });
            return this;
        }

        public PropertyDispatch generateList(BiFunction<T1, T2, List<Variant>> variantsFactory) {
            this.property1.getPossibleValues().forEach((firstValue) -> {
                this.property2.getPossibleValues().forEach((secondValue) -> {
                    this.select((T1)firstValue, secondValue, variantsFactory.apply((T1)firstValue, secondValue));
                });
            });
            return this;
        }
    }

    public static class C3<T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> extends PropertyDispatch {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;

        C3(Property<T1> first, Property<T2> second, Property<T3> third) {
            this.property1 = first;
            this.property2 = second;
            this.property3 = third;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return ImmutableList.of(this.property1, this.property2, this.property3);
        }

        public PropertyDispatch.C3<T1, T2, T3> select(T1 firstValue, T2 secondValue, T3 thirdValue, List<Variant> variants) {
            Selector selector = Selector.of(this.property1.value(firstValue), this.property2.value(secondValue), this.property3.value(thirdValue));
            this.putValue(selector, variants);
            return this;
        }

        public PropertyDispatch.C3<T1, T2, T3> select(T1 firstValue, T2 secondValue, T3 thirdValue, Variant variant) {
            return this.select(firstValue, secondValue, thirdValue, Collections.singletonList(variant));
        }

        public PropertyDispatch generate(PropertyDispatch.TriFunction<T1, T2, T3, Variant> variantFactory) {
            this.property1.getPossibleValues().forEach((firstValue) -> {
                this.property2.getPossibleValues().forEach((secondValue) -> {
                    this.property3.getPossibleValues().forEach((thirdValue) -> {
                        this.select((T1)firstValue, (T2)secondValue, thirdValue, variantFactory.apply((T1)firstValue, (T2)secondValue, thirdValue));
                    });
                });
            });
            return this;
        }

        public PropertyDispatch generateList(PropertyDispatch.TriFunction<T1, T2, T3, List<Variant>> variantFactory) {
            this.property1.getPossibleValues().forEach((firstValue) -> {
                this.property2.getPossibleValues().forEach((secondValue) -> {
                    this.property3.getPossibleValues().forEach((thirdValue) -> {
                        this.select((T1)firstValue, (T2)secondValue, thirdValue, variantFactory.apply((T1)firstValue, (T2)secondValue, thirdValue));
                    });
                });
            });
            return this;
        }
    }

    public static class C4<T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> extends PropertyDispatch {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;
        private final Property<T4> property4;

        C4(Property<T1> first, Property<T2> second, Property<T3> third, Property<T4> fourth) {
            this.property1 = first;
            this.property2 = second;
            this.property3 = third;
            this.property4 = fourth;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return ImmutableList.of(this.property1, this.property2, this.property3, this.property4);
        }

        public PropertyDispatch.C4<T1, T2, T3, T4> select(T1 firstValue, T2 secondValue, T3 thirdValue, T4 fourthValue, List<Variant> variants) {
            Selector selector = Selector.of(this.property1.value(firstValue), this.property2.value(secondValue), this.property3.value(thirdValue), this.property4.value(fourthValue));
            this.putValue(selector, variants);
            return this;
        }

        public PropertyDispatch.C4<T1, T2, T3, T4> select(T1 firstValue, T2 secondValue, T3 thirdValue, T4 fourthValue, Variant variant) {
            return this.select(firstValue, secondValue, thirdValue, fourthValue, Collections.singletonList(variant));
        }

        public PropertyDispatch generate(PropertyDispatch.QuadFunction<T1, T2, T3, T4, Variant> variantFactory) {
            this.property1.getPossibleValues().forEach((firstValue) -> {
                this.property2.getPossibleValues().forEach((secondValue) -> {
                    this.property3.getPossibleValues().forEach((thirdValue) -> {
                        this.property4.getPossibleValues().forEach((fourthValue) -> {
                            this.select((T1)firstValue, (T2)secondValue, (T3)thirdValue, fourthValue, variantFactory.apply((T1)firstValue, (T2)secondValue, (T3)thirdValue, fourthValue));
                        });
                    });
                });
            });
            return this;
        }

        public PropertyDispatch generateList(PropertyDispatch.QuadFunction<T1, T2, T3, T4, List<Variant>> variantFactory) {
            this.property1.getPossibleValues().forEach((firstValue) -> {
                this.property2.getPossibleValues().forEach((secondValue) -> {
                    this.property3.getPossibleValues().forEach((thirdValue) -> {
                        this.property4.getPossibleValues().forEach((fourthValue) -> {
                            this.select((T1)firstValue, (T2)secondValue, (T3)thirdValue, fourthValue, variantFactory.apply((T1)firstValue, (T2)secondValue, (T3)thirdValue, fourthValue));
                        });
                    });
                });
            });
            return this;
        }
    }

    public static class C5<T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> extends PropertyDispatch {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;
        private final Property<T4> property4;
        private final Property<T5> property5;

        C5(Property<T1> first, Property<T2> second, Property<T3> third, Property<T4> fourth, Property<T5> fifth) {
            this.property1 = first;
            this.property2 = second;
            this.property3 = third;
            this.property4 = fourth;
            this.property5 = fifth;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return ImmutableList.of(this.property1, this.property2, this.property3, this.property4, this.property5);
        }

        public PropertyDispatch.C5<T1, T2, T3, T4, T5> select(T1 firstValue, T2 secondValue, T3 thirdValue, T4 fourthValue, T5 fifthValue, List<Variant> variants) {
            Selector selector = Selector.of(this.property1.value(firstValue), this.property2.value(secondValue), this.property3.value(thirdValue), this.property4.value(fourthValue), this.property5.value(fifthValue));
            this.putValue(selector, variants);
            return this;
        }

        public PropertyDispatch.C5<T1, T2, T3, T4, T5> select(T1 firstValue, T2 secondValue, T3 thirdValue, T4 fourthValue, T5 fifthValue, Variant variant) {
            return this.select(firstValue, secondValue, thirdValue, fourthValue, fifthValue, Collections.singletonList(variant));
        }

        public PropertyDispatch generate(PropertyDispatch.PentaFunction<T1, T2, T3, T4, T5, Variant> variantFactory) {
            this.property1.getPossibleValues().forEach((firstValue) -> {
                this.property2.getPossibleValues().forEach((secondValue) -> {
                    this.property3.getPossibleValues().forEach((thirdValue) -> {
                        this.property4.getPossibleValues().forEach((fourthValue) -> {
                            this.property5.getPossibleValues().forEach((fifthValue) -> {
                                this.select((T1)firstValue, (T2)secondValue, (T3)thirdValue, (T4)fourthValue, fifthValue, variantFactory.apply((T1)firstValue, (T2)secondValue, (T3)thirdValue, (T4)fourthValue, fifthValue));
                            });
                        });
                    });
                });
            });
            return this;
        }

        public PropertyDispatch generateList(PropertyDispatch.PentaFunction<T1, T2, T3, T4, T5, List<Variant>> variantFactory) {
            this.property1.getPossibleValues().forEach((firstValue) -> {
                this.property2.getPossibleValues().forEach((secondValue) -> {
                    this.property3.getPossibleValues().forEach((thirdValue) -> {
                        this.property4.getPossibleValues().forEach((fourthValue) -> {
                            this.property5.getPossibleValues().forEach((fifthValue) -> {
                                this.select((T1)firstValue, (T2)secondValue, (T3)thirdValue, (T4)fourthValue, fifthValue, variantFactory.apply((T1)firstValue, (T2)secondValue, (T3)thirdValue, (T4)fourthValue, fifthValue));
                            });
                        });
                    });
                });
            });
            return this;
        }
    }

    @FunctionalInterface
    public interface PentaFunction<P1, P2, P3, P4, P5, R> {
        R apply(P1 one, P2 two, P3 three, P4 four, P5 five);
    }

    @FunctionalInterface
    public interface QuadFunction<P1, P2, P3, P4, R> {
        R apply(P1 one, P2 two, P3 three, P4 four);
    }

    @FunctionalInterface
    public interface TriFunction<P1, P2, P3, R> {
        R apply(P1 one, P2 two, P3 three);
    }
}
