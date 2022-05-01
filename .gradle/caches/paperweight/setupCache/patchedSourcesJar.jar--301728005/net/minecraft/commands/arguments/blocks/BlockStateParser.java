package net.minecraft.commands.arguments.blocks;

import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_BLOCK = new DynamicCommandExceptionType((object) -> {
        return new TranslatableComponent("argument.block.id.invalid", new Object[]{object});
    });
    public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_PROPERTY = new Dynamic2CommandExceptionType((object, object1) -> {
        return new TranslatableComponent("argument.block.property.unknown", new Object[]{object, object1});
    });
    public static final Dynamic2CommandExceptionType ERROR_DUPLICATE_PROPERTY = new Dynamic2CommandExceptionType((object, object1) -> {
        return new TranslatableComponent("argument.block.property.duplicate", new Object[]{object1, object});
    });
    public static final Dynamic3CommandExceptionType ERROR_INVALID_VALUE = new Dynamic3CommandExceptionType((object, object1, object2) -> {
        return new TranslatableComponent("argument.block.property.invalid", new Object[]{object, object2, object1});
    });
    public static final Dynamic2CommandExceptionType ERROR_EXPECTED_VALUE = new Dynamic2CommandExceptionType((object, object1) -> {
        return new TranslatableComponent("argument.block.property.novalue", new Object[]{object, object1});
    });
    public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_PROPERTIES = new SimpleCommandExceptionType(new TranslatableComponent("argument.block.property.unclosed"));
    private static final char SYNTAX_START_PROPERTIES = '[';
    private static final char SYNTAX_START_NBT = '{';
    private static final char SYNTAX_END_PROPERTIES = ']';
    private static final char SYNTAX_EQUALS = '=';
    private static final char SYNTAX_PROPERTY_SEPARATOR = ',';
    private static final char SYNTAX_TAG = '#';
    private static final BiFunction<SuggestionsBuilder, Registry<Block>, CompletableFuture<Suggestions>> SUGGEST_NOTHING = (suggestionsbuilder, iregistry) -> {
        return suggestionsbuilder.buildFuture();
    };
    private final StringReader reader;
    private final boolean forTesting;
    private final Map<Property<?>, Comparable<?>> properties = Maps.newLinkedHashMap(); // CraftBukkit - stable
    private final Map<String, String> vagueProperties = Maps.newHashMap();
    public ResourceLocation id = new ResourceLocation("");
    private StateDefinition<Block, BlockState> definition;
    private BlockState state;
    @Nullable
    private CompoundTag nbt;
    @Nullable
    private TagKey<Block> tag;
    private int tagCursor;
    private BiFunction<SuggestionsBuilder, Registry<Block>, CompletableFuture<Suggestions>> suggestions;

    public BlockStateParser(StringReader reader, boolean allowTag) {
        this.suggestions = BlockStateParser.SUGGEST_NOTHING;
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
            this.suggestions = BlockStateParser.SUGGEST_NOTHING;
            this.readNbt();
        }

        return this;
    }

    private CompletableFuture<Suggestions> suggestPropertyNameOrEnd(SuggestionsBuilder builder, Registry<Block> iregistry) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }

        return this.suggestPropertyName(builder, iregistry);
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyNameOrEnd(SuggestionsBuilder builder, Registry<Block> iregistry) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }

        return this.suggestVaguePropertyName(builder, iregistry);
    }

    private CompletableFuture<Suggestions> suggestPropertyName(SuggestionsBuilder builder, Registry<Block> iregistry) {
        String s = builder.getRemaining().toLowerCase(Locale.ROOT);
        Iterator iterator = this.state.getProperties().iterator();

        while (iterator.hasNext()) {
            Property<?> iblockstate = (Property) iterator.next();

            if (!this.properties.containsKey(iblockstate) && iblockstate.getName().startsWith(s)) {
                builder.suggest(iblockstate.getName() + "=");
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyName(SuggestionsBuilder builder, Registry<Block> iregistry) {
        String s = builder.getRemaining().toLowerCase(Locale.ROOT);

        if (this.tag != null) {
            Iterator iterator = iregistry.getTagOrEmpty(this.tag).iterator();

            while (iterator.hasNext()) {
                Holder<Block> holder = (Holder) iterator.next();
                Iterator iterator1 = ((Block) holder.value()).getStateDefinition().getProperties().iterator();

                while (iterator1.hasNext()) {
                    Property<?> iblockstate = (Property) iterator1.next();

                    if (!this.vagueProperties.containsKey(iblockstate.getName()) && iblockstate.getName().startsWith(s)) {
                        builder.suggest(iblockstate.getName() + "=");
                    }
                }
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder builder, Registry<Block> iregistry) {
        if (builder.getRemaining().isEmpty() && this.hasBlockEntity(iregistry)) {
            builder.suggest(String.valueOf('{'));
        }

        return builder.buildFuture();
    }

    private boolean hasBlockEntity(Registry<Block> iregistry) {
        if (this.state != null) {
            return this.state.hasBlockEntity();
        } else {
            if (this.tag != null) {
                Iterator iterator = iregistry.getTagOrEmpty(this.tag).iterator();

                while (iterator.hasNext()) {
                    Holder<Block> holder = (Holder) iterator.next();

                    if (((Block) holder.value()).defaultBlockState().hasBlockEntity()) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder builder, Registry<Block> iregistry) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf('='));
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestNextPropertyOrEnd(SuggestionsBuilder builder, Registry<Block> iregistry) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(']'));
        }

        if (builder.getRemaining().isEmpty() && this.properties.size() < this.state.getProperties().size()) {
            builder.suggest(String.valueOf(','));
        }

        return builder.buildFuture();
    }

    private static <T extends Comparable<T>> SuggestionsBuilder addSuggestions(SuggestionsBuilder builder, Property<T> property) {
        Iterator iterator = property.getPossibleValues().iterator();

        while (iterator.hasNext()) {
            T t0 = (T) iterator.next(); // CraftBukkit - decompile error

            if (t0 instanceof Integer) {
                builder.suggest((Integer) t0);
            } else {
                builder.suggest(property.getName(t0));
            }
        }

        return builder;
    }

    private CompletableFuture<Suggestions> suggestVaguePropertyValue(SuggestionsBuilder builder, Registry<Block> iregistry, String propertyName) {
        boolean flag = false;

        if (this.tag != null) {
            Iterator iterator = iregistry.getTagOrEmpty(this.tag).iterator();

            while (iterator.hasNext()) {
                Holder<Block> holder = (Holder) iterator.next();
                Block block = (Block) holder.value();
                Property<?> iblockstate = block.getStateDefinition().getProperty(propertyName);

                if (iblockstate != null) {
                    BlockStateParser.addSuggestions(builder, iblockstate);
                }

                if (!flag) {
                    Iterator iterator1 = block.getStateDefinition().getProperties().iterator();

                    while (iterator1.hasNext()) {
                        Property<?> iblockstate1 = (Property) iterator1.next();

                        if (!this.vagueProperties.containsKey(iblockstate1.getName())) {
                            flag = true;
                            break;
                        }
                    }
                }
            }
        }

        if (flag) {
            builder.suggest(String.valueOf(','));
        }

        builder.suggest(String.valueOf(']'));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOpenVaguePropertiesOrNbt(SuggestionsBuilder builder, Registry<Block> iregistry) {
        if (builder.getRemaining().isEmpty() && this.tag != null) {
            boolean flag = false;
            boolean flag1 = false;
            Iterator iterator = iregistry.getTagOrEmpty(this.tag).iterator();

            while (iterator.hasNext()) {
                Holder<Block> holder = (Holder) iterator.next();
                Block block = (Block) holder.value();

                flag |= !block.getStateDefinition().getProperties().isEmpty();
                flag1 |= block.defaultBlockState().hasBlockEntity();
                if (flag && flag1) {
                    break;
                }
            }

            if (flag) {
                builder.suggest(String.valueOf('['));
            }

            if (flag1) {
                builder.suggest(String.valueOf('{'));
            }
        }

        return this.suggestTag(builder, iregistry);
    }

    private CompletableFuture<Suggestions> suggestOpenPropertiesOrNbt(SuggestionsBuilder builder, Registry<Block> iregistry) {
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

    private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder builder, Registry<Block> iregistry) {
        return SharedSuggestionProvider.suggestResource(iregistry.getTagNames().map(TagKey::location), builder.createOffset(this.tagCursor).add(builder));
    }

    private CompletableFuture<Suggestions> suggestBlockIdOrTag(SuggestionsBuilder builder, Registry<Block> iregistry) {
        if (this.forTesting) {
            SharedSuggestionProvider.suggestResource(iregistry.getTagNames().map(TagKey::location), builder, String.valueOf('#'));
        }

        SharedSuggestionProvider.suggestResource((Iterable) iregistry.keySet(), builder);
        return builder.buildFuture();
    }

    public void readBlock() throws CommandSyntaxException {
        int i = this.reader.getCursor();

        this.id = ResourceLocation.read(this.reader);
        Block block = (Block) Registry.BLOCK.getOptional(this.id).orElseThrow(() -> {
            this.reader.setCursor(i);
            return BlockStateParser.ERROR_UNKNOWN_BLOCK.createWithContext(this.reader, this.id.toString());
        });

        this.definition = block.getStateDefinition();
        this.state = block.defaultBlockState();
    }

    public void readTag() throws CommandSyntaxException {
        if (!this.forTesting) {
            throw BlockStateParser.ERROR_NO_TAGS_ALLOWED.create();
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

        while (true) {
            if (this.reader.canRead() && this.reader.peek() != ']') {
                this.reader.skipWhitespace();
                int i = this.reader.getCursor();
                String s = this.reader.readString();
                Property<?> iblockstate = this.definition.getProperty(s);

                if (iblockstate == null) {
                    this.reader.setCursor(i);
                    throw BlockStateParser.ERROR_UNKNOWN_PROPERTY.createWithContext(this.reader, this.id.toString(), s);
                }

                if (this.properties.containsKey(iblockstate)) {
                    this.reader.setCursor(i);
                    throw BlockStateParser.ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), s);
                }

                this.reader.skipWhitespace();
                this.suggestions = this::suggestEquals;
                if (!this.reader.canRead() || this.reader.peek() != '=') {
                    throw BlockStateParser.ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), s);
                }

                this.reader.skip();
                this.reader.skipWhitespace();
                this.suggestions = (suggestionsbuilder, iregistry) -> {
                    return BlockStateParser.addSuggestions(suggestionsbuilder, iblockstate).buildFuture();
                };
                int j = this.reader.getCursor();

                this.setValue(iblockstate, this.reader.readString(), j);
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
                    throw BlockStateParser.ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
                }
            }

            if (this.reader.canRead()) {
                this.reader.skip();
                return;
            }

            throw BlockStateParser.ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
        }
    }

    public void readVagueProperties() throws CommandSyntaxException {
        this.reader.skip();
        this.suggestions = this::suggestVaguePropertyNameOrEnd;
        int i = -1;

        this.reader.skipWhitespace();

        while (true) {
            if (this.reader.canRead() && this.reader.peek() != ']') {
                this.reader.skipWhitespace();
                int j = this.reader.getCursor();
                String s = this.reader.readString();

                if (this.vagueProperties.containsKey(s)) {
                    this.reader.setCursor(j);
                    throw BlockStateParser.ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), s);
                }

                this.reader.skipWhitespace();
                if (!this.reader.canRead() || this.reader.peek() != '=') {
                    this.reader.setCursor(j);
                    throw BlockStateParser.ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), s);
                }

                this.reader.skip();
                this.reader.skipWhitespace();
                this.suggestions = (suggestionsbuilder, iregistry) -> {
                    return this.suggestVaguePropertyValue(suggestionsbuilder, iregistry, s);
                };
                i = this.reader.getCursor();
                String s1 = this.reader.readString();

                this.vagueProperties.put(s, s1);
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
                    throw BlockStateParser.ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
                }
            }

            if (this.reader.canRead()) {
                this.reader.skip();
                return;
            }

            if (i >= 0) {
                this.reader.setCursor(i);
            }

            throw BlockStateParser.ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
        }
    }

    public void readNbt() throws CommandSyntaxException {
        this.nbt = (new TagParser(this.reader)).readStruct();
    }

    private <T extends Comparable<T>> void setValue(Property<T> property, String value, int cursor) throws CommandSyntaxException {
        Optional<T> optional = property.getValue(value);

        if (optional.isPresent()) {
            this.state = (BlockState) this.state.setValue(property, (T) optional.get()); // CraftBukkit - decompile error
            this.properties.put(property, (Comparable) optional.get());
        } else {
            this.reader.setCursor(cursor);
            throw BlockStateParser.ERROR_INVALID_VALUE.createWithContext(this.reader, this.id.toString(), property.getName(), value);
        }
    }

    public static String serialize(BlockState state) {
        StringBuilder stringbuilder = new StringBuilder(Registry.BLOCK.getKey(state.getBlock()).toString());

        if (!state.getProperties().isEmpty()) {
            stringbuilder.append('[');
            boolean flag = false;

            for (UnmodifiableIterator unmodifiableiterator = state.getValues().entrySet().iterator(); unmodifiableiterator.hasNext(); flag = true) {
                Entry<Property<?>, Comparable<?>> entry = (Entry) unmodifiableiterator.next();

                if (flag) {
                    stringbuilder.append(',');
                }

                BlockStateParser.appendProperty(stringbuilder, (Property) entry.getKey(), (Comparable) entry.getValue());
            }

            stringbuilder.append(']');
        }

        return stringbuilder.toString();
    }

    private static <T extends Comparable<T>> void appendProperty(StringBuilder builder, Property<T> property, Comparable<?> value) {
        builder.append(property.getName());
        builder.append('=');
        builder.append(property.getName((T) value)); // CraftBukkit - decompile error
    }

    public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder builder, Registry<Block> iregistry) {
        return (CompletableFuture) this.suggestions.apply(builder.createOffset(this.reader.getCursor()), iregistry);
    }

    public Map<String, String> getVagueProperties() {
        return this.vagueProperties;
    }
}
