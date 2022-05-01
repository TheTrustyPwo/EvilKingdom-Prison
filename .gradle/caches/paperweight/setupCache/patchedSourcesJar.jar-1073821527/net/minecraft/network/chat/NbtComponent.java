package net.minecraft.network.chat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

public abstract class NbtComponent extends BaseComponent implements ContextAwareComponent {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final boolean interpreting;
    protected final Optional<Component> separator;
    protected final String nbtPathPattern;
    @Nullable
    protected final NbtPathArgument.NbtPath compiledNbtPath;

    @Nullable
    private static NbtPathArgument.NbtPath compileNbtPath(String rawPath) {
        try {
            return (new NbtPathArgument()).parse(new StringReader(rawPath));
        } catch (CommandSyntaxException var2) {
            return null;
        }
    }

    public NbtComponent(String rawPath, boolean interpret, Optional<Component> separator) {
        this(rawPath, compileNbtPath(rawPath), interpret, separator);
    }

    protected NbtComponent(String rawPath, @Nullable NbtPathArgument.NbtPath path, boolean interpret, Optional<Component> separator) {
        this.nbtPathPattern = rawPath;
        this.compiledNbtPath = path;
        this.interpreting = interpret;
        this.separator = separator;
    }

    protected abstract Stream<CompoundTag> getData(CommandSourceStack source) throws CommandSyntaxException;

    public String getNbtPath() {
        return this.nbtPathPattern;
    }

    public boolean isInterpreting() {
        return this.interpreting;
    }

    @Override
    public MutableComponent resolve(@Nullable CommandSourceStack source, @Nullable Entity sender, int depth) throws CommandSyntaxException {
        if (source != null && this.compiledNbtPath != null) {
            Stream<String> stream = this.getData(source).flatMap((nbt) -> {
                try {
                    return this.compiledNbtPath.get(nbt).stream();
                } catch (CommandSyntaxException var3) {
                    return Stream.empty();
                }
            }).map(Tag::getAsString);
            if (this.interpreting) {
                Component component = DataFixUtils.orElse(ComponentUtils.updateForEntity(source, this.separator, sender, depth), ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR);
                return stream.flatMap((text) -> {
                    try {
                        MutableComponent mutableComponent = Component.Serializer.fromJson(text);
                        return Stream.of(ComponentUtils.updateForEntity(source, mutableComponent, sender, depth));
                    } catch (Exception var5) {
                        LOGGER.warn("Failed to parse component: {}", text, var5);
                        return Stream.of();
                    }
                }).reduce((accumulator, current) -> {
                    return accumulator.append(component).append(current);
                }).orElseGet(() -> {
                    return new TextComponent("");
                });
            } else {
                return ComponentUtils.updateForEntity(source, this.separator, sender, depth).map((text) -> {
                    return stream.map((string) -> {
                        return new TextComponent(string);
                    }).reduce((accumulator, current) -> {
                        return accumulator.append(text).append(current);
                    }).orElseGet(() -> {
                        return new TextComponent("");
                    });
                }).orElseGet(() -> {
                    return new TextComponent(stream.collect(Collectors.joining(", ")));
                });
            }
        } else {
            return new TextComponent("");
        }
    }

    public static class BlockNbtComponent extends NbtComponent {
        private final String posPattern;
        @Nullable
        private final Coordinates compiledPos;

        public BlockNbtComponent(String rawPath, boolean rawJson, String rawPos, Optional<Component> separator) {
            super(rawPath, rawJson, separator);
            this.posPattern = rawPos;
            this.compiledPos = this.compilePos(this.posPattern);
        }

        @Nullable
        private Coordinates compilePos(String rawPos) {
            try {
                return BlockPosArgument.blockPos().parse(new StringReader(rawPos));
            } catch (CommandSyntaxException var3) {
                return null;
            }
        }

        private BlockNbtComponent(String rawPath, @Nullable NbtPathArgument.NbtPath path, boolean interpret, String rawPos, @Nullable Coordinates pos, Optional<Component> separator) {
            super(rawPath, path, interpret, separator);
            this.posPattern = rawPos;
            this.compiledPos = pos;
        }

        @Nullable
        public String getPos() {
            return this.posPattern;
        }

        @Override
        public NbtComponent.BlockNbtComponent plainCopy() {
            return new NbtComponent.BlockNbtComponent(this.nbtPathPattern, this.compiledNbtPath, this.interpreting, this.posPattern, this.compiledPos, this.separator);
        }

        @Override
        protected Stream<CompoundTag> getData(CommandSourceStack source) {
            if (this.compiledPos != null) {
                ServerLevel serverLevel = source.getLevel();
                BlockPos blockPos = this.compiledPos.getBlockPos(source);
                if (serverLevel.isLoaded(blockPos)) {
                    BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
                    if (blockEntity != null) {
                        return Stream.of(blockEntity.saveWithFullMetadata());
                    }
                }
            }

            return Stream.empty();
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof NbtComponent.BlockNbtComponent)) {
                return false;
            } else {
                NbtComponent.BlockNbtComponent blockNbtComponent = (NbtComponent.BlockNbtComponent)object;
                return Objects.equals(this.posPattern, blockNbtComponent.posPattern) && Objects.equals(this.nbtPathPattern, blockNbtComponent.nbtPathPattern) && super.equals(object);
            }
        }

        @Override
        public String toString() {
            return "BlockPosArgument{pos='" + this.posPattern + "'path='" + this.nbtPathPattern + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
        }
    }

    public static class EntityNbtComponent extends NbtComponent {
        private final String selectorPattern;
        @Nullable
        private final EntitySelector compiledSelector;

        public EntityNbtComponent(String rawPath, boolean interpret, String rawSelector, Optional<Component> separator) {
            super(rawPath, interpret, separator);
            this.selectorPattern = rawSelector;
            this.compiledSelector = compileSelector(rawSelector);
        }

        @Nullable
        private static EntitySelector compileSelector(String rawSelector) {
            try {
                EntitySelectorParser entitySelectorParser = new EntitySelectorParser(new StringReader(rawSelector));
                return entitySelectorParser.parse();
            } catch (CommandSyntaxException var2) {
                return null;
            }
        }

        private EntityNbtComponent(String rawPath, @Nullable NbtPathArgument.NbtPath path, boolean interpret, String rawSelector, @Nullable EntitySelector selector, Optional<Component> separator) {
            super(rawPath, path, interpret, separator);
            this.selectorPattern = rawSelector;
            this.compiledSelector = selector;
        }

        public String getSelector() {
            return this.selectorPattern;
        }

        @Override
        public NbtComponent.EntityNbtComponent plainCopy() {
            return new NbtComponent.EntityNbtComponent(this.nbtPathPattern, this.compiledNbtPath, this.interpreting, this.selectorPattern, this.compiledSelector, this.separator);
        }

        @Override
        protected Stream<CompoundTag> getData(CommandSourceStack source) throws CommandSyntaxException {
            if (this.compiledSelector != null) {
                List<? extends Entity> list = this.compiledSelector.findEntities(source);
                return list.stream().map(NbtPredicate::getEntityTagToCompare);
            } else {
                return Stream.empty();
            }
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof NbtComponent.EntityNbtComponent)) {
                return false;
            } else {
                NbtComponent.EntityNbtComponent entityNbtComponent = (NbtComponent.EntityNbtComponent)object;
                return Objects.equals(this.selectorPattern, entityNbtComponent.selectorPattern) && Objects.equals(this.nbtPathPattern, entityNbtComponent.nbtPathPattern) && super.equals(object);
            }
        }

        @Override
        public String toString() {
            return "EntityNbtComponent{selector='" + this.selectorPattern + "'path='" + this.nbtPathPattern + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
        }
    }

    public static class StorageNbtComponent extends NbtComponent {
        private final ResourceLocation id;

        public StorageNbtComponent(String rawPath, boolean interpret, ResourceLocation id, Optional<Component> separator) {
            super(rawPath, interpret, separator);
            this.id = id;
        }

        public StorageNbtComponent(String rawPath, @Nullable NbtPathArgument.NbtPath path, boolean interpret, ResourceLocation id, Optional<Component> separator) {
            super(rawPath, path, interpret, separator);
            this.id = id;
        }

        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public NbtComponent.StorageNbtComponent plainCopy() {
            return new NbtComponent.StorageNbtComponent(this.nbtPathPattern, this.compiledNbtPath, this.interpreting, this.id, this.separator);
        }

        @Override
        protected Stream<CompoundTag> getData(CommandSourceStack source) {
            CompoundTag compoundTag = source.getServer().getCommandStorage().get(this.id);
            return Stream.of(compoundTag);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof NbtComponent.StorageNbtComponent)) {
                return false;
            } else {
                NbtComponent.StorageNbtComponent storageNbtComponent = (NbtComponent.StorageNbtComponent)object;
                return Objects.equals(this.id, storageNbtComponent.id) && Objects.equals(this.nbtPathPattern, storageNbtComponent.nbtPathPattern) && super.equals(object);
            }
        }

        @Override
        public String toString() {
            return "StorageNbtComponent{id='" + this.id + "'path='" + this.nbtPathPattern + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
        }
    }
}
