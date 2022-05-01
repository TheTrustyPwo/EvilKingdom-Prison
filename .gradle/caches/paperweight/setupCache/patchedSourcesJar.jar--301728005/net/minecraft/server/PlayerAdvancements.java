package net.minecraft.server;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

public class PlayerAdvancements {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int VISIBILITY_DEPTH = 2;
    private static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(AdvancementProgress.class, new AdvancementProgress.Serializer()).registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).setPrettyPrinting().create();
    private static final TypeToken<Map<ResourceLocation, AdvancementProgress>> TYPE_TOKEN = new TypeToken<Map<ResourceLocation, AdvancementProgress>>() {
    };
    private final DataFixer dataFixer;
    private final PlayerList playerList;
    private final File file;
    public final Map<Advancement, AdvancementProgress> advancements = Maps.newLinkedHashMap();
    private final Set<Advancement> visible = Sets.newLinkedHashSet();
    private final Set<Advancement> visibilityChanged = Sets.newLinkedHashSet();
    private final Set<Advancement> progressChanged = Sets.newLinkedHashSet();
    private ServerPlayer player;
    @Nullable
    private Advancement lastSelectedTab;
    private boolean isFirstPacket = true;

    public final Map<SimpleCriterionTrigger, Set<CriterionTrigger.Listener>> criterionData = Maps.newIdentityHashMap(); // Paper - fix advancement data player leakage

    public PlayerAdvancements(DataFixer dataFixer, PlayerList playerManager, ServerAdvancementManager advancementLoader, File advancementFile, ServerPlayer owner) {
        this.dataFixer = dataFixer;
        this.playerList = playerManager;
        this.file = advancementFile;
        this.player = owner;
        this.load(advancementLoader);
    }

    public void setPlayer(ServerPlayer owner) {
        this.player = owner;
    }

    public void stopListening() {
        Iterator iterator = CriteriaTriggers.all().iterator();

        while (iterator.hasNext()) {
            CriterionTrigger<?> criteriontrigger = (CriterionTrigger) iterator.next();

            criteriontrigger.removePlayerListeners(this);
        }

    }

    public void reload(ServerAdvancementManager advancementLoader) {
        this.stopListening();
        this.advancements.clear();
        this.visible.clear();
        this.visibilityChanged.clear();
        this.progressChanged.clear();
        this.isFirstPacket = true;
        this.lastSelectedTab = null;
        this.load(advancementLoader);
    }

    private void registerListeners(ServerAdvancementManager advancementLoader) {
        Iterator iterator = advancementLoader.getAllAdvancements().iterator();

        while (iterator.hasNext()) {
            Advancement advancement = (Advancement) iterator.next();

            this.registerListeners(advancement);
        }

    }

    private void ensureAllVisible() {
        List<Advancement> list = Lists.newArrayList();
        Iterator iterator = this.advancements.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<Advancement, AdvancementProgress> entry = (Entry) iterator.next();

            if (((AdvancementProgress) entry.getValue()).isDone()) {
                list.add((Advancement) entry.getKey());
                this.progressChanged.add((Advancement) entry.getKey());
            }
        }

        iterator = list.iterator();

        while (iterator.hasNext()) {
            Advancement advancement = (Advancement) iterator.next();

            this.ensureVisibility(advancement);
        }

    }

    private void checkForAutomaticTriggers(ServerAdvancementManager advancementLoader) {
        Iterator iterator = advancementLoader.getAllAdvancements().iterator();

        while (iterator.hasNext()) {
            Advancement advancement = (Advancement) iterator.next();

            if (advancement.getCriteria().isEmpty()) {
                this.award(advancement, "");
                advancement.getRewards().grant(this.player);
            }
        }

    }

    private void load(ServerAdvancementManager advancementLoader) {
        if (this.file.isFile()) {
            try {
                JsonReader jsonreader = new JsonReader(new StringReader(Files.toString(this.file, StandardCharsets.UTF_8)));

                try {
                    jsonreader.setLenient(false);
                    Dynamic<JsonElement> dynamic = new Dynamic(JsonOps.INSTANCE, Streams.parse(jsonreader));

                    if (!dynamic.get("DataVersion").asNumber().result().isPresent()) {
                        dynamic = dynamic.set("DataVersion", dynamic.createInt(1343));
                    }

                    dynamic = this.dataFixer.update(DataFixTypes.ADVANCEMENTS.getType(), dynamic, dynamic.get("DataVersion").asInt(0), SharedConstants.getCurrentVersion().getWorldVersion());
                    dynamic = dynamic.remove("DataVersion");
                    Map<ResourceLocation, AdvancementProgress> map = (Map) PlayerAdvancements.GSON.getAdapter(PlayerAdvancements.TYPE_TOKEN).fromJsonTree((JsonElement) dynamic.getValue());

                    if (map == null) {
                        throw new JsonParseException("Found null for advancements");
                    }

                    Stream<Entry<ResourceLocation, AdvancementProgress>> stream = map.entrySet().stream().sorted(Comparator.comparing(Entry::getValue));
                    Iterator iterator = ((List) stream.collect(Collectors.toList())).iterator();

                    while (iterator.hasNext()) {
                        Entry<ResourceLocation, AdvancementProgress> entry = (Entry) iterator.next();
                        Advancement advancement = advancementLoader.getAdvancement((ResourceLocation) entry.getKey());

                        if (advancement == null) {
                            // CraftBukkit start
                            if (entry.getKey().getNamespace().equals("minecraft")) {
                                PlayerAdvancements.LOGGER.warn("Ignored advancement '{}' in progress file {} - it doesn't exist anymore?", entry.getKey(), this.file);
                            }
                            // CraftBukkit end
                        } else {
                            this.startProgress(advancement, (AdvancementProgress) entry.getValue());
                        }
                    }
                } catch (Throwable throwable) {
                    try {
                        jsonreader.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }

                    throw throwable;
                }

                jsonreader.close();
            } catch (JsonParseException jsonparseexception) {
                PlayerAdvancements.LOGGER.error("Couldn't parse player advancements in {}", this.file, jsonparseexception);
            } catch (IOException ioexception) {
                PlayerAdvancements.LOGGER.error("Couldn't access player advancements in {}", this.file, ioexception);
            }
        }

        this.checkForAutomaticTriggers(advancementLoader);
        this.ensureAllVisible();
        this.registerListeners(advancementLoader);
    }

    public void save() {
        if (org.spigotmc.SpigotConfig.disableAdvancementSaving) return; // Spigot
        Map<ResourceLocation, AdvancementProgress> map = Maps.newHashMap();
        Iterator iterator = this.advancements.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<Advancement, AdvancementProgress> entry = (Entry) iterator.next();
            AdvancementProgress advancementprogress = (AdvancementProgress) entry.getValue();

            if (advancementprogress.hasProgress()) {
                map.put(((Advancement) entry.getKey()).getId(), advancementprogress);
            }
        }

        if (this.file.getParentFile() != null) {
            this.file.getParentFile().mkdirs();
        }

        JsonElement jsonelement = PlayerAdvancements.GSON.toJsonTree(map);

        jsonelement.getAsJsonObject().addProperty("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());

        try {
            FileOutputStream fileoutputstream = new FileOutputStream(this.file);

            try {
                OutputStreamWriter outputstreamwriter = new OutputStreamWriter(fileoutputstream, Charsets.UTF_8.newEncoder());

                try {
                    PlayerAdvancements.GSON.toJson(jsonelement, outputstreamwriter);
                } catch (Throwable throwable) {
                    try {
                        outputstreamwriter.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }

                    throw throwable;
                }

                outputstreamwriter.close();
            } catch (Throwable throwable2) {
                try {
                    fileoutputstream.close();
                } catch (Throwable throwable3) {
                    throwable2.addSuppressed(throwable3);
                }

                throw throwable2;
            }

            fileoutputstream.close();
        } catch (IOException ioexception) {
            PlayerAdvancements.LOGGER.error("Couldn't save player advancements to {}", this.file, ioexception);
        }

    }

    public boolean award(Advancement advancement, String criterionName) {
        boolean flag = false;
        AdvancementProgress advancementprogress = this.getOrStartProgress(advancement);
        boolean flag1 = advancementprogress.isDone();

        if (advancementprogress.grantProgress(criterionName)) {
            // Paper start
            if (!new com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent(this.player.getBukkitEntity(), advancement.bukkit, criterionName).callEvent()) {
                advancementprogress.revokeProgress(criterionName);
                return false;
            }
            // Paper end
            this.unregisterListeners(advancement);
            this.progressChanged.add(advancement);
            flag = true;
            if (!flag1 && advancementprogress.isDone()) {
                // Paper start - Add Adventure message to PlayerAdvancementDoneEvent
                boolean announceToChat = advancement.getDisplay() != null && advancement.getDisplay().shouldAnnounceChat();
                net.kyori.adventure.text.Component message = announceToChat ? io.papermc.paper.adventure.PaperAdventure.asAdventure(new TranslatableComponent("chat.type.advancement." + advancement.getDisplay().getFrame().getName(), this.player.getDisplayName(), advancement.getChatComponent())) : null;
                org.bukkit.event.player.PlayerAdvancementDoneEvent event = new org.bukkit.event.player.PlayerAdvancementDoneEvent(this.player.getBukkitEntity(), advancement.bukkit, message);
                this.player.level.getCraftServer().getPluginManager().callEvent(event);
                message = event.message();
                // Paper end
                advancement.getRewards().grant(this.player);
                // Paper start - Add Adventure message to PlayerAdvancementDoneEvent
                if (message != null && this.player.level.getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)) {
                    this.playerList.broadcastMessage(io.papermc.paper.adventure.PaperAdventure.asVanilla(message), ChatType.SYSTEM, Util.NIL_UUID);
                    // Paper end
                }
            }
        }

        if (advancementprogress.isDone()) {
            this.ensureVisibility(advancement);
        }

        return flag;
    }

    public boolean revoke(Advancement advancement, String criterionName) {
        boolean flag = false;
        AdvancementProgress advancementprogress = this.getOrStartProgress(advancement);

        if (advancementprogress.revokeProgress(criterionName)) {
            this.registerListeners(advancement);
            this.progressChanged.add(advancement);
            flag = true;
        }

        if (!advancementprogress.hasProgress()) {
            this.ensureVisibility(advancement);
        }

        return flag;
    }

    private void registerListeners(Advancement advancement) {
        AdvancementProgress advancementprogress = this.getOrStartProgress(advancement);

        if (!advancementprogress.isDone()) {
            Iterator iterator = advancement.getCriteria().entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<String, Criterion> entry = (Entry) iterator.next();
                CriterionProgress criterionprogress = advancementprogress.getCriterion((String) entry.getKey());

                if (criterionprogress != null && !criterionprogress.isDone()) {
                    CriterionTriggerInstance criterioninstance = ((Criterion) entry.getValue()).getTrigger();

                    if (criterioninstance != null) {
                        CriterionTrigger<CriterionTriggerInstance> criteriontrigger = CriteriaTriggers.getCriterion(criterioninstance.getCriterion());

                        if (criteriontrigger != null) {
                            criteriontrigger.addPlayerListener(this, new CriterionTrigger.Listener<>(criterioninstance, advancement, (String) entry.getKey()));
                        }
                    }
                }
            }

        }
    }

    private void unregisterListeners(Advancement advancement) {
        AdvancementProgress advancementprogress = this.getOrStartProgress(advancement);
        Iterator iterator = advancement.getCriteria().entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<String, Criterion> entry = (Entry) iterator.next();
            CriterionProgress criterionprogress = advancementprogress.getCriterion((String) entry.getKey());

            if (criterionprogress != null && (criterionprogress.isDone() || advancementprogress.isDone())) {
                CriterionTriggerInstance criterioninstance = ((Criterion) entry.getValue()).getTrigger();

                if (criterioninstance != null) {
                    CriterionTrigger<CriterionTriggerInstance> criteriontrigger = CriteriaTriggers.getCriterion(criterioninstance.getCriterion());

                    if (criteriontrigger != null) {
                        criteriontrigger.removePlayerListener(this, new CriterionTrigger.Listener<>(criterioninstance, advancement, (String) entry.getKey()));
                    }
                }
            }
        }

    }

    public void flushDirty(ServerPlayer player) {
        if (this.isFirstPacket || !this.visibilityChanged.isEmpty() || !this.progressChanged.isEmpty()) {
            Map<ResourceLocation, AdvancementProgress> map = Maps.newHashMap();
            Set<Advancement> set = Sets.newLinkedHashSet();
            Set<ResourceLocation> set1 = Sets.newLinkedHashSet();
            Iterator iterator = this.progressChanged.iterator();

            Advancement advancement;

            while (iterator.hasNext()) {
                advancement = (Advancement) iterator.next();
                if (this.visible.contains(advancement)) {
                    map.put(advancement.getId(), (AdvancementProgress) this.advancements.get(advancement));
                }
            }

            iterator = this.visibilityChanged.iterator();

            while (iterator.hasNext()) {
                advancement = (Advancement) iterator.next();
                if (this.visible.contains(advancement)) {
                    set.add(advancement);
                } else {
                    set1.add(advancement.getId());
                }
            }

            if (this.isFirstPacket || !map.isEmpty() || !set.isEmpty() || !set1.isEmpty()) {
                player.connection.send(new ClientboundUpdateAdvancementsPacket(this.isFirstPacket, set, set1, map));
                this.visibilityChanged.clear();
                this.progressChanged.clear();
            }
        }

        this.isFirstPacket = false;
    }

    public void setSelectedTab(@Nullable Advancement advancement) {
        Advancement advancement1 = this.lastSelectedTab;

        if (advancement != null && advancement.getParent() == null && advancement.getDisplay() != null) {
            this.lastSelectedTab = advancement;
        } else {
            this.lastSelectedTab = null;
        }

        if (advancement1 != this.lastSelectedTab) {
            this.player.connection.send(new ClientboundSelectAdvancementsTabPacket(this.lastSelectedTab == null ? null : this.lastSelectedTab.getId()));
        }

    }

    public AdvancementProgress getOrStartProgress(Advancement advancement) {
        AdvancementProgress advancementprogress = (AdvancementProgress) this.advancements.get(advancement);

        if (advancementprogress == null) {
            advancementprogress = new AdvancementProgress();
            this.startProgress(advancement, advancementprogress);
        }

        return advancementprogress;
    }

    private void startProgress(Advancement advancement, AdvancementProgress progress) {
        progress.update(advancement.getCriteria(), advancement.getRequirements());
        this.advancements.put(advancement, progress);
    }

    private void ensureVisibility(Advancement advancement) {
        // Paper start
        ensureVisibility(advancement, IterationEntryPoint.ROOT);
    }
    private enum IterationEntryPoint {
        ROOT,
        ITERATOR,
        PARENT_OF_ITERATOR
    }
    private void ensureVisibility(Advancement advancement, IterationEntryPoint entryPoint) {
        // Paper end
        boolean flag = this.shouldBeVisible(advancement);
        boolean flag1 = this.visible.contains(advancement);

        if (flag && !flag1) {
            this.visible.add(advancement);
            this.visibilityChanged.add(advancement);
            if (this.advancements.containsKey(advancement)) {
                this.progressChanged.add(advancement);
            }
        } else if (!flag && flag1) {
            this.visible.remove(advancement);
            this.visibilityChanged.add(advancement);
        }

        if (flag != flag1 && advancement.getParent() != null) {
            // Paper start - If we're not coming from an iterator consider this to be a root entry, otherwise
            // market that we're entering from the parent of an iterator.
            this.ensureVisibility(advancement.getParent(), entryPoint == IterationEntryPoint.ITERATOR ? IterationEntryPoint.PARENT_OF_ITERATOR : IterationEntryPoint.ROOT);
        }

        // If this is true, we've went through a child iteration, entered the parent, processed the parent
        // and are about to reprocess the children. Stop processing here to prevent O(N^2) processing.
        if (entryPoint == IterationEntryPoint.PARENT_OF_ITERATOR) {
            return;
        } // Paper end

        Iterator iterator = advancement.getChildren().iterator();

        while (iterator.hasNext()) {
            Advancement advancement1 = (Advancement) iterator.next();

            this.ensureVisibility(advancement1, IterationEntryPoint.ITERATOR); // Paper - Mark this call as being from iteration
        }

    }

    private boolean shouldBeVisible(Advancement advancement) {
        for (int i = 0; advancement != null && i <= 2; ++i) {
            if (i == 0 && this.hasCompletedChildrenOrSelf(advancement)) {
                return true;
            }

            if (advancement.getDisplay() == null) {
                return false;
            }

            AdvancementProgress advancementprogress = this.getOrStartProgress(advancement);

            if (advancementprogress.isDone()) {
                return true;
            }

            if (advancement.getDisplay().isHidden()) {
                return false;
            }

            advancement = advancement.getParent();
        }

        return false;
    }

    private boolean hasCompletedChildrenOrSelf(Advancement advancement) {
        AdvancementProgress advancementprogress = this.getOrStartProgress(advancement);

        if (advancementprogress.isDone()) {
            return true;
        } else {
            Iterator iterator = advancement.getChildren().iterator();

            Advancement advancement1;

            do {
                if (!iterator.hasNext()) {
                    return false;
                }

                advancement1 = (Advancement) iterator.next();
            } while (!this.hasCompletedChildrenOrSelf(advancement1));

            return true;
        }
    }
}
