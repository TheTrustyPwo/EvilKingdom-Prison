package net.minecraft.advancements;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.apache.commons.lang3.ArrayUtils;

public class Advancement {
    @Nullable
    private final Advancement parent;
    @Nullable
    private final DisplayInfo display;
    private final AdvancementRewards rewards;
    private final ResourceLocation id;
    private final Map<String, Criterion> criteria;
    private final String[][] requirements;
    private final Set<Advancement> children = Sets.newLinkedHashSet();
    private final Component chatComponent;

    public Advancement(ResourceLocation id, @Nullable Advancement parent, @Nullable DisplayInfo display, AdvancementRewards rewards, Map<String, Criterion> criteria, String[][] requirements) {
        this.id = id;
        this.display = display;
        this.criteria = ImmutableMap.copyOf(criteria);
        this.parent = parent;
        this.rewards = rewards;
        this.requirements = requirements;
        if (parent != null) {
            parent.addChild(this);
        }

        if (display == null) {
            this.chatComponent = new TextComponent(id.toString());
        } else {
            Component component = display.getTitle();
            ChatFormatting chatFormatting = display.getFrame().getChatColor();
            Component component2 = ComponentUtils.mergeStyles(component.copy(), Style.EMPTY.withColor(chatFormatting)).append("\n").append(display.getDescription());
            Component component3 = component.copy().withStyle((style) -> {
                return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component2));
            });
            this.chatComponent = ComponentUtils.wrapInSquareBrackets(component3).withStyle(chatFormatting);
        }

    }

    public Advancement.Builder deconstruct() {
        return new Advancement.Builder(this.parent == null ? null : this.parent.getId(), this.display, this.rewards, this.criteria, this.requirements);
    }

    @Nullable
    public Advancement getParent() {
        return this.parent;
    }

    @Nullable
    public DisplayInfo getDisplay() {
        return this.display;
    }

    public AdvancementRewards getRewards() {
        return this.rewards;
    }

    @Override
    public String toString() {
        return "SimpleAdvancement{id=" + this.getId() + ", parent=" + (this.parent == null ? "null" : this.parent.getId()) + ", display=" + this.display + ", rewards=" + this.rewards + ", criteria=" + this.criteria + ", requirements=" + Arrays.deepToString(this.requirements) + "}";
    }

    public Iterable<Advancement> getChildren() {
        return this.children;
    }

    public Map<String, Criterion> getCriteria() {
        return this.criteria;
    }

    public int getMaxCriteraRequired() {
        return this.requirements.length;
    }

    public void addChild(Advancement child) {
        this.children.add(child);
    }

    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Advancement)) {
            return false;
        } else {
            Advancement advancement = (Advancement)object;
            return this.id.equals(advancement.id);
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public String[][] getRequirements() {
        return this.requirements;
    }

    public Component getChatComponent() {
        return this.chatComponent;
    }

    public static class Builder {
        @Nullable
        private ResourceLocation parentId;
        @Nullable
        private Advancement parent;
        @Nullable
        private DisplayInfo display;
        private AdvancementRewards rewards = AdvancementRewards.EMPTY;
        private Map<String, Criterion> criteria = Maps.newLinkedHashMap();
        @Nullable
        private String[][] requirements;
        private RequirementsStrategy requirementsStrategy = RequirementsStrategy.AND;

        Builder(@Nullable ResourceLocation parentId, @Nullable DisplayInfo display, AdvancementRewards rewards, Map<String, Criterion> criteria, String[][] requirements) {
            this.parentId = parentId;
            this.display = display;
            this.rewards = rewards;
            this.criteria = criteria;
            this.requirements = requirements;
        }

        private Builder() {
        }

        public static Advancement.Builder advancement() {
            return new Advancement.Builder();
        }

        public Advancement.Builder parent(Advancement parent) {
            this.parent = parent;
            return this;
        }

        public Advancement.Builder parent(ResourceLocation parentId) {
            this.parentId = parentId;
            return this;
        }

        public Advancement.Builder display(ItemStack icon, Component title, Component description, @Nullable ResourceLocation background, FrameType frame, boolean showToast, boolean announceToChat, boolean hidden) {
            return this.display(new DisplayInfo(icon, title, description, background, frame, showToast, announceToChat, hidden));
        }

        public Advancement.Builder display(ItemLike icon, Component title, Component description, @Nullable ResourceLocation background, FrameType frame, boolean showToast, boolean announceToChat, boolean hidden) {
            return this.display(new DisplayInfo(new ItemStack(icon.asItem()), title, description, background, frame, showToast, announceToChat, hidden));
        }

        public Advancement.Builder display(DisplayInfo display) {
            this.display = display;
            return this;
        }

        public Advancement.Builder rewards(AdvancementRewards.Builder builder) {
            return this.rewards(builder.build());
        }

        public Advancement.Builder rewards(AdvancementRewards rewards) {
            this.rewards = rewards;
            return this;
        }

        public Advancement.Builder addCriterion(String name, CriterionTriggerInstance conditions) {
            return this.addCriterion(name, new Criterion(conditions));
        }

        public Advancement.Builder addCriterion(String name, Criterion criterion) {
            if (this.criteria.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate criterion " + name);
            } else {
                this.criteria.put(name, criterion);
                return this;
            }
        }

        public Advancement.Builder requirements(RequirementsStrategy merger) {
            this.requirementsStrategy = merger;
            return this;
        }

        public Advancement.Builder requirements(String[][] requirements) {
            this.requirements = requirements;
            return this;
        }

        public boolean canBuild(Function<ResourceLocation, Advancement> parentProvider) {
            if (this.parentId == null) {
                return true;
            } else {
                if (this.parent == null) {
                    this.parent = parentProvider.apply(this.parentId);
                }

                return this.parent != null;
            }
        }

        public Advancement build(ResourceLocation id) {
            if (!this.canBuild((idx) -> {
                return null;
            })) {
                throw new IllegalStateException("Tried to build incomplete advancement!");
            } else {
                if (this.requirements == null) {
                    this.requirements = this.requirementsStrategy.createRequirements(this.criteria.keySet());
                }

                return new Advancement(id, this.parent, this.display, this.rewards, this.criteria, this.requirements);
            }
        }

        public Advancement save(Consumer<Advancement> consumer, String id) {
            Advancement advancement = this.build(new ResourceLocation(id));
            consumer.accept(advancement);
            return advancement;
        }

        public JsonObject serializeToJson() {
            if (this.requirements == null) {
                this.requirements = this.requirementsStrategy.createRequirements(this.criteria.keySet());
            }

            JsonObject jsonObject = new JsonObject();
            if (this.parent != null) {
                jsonObject.addProperty("parent", this.parent.getId().toString());
            } else if (this.parentId != null) {
                jsonObject.addProperty("parent", this.parentId.toString());
            }

            if (this.display != null) {
                jsonObject.add("display", this.display.serializeToJson());
            }

            jsonObject.add("rewards", this.rewards.serializeToJson());
            JsonObject jsonObject2 = new JsonObject();

            for(Entry<String, Criterion> entry : this.criteria.entrySet()) {
                jsonObject2.add(entry.getKey(), entry.getValue().serializeToJson());
            }

            jsonObject.add("criteria", jsonObject2);
            JsonArray jsonArray = new JsonArray();

            for(String[] strings : this.requirements) {
                JsonArray jsonArray2 = new JsonArray();

                for(String string : strings) {
                    jsonArray2.add(string);
                }

                jsonArray.add(jsonArray2);
            }

            jsonObject.add("requirements", jsonArray);
            return jsonObject;
        }

        public void serializeToNetwork(FriendlyByteBuf buf) {
            if (this.requirements == null) {
                this.requirements = this.requirementsStrategy.createRequirements(this.criteria.keySet());
            }

            if (this.parentId == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                buf.writeResourceLocation(this.parentId);
            }

            if (this.display == null) {
                buf.writeBoolean(false);
            } else {
                buf.writeBoolean(true);
                this.display.serializeToNetwork(buf);
            }

            Criterion.serializeToNetwork(this.criteria, buf);
            buf.writeVarInt(this.requirements.length);

            for(String[] strings : this.requirements) {
                buf.writeVarInt(strings.length);

                for(String string : strings) {
                    buf.writeUtf(string);
                }
            }

        }

        @Override
        public String toString() {
            return "Task Advancement{parentId=" + this.parentId + ", display=" + this.display + ", rewards=" + this.rewards + ", criteria=" + this.criteria + ", requirements=" + Arrays.deepToString(this.requirements) + "}";
        }

        public static Advancement.Builder fromJson(JsonObject obj, DeserializationContext predicateDeserializer) {
            ResourceLocation resourceLocation = obj.has("parent") ? new ResourceLocation(GsonHelper.getAsString(obj, "parent")) : null;
            DisplayInfo displayInfo = obj.has("display") ? DisplayInfo.fromJson(GsonHelper.getAsJsonObject(obj, "display")) : null;
            AdvancementRewards advancementRewards = obj.has("rewards") ? AdvancementRewards.deserialize(GsonHelper.getAsJsonObject(obj, "rewards")) : AdvancementRewards.EMPTY;
            Map<String, Criterion> map = Criterion.criteriaFromJson(GsonHelper.getAsJsonObject(obj, "criteria"), predicateDeserializer);
            if (map.isEmpty()) {
                throw new JsonSyntaxException("Advancement criteria cannot be empty");
            } else {
                JsonArray jsonArray = GsonHelper.getAsJsonArray(obj, "requirements", new JsonArray());
                String[][] strings = new String[jsonArray.size()][];

                for(int i = 0; i < jsonArray.size(); ++i) {
                    JsonArray jsonArray2 = GsonHelper.convertToJsonArray(jsonArray.get(i), "requirements[" + i + "]");
                    strings[i] = new String[jsonArray2.size()];

                    for(int j = 0; j < jsonArray2.size(); ++j) {
                        strings[i][j] = GsonHelper.convertToString(jsonArray2.get(j), "requirements[" + i + "][" + j + "]");
                    }
                }

                if (strings.length == 0) {
                    strings = new String[map.size()][];
                    int k = 0;

                    for(String string : map.keySet()) {
                        strings[k++] = new String[]{string};
                    }
                }

                for(String[] strings2 : strings) {
                    if (strings2.length == 0 && map.isEmpty()) {
                        throw new JsonSyntaxException("Requirement entry cannot be empty");
                    }

                    for(String string2 : strings2) {
                        if (!map.containsKey(string2)) {
                            throw new JsonSyntaxException("Unknown required criterion '" + string2 + "'");
                        }
                    }
                }

                for(String string3 : map.keySet()) {
                    boolean bl = false;

                    for(String[] strings3 : strings) {
                        if (ArrayUtils.contains(strings3, string3)) {
                            bl = true;
                            break;
                        }
                    }

                    if (!bl) {
                        throw new JsonSyntaxException("Criterion '" + string3 + "' isn't a requirement for completion. This isn't supported behaviour, all criteria must be required.");
                    }
                }

                return new Advancement.Builder(resourceLocation, displayInfo, advancementRewards, map, strings);
            }
        }

        public static Advancement.Builder fromNetwork(FriendlyByteBuf buf) {
            ResourceLocation resourceLocation = buf.readBoolean() ? buf.readResourceLocation() : null;
            DisplayInfo displayInfo = buf.readBoolean() ? DisplayInfo.fromNetwork(buf) : null;
            Map<String, Criterion> map = Criterion.criteriaFromNetwork(buf);
            String[][] strings = new String[buf.readVarInt()][];

            for(int i = 0; i < strings.length; ++i) {
                strings[i] = new String[buf.readVarInt()];

                for(int j = 0; j < strings[i].length; ++j) {
                    strings[i][j] = buf.readUtf();
                }
            }

            return new Advancement.Builder(resourceLocation, displayInfo, AdvancementRewards.EMPTY, map, strings);
        }

        public Map<String, Criterion> getCriteria() {
            return this.criteria;
        }
    }
}
