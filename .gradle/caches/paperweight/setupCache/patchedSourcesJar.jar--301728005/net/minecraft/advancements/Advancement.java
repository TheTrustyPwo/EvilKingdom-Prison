package net.minecraft.advancements;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
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
    public final org.bukkit.advancement.Advancement bukkit = new org.bukkit.craftbukkit.v1_18_R2.advancement.CraftAdvancement(this); // CraftBukkit

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
            Component ichatbasecomponent = display.getTitle();
            ChatFormatting enumchatformat = display.getFrame().getChatColor();
            MutableComponent ichatmutablecomponent = ComponentUtils.mergeStyles(ichatbasecomponent.copy(), Style.EMPTY.withColor(enumchatformat)).append("\n").append(display.getDescription());
            MutableComponent ichatmutablecomponent1 = ichatbasecomponent.copy().withStyle((chatmodifier) -> {
                return chatmodifier.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, ichatmutablecomponent));
            });

            this.chatComponent = ComponentUtils.wrapInSquareBrackets(ichatmutablecomponent1).withStyle(enumchatformat);
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

    public String toString() {
        ResourceLocation minecraftkey = this.getId();

        return "SimpleAdvancement{id=" + minecraftkey + ", parent=" + (this.parent == null ? "null" : this.parent.getId()) + ", display=" + this.display + ", rewards=" + this.rewards + ", criteria=" + this.criteria + ", requirements=" + Arrays.deepToString(this.requirements) + "}";
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

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Advancement)) {
            return false;
        } else {
            Advancement advancement = (Advancement) object;

            return this.id.equals(advancement.id);
        }
    }

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
        private AdvancementRewards rewards;
        private Map<String, Criterion> criteria;
        @Nullable
        private String[][] requirements;
        private RequirementsStrategy requirementsStrategy;

        Builder(@Nullable ResourceLocation parentId, @Nullable DisplayInfo display, AdvancementRewards rewards, Map<String, Criterion> criteria, String[][] requirements) {
            this.rewards = AdvancementRewards.EMPTY;
            this.criteria = Maps.newLinkedHashMap();
            this.requirementsStrategy = RequirementsStrategy.AND;
            this.parentId = parentId;
            this.display = display;
            this.rewards = rewards;
            this.criteria = criteria;
            this.requirements = requirements;
        }

        private Builder() {
            this.rewards = AdvancementRewards.EMPTY;
            this.criteria = Maps.newLinkedHashMap();
            this.requirementsStrategy = RequirementsStrategy.AND;
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
                    this.parent = (Advancement) parentProvider.apply(this.parentId);
                }

                return this.parent != null;
            }
        }

        public Advancement build(ResourceLocation id) {
            if (!this.canBuild((minecraftkey1) -> {
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

            JsonObject jsonobject = new JsonObject();

            if (this.parent != null) {
                jsonobject.addProperty("parent", this.parent.getId().toString());
            } else if (this.parentId != null) {
                jsonobject.addProperty("parent", this.parentId.toString());
            }

            if (this.display != null) {
                jsonobject.add("display", this.display.serializeToJson());
            }

            jsonobject.add("rewards", this.rewards.serializeToJson());
            JsonObject jsonobject1 = new JsonObject();
            Iterator iterator = this.criteria.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<String, Criterion> entry = (Entry) iterator.next();

                jsonobject1.add((String) entry.getKey(), ((Criterion) entry.getValue()).serializeToJson());
            }

            jsonobject.add("criteria", jsonobject1);
            JsonArray jsonarray = new JsonArray();
            String[][] astring = this.requirements;
            int i = astring.length;

            for (int j = 0; j < i; ++j) {
                String[] astring1 = astring[j];
                JsonArray jsonarray1 = new JsonArray();
                String[] astring2 = astring1;
                int k = astring1.length;

                for (int l = 0; l < k; ++l) {
                    String s = astring2[l];

                    jsonarray1.add(s);
                }

                jsonarray.add(jsonarray1);
            }

            jsonobject.add("requirements", jsonarray);
            return jsonobject;
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
            String[][] astring = this.requirements;
            int i = astring.length;

            for (int j = 0; j < i; ++j) {
                String[] astring1 = astring[j];

                buf.writeVarInt(astring1.length);
                String[] astring2 = astring1;
                int k = astring1.length;

                for (int l = 0; l < k; ++l) {
                    String s = astring2[l];

                    buf.writeUtf(s);
                }
            }

        }

        public String toString() {
            return "Task Advancement{parentId=" + this.parentId + ", display=" + this.display + ", rewards=" + this.rewards + ", criteria=" + this.criteria + ", requirements=" + Arrays.deepToString(this.requirements) + "}";
        }

        public static Advancement.Builder fromJson(JsonObject obj, DeserializationContext predicateDeserializer) {
            ResourceLocation minecraftkey = obj.has("parent") ? new ResourceLocation(GsonHelper.getAsString(obj, "parent")) : null;
            DisplayInfo advancementdisplay = obj.has("display") ? DisplayInfo.fromJson(GsonHelper.getAsJsonObject(obj, "display")) : null;
            AdvancementRewards advancementrewards = obj.has("rewards") ? AdvancementRewards.deserialize(GsonHelper.getAsJsonObject(obj, "rewards")) : AdvancementRewards.EMPTY;
            Map<String, Criterion> map = Criterion.criteriaFromJson(GsonHelper.getAsJsonObject(obj, "criteria"), predicateDeserializer);

            if (map.isEmpty()) {
                throw new JsonSyntaxException("Advancement criteria cannot be empty");
            } else {
                JsonArray jsonarray = GsonHelper.getAsJsonArray(obj, "requirements", new JsonArray());
                String[][] astring = new String[jsonarray.size()][];

                int i;
                int j;

                for (i = 0; i < jsonarray.size(); ++i) {
                    JsonArray jsonarray1 = GsonHelper.convertToJsonArray(jsonarray.get(i), "requirements[" + i + "]");

                    astring[i] = new String[jsonarray1.size()];

                    for (j = 0; j < jsonarray1.size(); ++j) {
                        astring[i][j] = GsonHelper.convertToString(jsonarray1.get(j), "requirements[" + i + "][" + j + "]");
                    }
                }

                if (astring.length == 0) {
                    astring = new String[map.size()][];
                    i = 0;

                    String s;

                    for (Iterator iterator = map.keySet().iterator(); iterator.hasNext(); astring[i++] = new String[]{s}) {
                        s = (String) iterator.next();
                    }
                }

                String[][] astring1 = astring;
                int k = astring.length;

                int l;

                for (j = 0; j < k; ++j) {
                    String[] astring2 = astring1[j];

                    if (astring2.length == 0 && map.isEmpty()) {
                        throw new JsonSyntaxException("Requirement entry cannot be empty");
                    }

                    String[] astring3 = astring2;

                    l = astring2.length;

                    for (int i1 = 0; i1 < l; ++i1) {
                        String s1 = astring3[i1];

                        if (!map.containsKey(s1)) {
                            throw new JsonSyntaxException("Unknown required criterion '" + s1 + "'");
                        }
                    }
                }

                Iterator iterator1 = map.keySet().iterator();

                while (iterator1.hasNext()) {
                    String s2 = (String) iterator1.next();
                    boolean flag = false;
                    String[][] astring4 = astring;
                    int j1 = astring.length;

                    l = 0;

                    while (true) {
                        if (l < j1) {
                            String[] astring5 = astring4[l];

                            if (!ArrayUtils.contains(astring5, s2)) {
                                ++l;
                                continue;
                            }

                            flag = true;
                        }

                        if (!flag) {
                            throw new JsonSyntaxException("Criterion '" + s2 + "' isn't a requirement for completion. This isn't supported behaviour, all criteria must be required.");
                        }
                        break;
                    }
                }

                return new Advancement.Builder(minecraftkey, advancementdisplay, advancementrewards, map, astring);
            }
        }

        public static Advancement.Builder fromNetwork(FriendlyByteBuf buf) {
            ResourceLocation minecraftkey = buf.readBoolean() ? buf.readResourceLocation() : null;
            DisplayInfo advancementdisplay = buf.readBoolean() ? DisplayInfo.fromNetwork(buf) : null;
            Map<String, Criterion> map = Criterion.criteriaFromNetwork(buf);
            String[][] astring = new String[buf.readVarInt()][];

            for (int i = 0; i < astring.length; ++i) {
                astring[i] = new String[buf.readVarInt()];

                for (int j = 0; j < astring[i].length; ++j) {
                    astring[i][j] = buf.readUtf();
                }
            }

            return new Advancement.Builder(minecraftkey, advancementdisplay, AdvancementRewards.EMPTY, map, astring);
        }

        public Map<String, Criterion> getCriteria() {
            return this.criteria;
        }
    }
}
