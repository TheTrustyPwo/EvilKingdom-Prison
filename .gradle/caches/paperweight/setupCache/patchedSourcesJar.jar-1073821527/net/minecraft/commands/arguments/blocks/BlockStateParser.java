package net.minecraft.commands.arguments.blocks;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockStateParser {
    public static final SimpleCommandExceptionType ERROR_NO_TAGS_ALLOWED = new SimpleCommandExceptionType(new TranslatableComponent("argument.block.tag.disallowed"));
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_BLOCK = new DynamicCommandExceptionType((block) -> {
        return new TranslatableComponent("argument.block.id.invalid", block);
    });
    public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_PROPERTY = new Dynamic2CommandExceptionType((block, property) -> {
        return new TranslatableComponent("argument.block.property.unknown", block, property);
    });
    public static final Dynamic2CommandExceptionType ERROR_DUPLICATE_PROPERTY = new Dynamic2CommandExceptionType((block, property) -> {
        return new TranslatableComponent("argument.block.property.duplicate", property, block);
    });
    public static final Dynamic3CommandExceptionType ERROR_INVALID_VALUE = new Dynamic3CommandExceptionType((block, property, value) -> {
        return new TranslatableComponent("argument.block.property.invalid", block, value, property);
    });
    public static final Dynamic2CommandExceptionType ERROR_EXPECTED_VALUE = new Dynamic2CommandExceptionType((block, property) -> {
        return new TranslatableComponent("argument.block.property.novalue", block, property);
    });
    public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_PROPERTIES = new SimpleCommandExceptionType(new TranslatableComponent("argument.block.property.unclosed"));
    private static final char SYNTAX_START_PROPERTIES = '[';
    private static final char SYNTAX_START_NBT = '{';
    private static final char SYNTAX_END_PROPERTIES = ']';
    private static final char SYNTAX_EQUALS = '=';
    private static final char SYNTAX_PROPERTY_SEPARATOR = ',';
    private static final char SYNTAX_TAG = '#';
    private static final BiFunction<SuggestionsBuilder, Registry<Block>, CompletableFuture<Suggestions>> SUGGEST_NOTHING = (builder, registry) -> {
        return builder.buildFuture();
    };
    private final StringReader reader;
    private final boolean forTesting;
    private final Map<Property<?>, Comparable<?>> properties = Maps.newHashMap();
    private final Map<String, String> vagueProperties = Maps.newHashMap();
    public ResourceLocation id = new ResourceLocation("");
    private StateDefinition<Block, BlockState> definition;
    private BlockState state;
    @Nullable
    private CompoundTag nbt;
    @Nullable
    private TagKey<Block> tag;
    private int tagCursor;
    private BiFunction<SuggestionsBuilder, Registry<Block>, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

    public BlockStateParser(StringReader reader, boolean allowTag) {
        this.reader = reader;
        this.forTesting = allowTag;
    }

    public Map<Property<?>, Comparable<?>> getProperties() {
        return this.properties;
    }

    @Nullable
    public BlockState getState() {
        return this.state;
    }

    @Nullable
    public CompoundTag getNbt() {
        return this.nbt;
    }

    @Nullable
    public TagKey<Block> getTag() {
        return this.tag;
    }

    public BlockStateParser parse(boolean allowNbt) throws CommandSyntaxException {
        this.suggestions = this::suggestBlockIdOrTag;
        if (this.reader.canRead() && this.reader.peek() == '#') {
            this.readTag();
            this.suggestions = this::suggestOpenVaguePropertiesOrNbt;
            if (this.reader.canRead() && this.reader.peek() == '[') {
                this.readVagueProperties();
                this.suggestions = this::suggestOpenNbt;
            }
        } else {
            this.readBlock();
            this.suggestions = this::suggestOpenPropertiesOrNbt;
            if (this.reader.canRead() && this.reader.peek() == '[') {
                this.readProperties();
                this.suggestions = this::suggestOpenNbt;
            }
        }

        if (allowNbt && this.reader.canRead() && this.reader.peek() == '{') {
            this.suggestions = SUGGEST_NOTHING;
            this.readNbt();
        }

        return this;
    }

    private CompletableFuture<Suggestions> suggestPropertyNameOrEnd(SuggestionsBuilder builder, Registry<Block> registry) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }

        return this.suggestPropertyName(builder, registry);
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyNameOrEnd(SuggestionsBuilder builder, Registry<Block> registry) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }

        return this.suggestVaguePropertyName(builder, registry);
    }

    private CompletableFuture<Suggestions> suggestPropertyName(SuggestionsBuilder builder, Registry<Block> registry) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);

        for(Property<?> property : this.state.getProperties()) {
            if (!this.properties.containsKey(property) && property.getName().startsWith(string)) {
                builder.suggest(property.getName() + "=");
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyName(SuggestionsBuilder builder, Registry<Block> registry) {
        String string = builder.getRemaining().toLowerCase(Locale.ROOT);
        if (this.tag != null) {
            for(Holder<Block> holder : registry.getTagOrEmpty(this.tag)) {
                for(Property<?> property : holder.value().getStateDefinition().getProperties()) {
                    if (!this.vagueProperties.containsKey(property.getName()) && property.getName().startsWith(string)) {
                        builder.suggest(property.getName() + "=");
                    }
                }
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder builder, Registry<Block> registry) {
        if (builder.getRemaining().isEmpty() && this.hasBlockEntity(registry)) {
            builder.suggest(String.valueOf('{'));
        }

        return builder.buildFuture();
    }

    private boolean hasBlockEntity(Registry<Block> registry) {
        if (this.state != null) {
            return this.state.hasBlockEntity();
        } else {
            if (this.tag != null) {
                for(Holder<Block> holder : registry.getTagOrEmpty(this.tag)) {
                    if (holder.value().defaultBlockState().hasBlockEntity()) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder builder, Registry<Block> registry) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf('='));
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestNextPropertyOrEnd(SuggestionsBuilder builder, Registry<Block> registry) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }

        if (builder.getRemaining().isEmpty() && this.properties.size() < this.state.getProperties().size()) {
            builder.suggest(String.valueOf(','));
        }

        return builder.buildFuture();
    }

    private static <T extends Comparable<T>> SuggestionsBuilder addSuggestions(SuggestionsBuilder builder, Property<T> property) {
        for(T comparable : property.getPossibleValues()) {
            if (comparable instanceof Integer) {
                builder.suggest(comparable);
            } else {
                builder.suggest(property.getName(comparable));
            }
        }

        return builder;
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyValue(SuggestionsBuilder builder, Registry<Block> registry, String propertyName) {
        boolean bl = false;
        if (this.tag != null) {
            for(Holder<Block> holder : registry.getTagOrEmpty(this.tag)) {
                Block block = holder.value();
                Property<?> property = block.getStateDefinition().getProperty(propertyName);
                if (property != null) {
                    addSuggestions(builder, property);
                }

                if (!bl) {
                    for(Property<?> property2 : block.getStateDefinition().getProperties()) {
                        if (!this.vagueProperties.containsKey(property2.getName())) {
                            bl = true;
                            break;
                        }
                    }
                }
            }
        }

        if (bl) {
            builder.suggest(String.valueOf(','));
        }

        builder.suggest(String.valueOf(']'));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOpenVaguePropertiesOrNbt(SuggestionsBuilder builder, Registry<Block> registry) {
        if (builder.getRemaining().isEmpty() && this.tag != null) {
            boolean bl = false;
            boolean bl2 = false;

            for(Holder<Block> holder : registry.getTagOrEmpty(this.tag)) {
                Block block = holder.value();
                bl |= !block.getStateDefinition().getProperties().isEmpty();
                bl2 |= block.defaultBlockState().hasBlockEntity();
                if (bl && bl2) {
                    break;
                }
            }

            if (bl) {
                builder.suggest(String.valueOf('['));
            }

            if (bl2) {
                builder.suggest(String.valueOf('{'));
            }
        }

        return this.suggestTag(builder, registry);
    }

    private CompletableFuture<Suggestions> suggestOpenPropertiesOrNbt(SuggestionsBuilder builder, Registry<Block> registry) {
        if (builder.getRemaining().isEmpty()) {
            if (!this.state.getBlock().getStateDefinition().getProperties().isEmpty()) {
                builder.suggest(String.valueOf('['));
            }

            if (this.state.hasBlockEntity()) {
                builder.suggest(String.valueOf('{'));
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder builder, Registry<Block> registry) {
        return SharedSuggestionProvider.suggestResource(registry.getTagNames().map(TagKey::location), builder.createOffset(this.tagCursor).add(builder));
    }

    private CompletableFuture<Suggestions> suggestBlockIdOrTag(SuggestionsBuilder builder, Registry<Block> registry) {
        if (this.forTesting) {
            SharedSuggestionProvider.suggestResource(registry.getTagNames().map(TagKey::location), builder, String.valueOf('#'));
        }

        SharedSuggestionProvider.suggestResource(registry.keySet(), builder);
        return builder.buildFuture();
    }

    public void readBlock() throws CommandSyntaxException {
        int i = this.reader.getCursor();
        this.id = ResourceLocation.read(this.reader);
        Block block = Registry.BLOCK.getOptional(this.id).orElseThrow(() -> {
            this.reader.setCursor(i);
            return ERROR_UNKNOWN_BLOCK.createWithContext(this.reader, this.id.toString());
        });
        this.definition = block.getStateDefinition();
        this.state = block.defaultBlockState();
    }

    public void readTag() throws CommandSyntaxException {
        if (!this.forTesting) {
            throw ERROR_NO_TAGS_ALLOWED.create();
        } else {
            this.suggestions = this::suggestTag;
            this.reader.expect('#');
            this.tagCursor = this.reader.getCursor();
            this.tag = TagKey.create(Registry.BLOCK_REGISTRY, ResourceLocation.read(this.reader));
        }
    }

    public void readProperties() throws CommandSyntaxException {
        this.reader.skip();
        this.suggestions = this::suggestPropertyNameOrEnd;
        this.reader.skipWhitespace();

        while(true) {
            if (this.reader.canRead() && this.reader.peek() != ']') {
                this.reader.skipWhitespace();
                int i = this.reader.getCursor();
                String string = this.reader.readString();
                Property<?> property = this.definition.getProperty(string);
                if (property == null) {
                    this.reader.setCursor(i);
                    throw ERROR_UNKNOWN_PROPERTY.createWithContext(this.reader, this.id.toString(), string);
                }

                if (this.properties.containsKey(property)) {
                    this.reader.setCursor(i);
                    throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), string);
                }

                this.reader.skipWhitespace();
                this.suggestions = this::suggestEquals;
                if (!this.reader.canRead() || this.reader.peek() != '=') {
                    throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), string);
                }

                this.reader.skip();
                this.reader.skipWhitespace();
                this.suggestions = (builder, registry) -> {
                    return addSuggestions(builder, property).buildFuture();
                };
                int j = this.reader.getCursor();
                this.setValue(property, this.reader.readString(), j);
                this.suggestions = this::suggestNextPropertyOrEnd;
                this.reader.skipWhitespace();
                if (!this.reader.canRead()) {
                    continue;
                }

                if (this.reader.peek() == ',') {
                    this.reader.skip();
                    this.suggestions = this::suggestPropertyName;
                    continue;
                }

                if (this.reader.peek() != ']') {
                    throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
                }
            }

            if (this.reader.canRead()) {
                this.reader.skip();
                return;
            }

            throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
        }
    }

    public void readVagueProperties() throws CommandSyntaxException {
        this.reader.skip();
        this.suggestions = this::suggestVaguePropertyNameOrEnd;
        int i = -1;
        this.reader.skipWhitespace();

        while(true) {
            if (this.reader.canRead() && this.reader.peek() != ']') {
                this.reader.skipWhitespace();
                int j = this.reader.getCursor();
                String string = this.reader.readString();
                if (this.vagueProperties.containsKey(string)) {
                    this.reader.setCursor(j);
                    throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), string);
                }

                this.reader.skipWhitespace();
                if (!this.reader.canRead() || this.reader.peek() != '=') {
                    this.reader.setCursor(j);
                    throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), string);
                }

                this.reader.skip();
                this.reader.skipWhitespace();
                this.suggestions = (builder, registry) -> {
                    return this.suggestVaguePropertyValue(builder, registry, string);
                };
                i = this.reader.getCursor();
                String string2 = this.reader.readString();
                this.vagueProperties.put(string, string2);
                this.reader.skipWhitespace();
                if (!this.reader.canRead()) {
                    continue;
                }

                i = -1;
                if (this.reader.peek() == ',') {
                    this.reader.skip();
                    this.suggestions = this::suggestVaguePropertyName;
                    continue;
                }

                if (this.reader.peek() != ']') {
                    throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
                }
            }

            if (this.reader.canRead()) {
                this.reader.skip();
                return;
            }

            if (i >= 0) {
                this.reader.setCursor(i);
            }

            throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
        }
    }

    public void readNbt() throws CommandSyntaxException {
        this.nbt = (new TagParser(this.reader)).readStruct();
    }

    private <T extends Comparable<T>> void setValue(Property<T> property, String value, int cursor) throws CommandSyntaxException {
        Optional<T> optional = property.getValue(value);
        if (optional.isPresent()) {
            this.state = this.state.setValue(property, optional.get());
            this.properties.put(property, optional.get());
        } else {
            this.reader.setCursor(cursor);
            throw ERROR_INVALID_VALUE.createWithContext(this.reader, this.id.toString(), property.getName(), value);
        }
    }

    public static String serialize(BlockState state) {
        StringBuilder stringBuilder = new StringBuilder(Registry.BLOCK.getKey(state.getBlock()).toString());
        if (!state.getProperties().isEmpty()) {
            stringBuilder.append('[');
            boolean bl = false;

            for(Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
                if (bl) {
                    stringBuilder.append(',');
                }

                appendProperty(stringBuilder, entry.getKey(), entry.getValue());
                bl = true;
            }

            stringBuilder.append(']');
        }

        return stringBuilder.toString();
    }

    private static <T extends Comparable<T>> void appendProperty(StringBuilder builder, Property<T> property, Comparable<?> value) {
        builder.append(property.getName());
        builder.append('=');
        builder.append(property.getName((T)value));
    }

    public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder builder, Registry<Block> registry) {
        return this.suggestions.apply(builder.createOffset(this.reader.getCursor()), registry);
    }

    public Map<String, String> getVagueProperties() {
        return this.vagueProperties;
    }
}
