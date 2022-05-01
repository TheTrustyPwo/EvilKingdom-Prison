package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import org.slf4j.Logger;

public abstract class TagsProvider<T> implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    protected final DataGenerator generator;
    protected final Registry<T> registry;
    private final Map<ResourceLocation, Tag.Builder> builders = Maps.newLinkedHashMap();

    protected TagsProvider(DataGenerator root, Registry<T> registry) {
        this.generator = root;
        this.registry = registry;
    }

    protected abstract void addTags();

    @Override
    public void run(HashCache cache) {
        this.builders.clear();
        this.addTags();
        this.builders.forEach((id, builder) -> {
            List<Tag.BuilderEntry> list = builder.getEntries().filter((tag) -> {
                return !tag.entry().verifyIfPresent(this.registry::containsKey, this.builders::containsKey);
            }).toList();
            if (!list.isEmpty()) {
                throw new IllegalArgumentException(String.format("Couldn't define tag %s as it is missing following references: %s", id, list.stream().map(Objects::toString).collect(Collectors.joining(","))));
            } else {
                JsonObject jsonObject = builder.serializeToJson();
                Path path = this.getPath(id);

                try {
                    String string = GSON.toJson((JsonElement)jsonObject);
                    String string2 = SHA1.hashUnencodedChars(string).toString();
                    if (!Objects.equals(cache.getHash(path), string2) || !Files.exists(path)) {
                        Files.createDirectories(path.getParent());
                        BufferedWriter bufferedWriter = Files.newBufferedWriter(path);

                        try {
                            bufferedWriter.write(string);
                        } catch (Throwable var13) {
                            if (bufferedWriter != null) {
                                try {
                                    bufferedWriter.close();
                                } catch (Throwable var12) {
                                    var13.addSuppressed(var12);
                                }
                            }

                            throw var13;
                        }

                        if (bufferedWriter != null) {
                            bufferedWriter.close();
                        }
                    }

                    cache.putNew(path, string2);
                } catch (IOException var14) {
                    LOGGER.error("Couldn't save tags to {}", path, var14);
                }

            }
        });
    }

    private Path getPath(ResourceLocation id) {
        ResourceKey<? extends Registry<T>> resourceKey = this.registry.key();
        return this.generator.getOutputFolder().resolve("data/" + id.getNamespace() + "/" + TagManager.getTagDir(resourceKey) + "/" + id.getPath() + ".json");
    }

    protected TagsProvider.TagAppender<T> tag(TagKey<T> tag) {
        Tag.Builder builder = this.getOrCreateRawBuilder(tag);
        return new TagsProvider.TagAppender<>(builder, this.registry, "vanilla");
    }

    protected Tag.Builder getOrCreateRawBuilder(TagKey<T> tag) {
        return this.builders.computeIfAbsent(tag.location(), (id) -> {
            return new Tag.Builder();
        });
    }

    protected static class TagAppender<T> {
        private final Tag.Builder builder;
        private final Registry<T> registry;
        private final String source;

        TagAppender(Tag.Builder builder, Registry<T> registry, String source) {
            this.builder = builder;
            this.registry = registry;
            this.source = source;
        }

        public TagsProvider.TagAppender<T> add(T element) {
            this.builder.addElement(this.registry.getKey(element), this.source);
            return this;
        }

        @SafeVarargs
        public final TagsProvider.TagAppender<T> add(ResourceKey<T>... keys) {
            for(ResourceKey<T> resourceKey : keys) {
                this.builder.addElement(resourceKey.location(), this.source);
            }

            return this;
        }

        public TagsProvider.TagAppender<T> addOptional(ResourceLocation id) {
            this.builder.addOptionalElement(id, this.source);
            return this;
        }

        public TagsProvider.TagAppender<T> addTag(TagKey<T> identifiedTag) {
            this.builder.addTag(identifiedTag.location(), this.source);
            return this;
        }

        public TagsProvider.TagAppender<T> addOptionalTag(ResourceLocation id) {
            this.builder.addOptionalTag(id, this.source);
            return this;
        }

        @SafeVarargs
        public final TagsProvider.TagAppender<T> add(T... elements) {
            Stream.<T>of(elements).map(this.registry::getKey).forEach((id) -> {
                this.builder.addElement(id, this.source);
            });
            return this;
        }
    }
}
