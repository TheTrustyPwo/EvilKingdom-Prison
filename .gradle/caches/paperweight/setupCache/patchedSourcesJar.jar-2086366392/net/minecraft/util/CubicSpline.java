package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.mutable.MutableObject;

public interface CubicSpline<C> extends ToFloatFunction<C> {
    @VisibleForDebug
    String parityString();

    float min();

    float max();

    CubicSpline<C> mapAll(CubicSpline.CoordinateVisitor<C> coordinateVisitor);

    static <C> Codec<CubicSpline<C>> codec(Codec<ToFloatFunction<C>> locationFunctionCodec) {
        MutableObject<Codec<CubicSpline<C>>> mutableObject = new MutableObject<>();
        Codec<Point<C>> codec = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.FLOAT.fieldOf("location").forGetter(Point::location), ExtraCodecs.lazyInitializedCodec(mutableObject::getValue).fieldOf("value").forGetter(Point::value), Codec.FLOAT.fieldOf("derivative").forGetter(Point::derivative)).apply(instance, (location, value, derivative) -> {
                record Point<C>(float location, CubicSpline<C> value, float derivative) {
                }

                return new Point<>((float)location, value, (float)derivative);
            });
        });
        Codec<CubicSpline.Multipoint<C>> codec2 = RecordCodecBuilder.create((instance) -> {
            return instance.group(locationFunctionCodec.fieldOf("coordinate").forGetter(CubicSpline.Multipoint::coordinate), ExtraCodecs.nonEmptyList(codec.listOf()).fieldOf("points").forGetter((spline) -> {
                return IntStream.range(0, spline.locations.length).mapToObj((index) -> {
                    return new Point<>(spline.locations()[index], (CubicSpline)spline.values().get(index), spline.derivatives()[index]);
                }).toList();
            })).apply(instance, (locationFunction, splines) -> {
                float[] fs = new float[splines.size()];
                ImmutableList.Builder<CubicSpline<C>> builder = ImmutableList.builder();
                float[] gs = new float[splines.size()];

                for(int i = 0; i < splines.size(); ++i) {
                    Point<C> lv = splines.get(i);
                    fs[i] = lv.location();
                    builder.add(lv.value());
                    gs[i] = lv.derivative();
                }

                return new CubicSpline.Multipoint<>(locationFunction, fs, builder.build(), gs);
            });
        });
        mutableObject.setValue(Codec.either(Codec.FLOAT, codec2).xmap((either) -> {
            return either.map(CubicSpline.Constant::new, (spline) -> {
                return spline;
            });
        }, (spline) -> {
            Either var10000;
            if (spline instanceof CubicSpline.Constant) {
                CubicSpline.Constant<C> constant = spline;
                var10000 = Either.left(constant.value());
            } else {
                var10000 = Either.right(spline);
            }

            return var10000;
        }));
        return mutableObject.getValue();
    }

    static <C> CubicSpline<C> constant(float value) {
        return new CubicSpline.Constant<>(value);
    }

    static <C> CubicSpline.Builder<C> builder(ToFloatFunction<C> locationFunction) {
        return new CubicSpline.Builder<>(locationFunction);
    }

    static <C> CubicSpline.Builder<C> builder(ToFloatFunction<C> locationFunction, ToFloatFunction<Float> amplifier) {
        return new CubicSpline.Builder<>(locationFunction, amplifier);
    }

    public static final class Builder<C> {
        private final ToFloatFunction<C> coordinate;
        private final ToFloatFunction<Float> valueTransformer;
        private final FloatList locations = new FloatArrayList();
        private final List<CubicSpline<C>> values = Lists.newArrayList();
        private final FloatList derivatives = new FloatArrayList();

        protected Builder(ToFloatFunction<C> locationFunction) {
            this(locationFunction, (value) -> {
                return value;
            });
        }

        protected Builder(ToFloatFunction<C> locationFunction, ToFloatFunction<Float> amplifier) {
            this.coordinate = locationFunction;
            this.valueTransformer = amplifier;
        }

        public CubicSpline.Builder<C> addPoint(float location, float value, float derivative) {
            return this.addPoint(location, new CubicSpline.Constant<>(this.valueTransformer.apply(value)), derivative);
        }

        public CubicSpline.Builder<C> addPoint(float location, CubicSpline<C> value, float derivative) {
            if (!this.locations.isEmpty() && location <= this.locations.getFloat(this.locations.size() - 1)) {
                throw new IllegalArgumentException("Please register points in ascending order");
            } else {
                this.locations.add(location);
                this.values.add(value);
                this.derivatives.add(derivative);
                return this;
            }
        }

        public CubicSpline<C> build() {
            if (this.locations.isEmpty()) {
                throw new IllegalStateException("No elements added");
            } else {
                return new CubicSpline.Multipoint<>(this.coordinate, this.locations.toFloatArray(), ImmutableList.copyOf(this.values), this.derivatives.toFloatArray());
            }
        }
    }

    @VisibleForDebug
    public static record Constant<C>(float value) implements CubicSpline<C> {
        @Override
        public float apply(C x) {
            return this.value;
        }

        @Override
        public String parityString() {
            return String.format("k=%.3f", this.value);
        }

        @Override
        public float min() {
            return this.value;
        }

        @Override
        public float max() {
            return this.value;
        }

        @Override
        public CubicSpline<C> mapAll(CubicSpline.CoordinateVisitor<C> coordinateVisitor) {
            return this;
        }
    }

    public interface CoordinateVisitor<C> {
        ToFloatFunction<C> visit(ToFloatFunction<C> toFloatFunction);
    }

    @VisibleForDebug
    public static record Multipoint<C>(ToFloatFunction<C> coordinate, float[] locations, List<CubicSpline<C>> values, float[] derivatives) implements CubicSpline<C> {
        public Multipoint(ToFloatFunction<C> toFloatFunction, float[] fs, List<CubicSpline<C>> list, float[] gs) {
            if (fs.length == list.size() && fs.length == gs.length) {
                this.coordinate = toFloatFunction;
                this.locations = fs;
                this.values = list;
                this.derivatives = gs;
            } else {
                throw new IllegalArgumentException("All lengths must be equal, got: " + fs.length + " " + list.size() + " " + gs.length);
            }
        }

        @Override
        public float apply(C x) {
            float f = this.coordinate.apply(x);
            int i = Mth.binarySearch(0, this.locations.length, (index) -> {
                return f < this.locations[index];
            }) - 1;
            int j = this.locations.length - 1;
            if (i < 0) {
                return this.values.get(0).apply(x) + this.derivatives[0] * (f - this.locations[0]);
            } else if (i == j) {
                return this.values.get(j).apply(x) + this.derivatives[j] * (f - this.locations[j]);
            } else {
                float g = this.locations[i];
                float h = this.locations[i + 1];
                float k = (f - g) / (h - g);
                ToFloatFunction<C> toFloatFunction = this.values.get(i);
                ToFloatFunction<C> toFloatFunction2 = this.values.get(i + 1);
                float l = this.derivatives[i];
                float m = this.derivatives[i + 1];
                float n = toFloatFunction.apply(x);
                float o = toFloatFunction2.apply(x);
                float p = l * (h - g) - (o - n);
                float q = -m * (h - g) + (o - n);
                return Mth.lerp(k, n, o) + k * (1.0F - k) * Mth.lerp(k, p, q);
            }
        }

        @VisibleForTesting
        @Override
        public String parityString() {
            return "Spline{coordinate=" + this.coordinate + ", locations=" + this.toString(this.locations) + ", derivatives=" + this.toString(this.derivatives) + ", values=" + (String)this.values.stream().map(CubicSpline::parityString).collect(Collectors.joining(", ", "[", "]")) + "}";
        }

        private String toString(float[] values) {
            return "[" + (String)IntStream.range(0, values.length).mapToDouble((index) -> {
                return (double)values[index];
            }).mapToObj((value) -> {
                return String.format(Locale.ROOT, "%.3f", value);
            }).collect(Collectors.joining(", ")) + "]";
        }

        @Override
        public float min() {
            return (float)this.values().stream().mapToDouble(CubicSpline::min).min().orElseThrow();
        }

        @Override
        public float max() {
            return (float)this.values().stream().mapToDouble(CubicSpline::max).max().orElseThrow();
        }

        @Override
        public CubicSpline<C> mapAll(CubicSpline.CoordinateVisitor<C> coordinateVisitor) {
            return new CubicSpline.Multipoint<>(coordinateVisitor.visit(this.coordinate), this.locations, this.values().stream().map((cubicSpline) -> {
                return cubicSpline.mapAll(coordinateVisitor);
            }).toList(), this.derivatives);
        }
    }
}
