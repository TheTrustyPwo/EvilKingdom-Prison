package net.minecraft.network.chat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class HoverEvent {
    static final Logger LOGGER = LogUtils.getLogger();
    private final HoverEvent.Action<?> action;
    private final Object value;

    public <T> HoverEvent(HoverEvent.Action<T> action, T contents) {
        this.action = action;
        this.value = contents;
    }

    public HoverEvent.Action<?> getAction() {
        return this.action;
    }

    @Nullable
    public <T> T getValue(HoverEvent.Action<T> action) {
        return (T)(this.action == action ? action.cast(this.value) : null);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            HoverEvent hoverEvent = (HoverEvent)object;
            return this.action == hoverEvent.action && Objects.equals(this.value, hoverEvent.value);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "HoverEvent{action=" + this.action + ", value='" + this.value + "'}";
    }

    @Override
    public int hashCode() {
        int i = this.action.hashCode();
        return 31 * i + (this.value != null ? this.value.hashCode() : 0);
    }

    @Nullable
    public static HoverEvent deserialize(JsonObject json) {
        String string = GsonHelper.getAsString(json, "action", (String)null);
        if (string == null) {
            return null;
        } else {
            HoverEvent.Action<?> action = HoverEvent.Action.getByName(string);
            if (action == null) {
                return null;
            } else {
                JsonElement jsonElement = json.get("contents");
                if (jsonElement != null) {
                    return action.deserialize(jsonElement);
                } else {
                    Component component = Component.Serializer.fromJson(json.get("value"));
                    return component != null ? action.deserializeFromLegacy(component) : null;
                }
            }
        }
    }

    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("action", this.action.getName());
        jsonObject.add("contents", this.action.serializeArg(this.value));
        return jsonObject;
    }

    public static class Action<T> {
        public static final HoverEvent.Action<Component> SHOW_TEXT = new HoverEvent.Action<>("show_text", true, Component.Serializer::fromJson, Component.Serializer::toJsonTree, Function.identity());
        public static final HoverEvent.Action<HoverEvent.ItemStackInfo> SHOW_ITEM = new HoverEvent.Action<>("show_item", true, HoverEvent.ItemStackInfo::create, HoverEvent.ItemStackInfo::serialize, HoverEvent.ItemStackInfo::create);
        public static final HoverEvent.Action<HoverEvent.EntityTooltipInfo> SHOW_ENTITY = new HoverEvent.Action<>("show_entity", true, HoverEvent.EntityTooltipInfo::create, HoverEvent.EntityTooltipInfo::serialize, HoverEvent.EntityTooltipInfo::create);
        private static final Map<String, HoverEvent.Action<?>> LOOKUP = Stream.of(SHOW_TEXT, SHOW_ITEM, SHOW_ENTITY).collect(ImmutableMap.toImmutableMap(HoverEvent.Action::getName, (action) -> {
            return action;
        }));
        private final String name;
        private final boolean allowFromServer;
        private final Function<JsonElement, T> argDeserializer;
        private final Function<T, JsonElement> argSerializer;
        private final Function<Component, T> legacyArgDeserializer;

        public Action(String name, boolean parsable, Function<JsonElement, T> deserializer, Function<T, JsonElement> serializer, Function<Component, T> legacyDeserializer) {
            this.name = name;
            this.allowFromServer = parsable;
            this.argDeserializer = deserializer;
            this.argSerializer = serializer;
            this.legacyArgDeserializer = legacyDeserializer;
        }

        public boolean isAllowedFromServer() {
            return this.allowFromServer;
        }

        public String getName() {
            return this.name;
        }

        @Nullable
        public static HoverEvent.Action<?> getByName(String name) {
            return LOOKUP.get(name);
        }

        T cast(Object o) {
            return (T)o;
        }

        @Nullable
        public HoverEvent deserialize(JsonElement contents) {
            T object = this.argDeserializer.apply(contents);
            return object == null ? null : new HoverEvent(this, object);
        }

        @Nullable
        public HoverEvent deserializeFromLegacy(Component value) {
            T object = this.legacyArgDeserializer.apply(value);
            return object == null ? null : new HoverEvent(this, object);
        }

        public JsonElement serializeArg(Object contents) {
            return this.argSerializer.apply(this.cast(contents));
        }

        @Override
        public String toString() {
            return "<action " + this.name + ">";
        }
    }

    public static class EntityTooltipInfo {
        public final EntityType<?> type;
        public final UUID id;
        @Nullable
        public final Component name;
        @Nullable
        private List<Component> linesCache;

        public EntityTooltipInfo(EntityType<?> entityType, UUID uuid, @Nullable Component name) {
            this.type = entityType;
            this.id = uuid;
            this.name = name;
        }

        @Nullable
        public static HoverEvent.EntityTooltipInfo create(JsonElement json) {
            if (!json.isJsonObject()) {
                return null;
            } else {
                JsonObject jsonObject = json.getAsJsonObject();
                EntityType<?> entityType = Registry.ENTITY_TYPE.get(new ResourceLocation(GsonHelper.getAsString(jsonObject, "type")));
                UUID uUID = UUID.fromString(GsonHelper.getAsString(jsonObject, "id"));
                Component component = Component.Serializer.fromJson(jsonObject.get("name"));
                return new HoverEvent.EntityTooltipInfo(entityType, uUID, component);
            }
        }

        @Nullable
        public static HoverEvent.EntityTooltipInfo create(Component text) {
            try {
                CompoundTag compoundTag = TagParser.parseTag(text.getString());
                Component component = Component.Serializer.fromJson(compoundTag.getString("name"));
                EntityType<?> entityType = Registry.ENTITY_TYPE.get(new ResourceLocation(compoundTag.getString("type")));
                UUID uUID = UUID.fromString(compoundTag.getString("id"));
                return new HoverEvent.EntityTooltipInfo(entityType, uUID, component);
            } catch (Exception var5) {
                return null;
            }
        }

        public JsonElement serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", Registry.ENTITY_TYPE.getKey(this.type).toString());
            jsonObject.addProperty("id", this.id.toString());
            if (this.name != null) {
                jsonObject.add("name", Component.Serializer.toJsonTree(this.name));
            }

            return jsonObject;
        }

        public List<Component> getTooltipLines() {
            if (this.linesCache == null) {
                this.linesCache = Lists.newArrayList();
                if (this.name != null) {
                    this.linesCache.add(this.name);
                }

                this.linesCache.add(new TranslatableComponent("gui.entity_tooltip.type", this.type.getDescription()));
                this.linesCache.add(new TextComponent(this.id.toString()));
            }

            return this.linesCache;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                HoverEvent.EntityTooltipInfo entityTooltipInfo = (HoverEvent.EntityTooltipInfo)object;
                return this.type.equals(entityTooltipInfo.type) && this.id.equals(entityTooltipInfo.id) && Objects.equals(this.name, entityTooltipInfo.name);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.type.hashCode();
            i = 31 * i + this.id.hashCode();
            return 31 * i + (this.name != null ? this.name.hashCode() : 0);
        }
    }

    public static class ItemStackInfo {
        private final Item item;
        private final int count;
        @Nullable
        private final CompoundTag tag;
        @Nullable
        private ItemStack itemStack;

        ItemStackInfo(Item item, int count, @Nullable CompoundTag nbt) {
            this.item = item;
            this.count = count;
            this.tag = nbt;
        }

        public ItemStackInfo(ItemStack stack) {
            this(stack.getItem(), stack.getCount(), stack.getTag() != null ? stack.getTag().copy() : null);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                HoverEvent.ItemStackInfo itemStackInfo = (HoverEvent.ItemStackInfo)object;
                return this.count == itemStackInfo.count && this.item.equals(itemStackInfo.item) && Objects.equals(this.tag, itemStackInfo.tag);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.item.hashCode();
            i = 31 * i + this.count;
            return 31 * i + (this.tag != null ? this.tag.hashCode() : 0);
        }

        public ItemStack getItemStack() {
            if (this.itemStack == null) {
                this.itemStack = new ItemStack(this.item, this.count);
                if (this.tag != null) {
                    this.itemStack.setTag(this.tag);
                }
            }

            return this.itemStack;
        }

        private static HoverEvent.ItemStackInfo create(JsonElement json) {
            if (json.isJsonPrimitive()) {
                return new HoverEvent.ItemStackInfo(Registry.ITEM.get(new ResourceLocation(json.getAsString())), 1, (CompoundTag)null);
            } else {
                JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "item");
                Item item = Registry.ITEM.get(new ResourceLocation(GsonHelper.getAsString(jsonObject, "id")));
                int i = GsonHelper.getAsInt(jsonObject, "count", 1);
                if (jsonObject.has("tag")) {
                    String string = GsonHelper.getAsString(jsonObject, "tag");

                    try {
                        CompoundTag compoundTag = TagParser.parseTag(string);
                        return new HoverEvent.ItemStackInfo(item, i, compoundTag);
                    } catch (CommandSyntaxException var6) {
                        HoverEvent.LOGGER.warn("Failed to parse tag: {}", string, var6);
                    }
                }

                return new HoverEvent.ItemStackInfo(item, i, (CompoundTag)null);
            }
        }

        @Nullable
        private static HoverEvent.ItemStackInfo create(Component text) {
            try {
                CompoundTag compoundTag = TagParser.parseTag(text.getString());
                return new HoverEvent.ItemStackInfo(ItemStack.of(compoundTag));
            } catch (CommandSyntaxException var2) {
                HoverEvent.LOGGER.warn("Failed to parse item tag: {}", text, var2);
                return null;
            }
        }

        private JsonElement serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", Registry.ITEM.getKey(this.item).toString());
            if (this.count != 1) {
                jsonObject.addProperty("count", this.count);
            }

            if (this.tag != null) {
                jsonObject.addProperty("tag", this.tag.toString());
            }

            return jsonObject;
        }
    }
}
