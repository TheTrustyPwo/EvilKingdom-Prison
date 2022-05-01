package net.minecraft.server.bossevents;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public class CustomBossEvent extends ServerBossEvent {
    private final ResourceLocation id;
    private final Set<UUID> players = Sets.newHashSet();
    private int value;
    private int max = 100;

    public CustomBossEvent(ResourceLocation id, Component displayName) {
        super(displayName, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
        this.id = id;
        this.setProgress(0.0F);
    }

    public ResourceLocation getTextId() {
        return this.id;
    }

    @Override
    public void addPlayer(ServerPlayer player) {
        super.addPlayer(player);
        this.players.add(player.getUUID());
    }

    public void addOfflinePlayer(UUID uuid) {
        this.players.add(uuid);
    }

    @Override
    public void removePlayer(ServerPlayer player) {
        super.removePlayer(player);
        this.players.remove(player.getUUID());
    }

    @Override
    public void removeAllPlayers() {
        super.removeAllPlayers();
        this.players.clear();
    }

    public int getValue() {
        return this.value;
    }

    public int getMax() {
        return this.max;
    }

    public void setValue(int value) {
        this.value = value;
        this.setProgress(Mth.clamp((float)value / (float)this.max, 0.0F, 1.0F));
    }

    public void setMax(int maxValue) {
        this.max = maxValue;
        this.setProgress(Mth.clamp((float)this.value / (float)maxValue, 0.0F, 1.0F));
    }

    public final Component getDisplayName() {
        return ComponentUtils.wrapInSquareBrackets(this.getName()).withStyle((style) -> {
            return style.withColor(this.getColor().getFormatting()).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(this.getTextId().toString()))).withInsertion(this.getTextId().toString());
        });
    }

    public boolean setPlayers(Collection<ServerPlayer> players) {
        Set<UUID> set = Sets.newHashSet();
        Set<ServerPlayer> set2 = Sets.newHashSet();

        for(UUID uUID : this.players) {
            boolean bl = false;

            for(ServerPlayer serverPlayer : players) {
                if (serverPlayer.getUUID().equals(uUID)) {
                    bl = true;
                    break;
                }
            }

            if (!bl) {
                set.add(uUID);
            }
        }

        for(ServerPlayer serverPlayer2 : players) {
            boolean bl2 = false;

            for(UUID uUID2 : this.players) {
                if (serverPlayer2.getUUID().equals(uUID2)) {
                    bl2 = true;
                    break;
                }
            }

            if (!bl2) {
                set2.add(serverPlayer2);
            }
        }

        for(UUID uUID3 : set) {
            for(ServerPlayer serverPlayer3 : this.getPlayers()) {
                if (serverPlayer3.getUUID().equals(uUID3)) {
                    this.removePlayer(serverPlayer3);
                    break;
                }
            }

            this.players.remove(uUID3);
        }

        for(ServerPlayer serverPlayer4 : set2) {
            this.addPlayer(serverPlayer4);
        }

        return !set.isEmpty() || !set2.isEmpty();
    }

    public CompoundTag save() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("Name", Component.Serializer.toJson(this.name));
        compoundTag.putBoolean("Visible", this.isVisible());
        compoundTag.putInt("Value", this.value);
        compoundTag.putInt("Max", this.max);
        compoundTag.putString("Color", this.getColor().getName());
        compoundTag.putString("Overlay", this.getOverlay().getName());
        compoundTag.putBoolean("DarkenScreen", this.shouldDarkenScreen());
        compoundTag.putBoolean("PlayBossMusic", this.shouldPlayBossMusic());
        compoundTag.putBoolean("CreateWorldFog", this.shouldCreateWorldFog());
        ListTag listTag = new ListTag();

        for(UUID uUID : this.players) {
            listTag.add(NbtUtils.createUUID(uUID));
        }

        compoundTag.put("Players", listTag);
        return compoundTag;
    }

    public static CustomBossEvent load(CompoundTag nbt, ResourceLocation id) {
        CustomBossEvent customBossEvent = new CustomBossEvent(id, Component.Serializer.fromJson(nbt.getString("Name")));
        customBossEvent.setVisible(nbt.getBoolean("Visible"));
        customBossEvent.setValue(nbt.getInt("Value"));
        customBossEvent.setMax(nbt.getInt("Max"));
        customBossEvent.setColor(BossEvent.BossBarColor.byName(nbt.getString("Color")));
        customBossEvent.setOverlay(BossEvent.BossBarOverlay.byName(nbt.getString("Overlay")));
        customBossEvent.setDarkenScreen(nbt.getBoolean("DarkenScreen"));
        customBossEvent.setPlayBossMusic(nbt.getBoolean("PlayBossMusic"));
        customBossEvent.setCreateWorldFog(nbt.getBoolean("CreateWorldFog"));
        ListTag listTag = nbt.getList("Players", 11);

        for(int i = 0; i < listTag.size(); ++i) {
            customBossEvent.addOfflinePlayer(NbtUtils.loadUUID(listTag.get(i)));
        }

        return customBossEvent;
    }

    public void onPlayerConnect(ServerPlayer player) {
        if (this.players.contains(player.getUUID())) {
            this.addPlayer(player);
        }

    }

    public void onPlayerDisconnect(ServerPlayer player) {
        super.removePlayer(player);
    }
}
