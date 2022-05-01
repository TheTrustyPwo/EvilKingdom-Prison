package net.minecraft.server.packs.repository;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import org.slf4j.Logger;

public class Pack implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String id;
    private final Supplier<PackResources> supplier;
    private final Component title;
    private final Component description;
    private final PackCompatibility compatibility;
    private final Pack.Position defaultPosition;
    private final boolean required;
    private final boolean fixedPosition;
    private final PackSource packSource;

    @Nullable
    public static Pack create(String name, boolean alwaysEnabled, Supplier<PackResources> packFactory, Pack.PackConstructor profileFactory, Pack.Position insertionPosition, PackSource packSource) {
        try {
            PackResources packResources = packFactory.get();

            Pack var8;
            label54: {
                try {
                    PackMetadataSection packMetadataSection = packResources.getMetadataSection(PackMetadataSection.SERIALIZER);
                    if (packMetadataSection != null) {
                        var8 = profileFactory.create(name, new TextComponent(packResources.getName()), alwaysEnabled, packFactory, packMetadataSection, insertionPosition, packSource);
                        break label54;
                    }

                    LOGGER.warn("Couldn't find pack meta for pack {}", (Object)name);
                } catch (Throwable var10) {
                    if (packResources != null) {
                        try {
                            packResources.close();
                        } catch (Throwable var9) {
                            var10.addSuppressed(var9);
                        }
                    }

                    throw var10;
                }

                if (packResources != null) {
                    packResources.close();
                }

                return null;
            }

            if (packResources != null) {
                packResources.close();
            }

            return var8;
        } catch (IOException var11) {
            LOGGER.warn("Couldn't get pack info for: {}", (Object)var11.toString());
            return null;
        }
    }

    public Pack(String name, boolean alwaysEnabled, Supplier<PackResources> packFactory, Component displayName, Component description, PackCompatibility compatibility, Pack.Position direction, boolean pinned, PackSource source) {
        this.id = name;
        this.supplier = packFactory;
        this.title = displayName;
        this.description = description;
        this.compatibility = compatibility;
        this.required = alwaysEnabled;
        this.defaultPosition = direction;
        this.fixedPosition = pinned;
        this.packSource = source;
    }

    public Pack(String name, Component displayName, boolean alwaysEnabled, Supplier<PackResources> packFactory, PackMetadataSection metadata, PackType type, Pack.Position direction, PackSource source) {
        this(name, alwaysEnabled, packFactory, displayName, metadata.getDescription(), PackCompatibility.forMetadata(metadata, type), direction, false, source);
    }

    public Component getTitle() {
        return this.title;
    }

    public Component getDescription() {
        return this.description;
    }

    public Component getChatLink(boolean enabled) {
        return ComponentUtils.wrapInSquareBrackets(this.packSource.decorate(new TextComponent(this.id))).withStyle((style) -> {
            return style.withColor(enabled ? ChatFormatting.GREEN : ChatFormatting.RED).withInsertion(StringArgumentType.escapeIfRequired(this.id)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (new TextComponent("")).append(this.title).append("\n").append(this.description)));
        });
    }

    public PackCompatibility getCompatibility() {
        return this.compatibility;
    }

    public PackResources open() {
        return this.supplier.get();
    }

    public String getId() {
        return this.id;
    }

    public boolean isRequired() {
        return this.required;
    }

    public boolean isFixedPosition() {
        return this.fixedPosition;
    }

    public Pack.Position getDefaultPosition() {
        return this.defaultPosition;
    }

    public PackSource getPackSource() {
        return this.packSource;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Pack)) {
            return false;
        } else {
            Pack pack = (Pack)object;
            return this.id.equals(pack.id);
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public void close() {
    }

    @FunctionalInterface
    public interface PackConstructor {
        @Nullable
        Pack create(String name, Component displayName, boolean alwaysEnabled, Supplier<PackResources> packFactory, PackMetadataSection metadata, Pack.Position initialPosition, PackSource source);
    }

    public static enum Position {
        TOP,
        BOTTOM;

        public <T> int insert(List<T> items, T item, Function<T, Pack> profileGetter, boolean listInverted) {
            Pack.Position position = listInverted ? this.opposite() : this;
            if (position == BOTTOM) {
                int i;
                for(i = 0; i < items.size(); ++i) {
                    Pack pack = profileGetter.apply(items.get(i));
                    if (!pack.isFixedPosition() || pack.getDefaultPosition() != this) {
                        break;
                    }
                }

                items.add(i, item);
                return i;
            } else {
                int j;
                for(j = items.size() - 1; j >= 0; --j) {
                    Pack pack2 = profileGetter.apply(items.get(j));
                    if (!pack2.isFixedPosition() || pack2.getDefaultPosition() != this) {
                        break;
                    }
                }

                items.add(j + 1, item);
                return j + 1;
            }
        }

        public Pack.Position opposite() {
            return this == TOP ? BOTTOM : TOP;
        }
    }
}
