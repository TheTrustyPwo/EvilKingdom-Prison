package net.minecraft.server.dedicated;

import com.google.common.base.MoreObjects;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.core.RegistryAccess;
import org.slf4j.Logger;

public abstract class Settings<T extends Settings<T>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public final Properties properties;

    public Settings(Properties properties) {
        this.properties = properties;
    }

    public static Properties loadFromFile(Path path) {
        Properties properties = new Properties();

        try {
            InputStream inputStream = Files.newInputStream(path);

            try {
                properties.load(inputStream);
            } catch (Throwable var6) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException var7) {
            LOGGER.error("Failed to load properties from file: {}", (Object)path);
        }

        return properties;
    }

    public void store(Path path) {
        try {
            OutputStream outputStream = Files.newOutputStream(path);

            try {
                this.properties.store(outputStream, "Minecraft server properties");
            } catch (Throwable var6) {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException var7) {
            LOGGER.error("Failed to store properties to file: {}", (Object)path);
        }

    }

    private static <V extends Number> Function<String, V> wrapNumberDeserializer(Function<String, V> parser) {
        return (string) -> {
            try {
                return parser.apply(string);
            } catch (NumberFormatException var3) {
                return (V)null;
            }
        };
    }

    protected static <V> Function<String, V> dispatchNumberOrString(IntFunction<V> intParser, Function<String, V> fallbackParser) {
        return (string) -> {
            try {
                return intParser.apply(Integer.parseInt(string));
            } catch (NumberFormatException var4) {
                return fallbackParser.apply(string);
            }
        };
    }

    @Nullable
    public String getStringRaw(String key) {
        return (String)this.properties.get(key);
    }

    @Nullable
    protected <V> V getLegacy(String key, Function<String, V> stringifier) {
        String string = this.getStringRaw(key);
        if (string == null) {
            return (V)null;
        } else {
            this.properties.remove(key);
            return stringifier.apply(string);
        }
    }

    protected <V> V get(String key, Function<String, V> parser, Function<V, String> stringifier, V fallback) {
        String string = this.getStringRaw(key);
        V object = MoreObjects.firstNonNull((V)(string != null ? parser.apply(string) : null), fallback);
        this.properties.put(key, stringifier.apply(object));
        return object;
    }

    protected <V> Settings<T>.MutableValue<V> getMutable(String key, Function<String, V> parser, Function<V, String> stringifier, V fallback) {
        String string = this.getStringRaw(key);
        V object = MoreObjects.firstNonNull((V)(string != null ? parser.apply(string) : null), fallback);
        this.properties.put(key, stringifier.apply(object));
        return new Settings.MutableValue<>(key, object, stringifier);
    }

    protected <V> V get(String key, Function<String, V> parser, UnaryOperator<V> parsedTransformer, Function<V, String> stringifier, V fallback) {
        return this.get(key, (value) -> {
            V object = parser.apply(value);
            return (V)(object != null ? parsedTransformer.apply(object) : null);
        }, stringifier, fallback);
    }

    protected <V> V get(String key, Function<String, V> parser, V fallback) {
        return this.get(key, parser, Objects::toString, fallback);
    }

    protected <V> Settings<T>.MutableValue<V> getMutable(String key, Function<String, V> parser, V fallback) {
        return this.getMutable(key, parser, Objects::toString, fallback);
    }

    protected String get(String key, String fallback) {
        return this.get(key, Function.identity(), Function.identity(), fallback);
    }

    @Nullable
    protected String getLegacyString(String key) {
        return this.getLegacy(key, Function.identity());
    }

    protected int get(String key, int fallback) {
        return this.get(key, wrapNumberDeserializer(Integer::parseInt), Integer.valueOf(fallback));
    }

    protected Settings<T>.MutableValue<Integer> getMutable(String key, int fallback) {
        return this.getMutable(key, wrapNumberDeserializer(Integer::parseInt), fallback);
    }

    protected int get(String key, UnaryOperator<Integer> transformer, int fallback) {
        return this.get(key, wrapNumberDeserializer(Integer::parseInt), transformer, Objects::toString, fallback);
    }

    protected long get(String key, long fallback) {
        return this.get(key, wrapNumberDeserializer(Long::parseLong), fallback);
    }

    protected boolean get(String key, boolean fallback) {
        return this.get(key, Boolean::valueOf, fallback);
    }

    protected Settings<T>.MutableValue<Boolean> getMutable(String key, boolean fallback) {
        return this.getMutable(key, Boolean::valueOf, fallback);
    }

    @Nullable
    protected Boolean getLegacyBoolean(String key) {
        return this.getLegacy(key, Boolean::valueOf);
    }

    protected Properties cloneProperties() {
        Properties properties = new Properties();
        properties.putAll(this.properties);
        return properties;
    }

    protected abstract T reload(RegistryAccess registryManager, Properties properties);

    public class MutableValue<V> implements Supplier<V> {
        private final String key;
        private final V value;
        private final Function<V, String> serializer;

        MutableValue(String key, V value, Function<V, String> stringifier) {
            this.key = key;
            this.value = value;
            this.serializer = stringifier;
        }

        @Override
        public V get() {
            return this.value;
        }

        public T update(RegistryAccess registryManager, V value) {
            Properties properties = Settings.this.cloneProperties();
            properties.put(this.key, this.serializer.apply(value));
            return Settings.this.reload(registryManager, properties);
        }
    }
}
