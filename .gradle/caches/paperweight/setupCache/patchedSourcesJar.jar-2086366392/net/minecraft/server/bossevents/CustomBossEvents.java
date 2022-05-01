package net.minecraft.server.bossevents;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CustomBossEvents {
    private final Map<ResourceLocation, CustomBossEvent> events = Maps.newHashMap();

    @Nullable
    public CustomBossEvent get(ResourceLocation id) {
        return this.events.get(id);
    }

    public CustomBossEvent create(ResourceLocation id, Component displayName) {
        CustomBossEvent customBossEvent = new CustomBossEvent(id, displayName);
        this.events.put(id, customBossEvent);
        return customBossEvent;
    }

    public void remove(CustomBossEvent bossBar) {
        this.events.remove(bossBar.getTextId());
    }

    public Collection<ResourceLocation> getIds() {
        return this.events.keySet();
    }

    public Collection<CustomBossEvent> getEvents() {
        return this.events.values();
    }

    public CompoundTag save() {
        CompoundTag compoundTag = new CompoundTag();

        for(CustomBossEvent customBossEvent : this.events.values()) {
            compoundTag.put(customBossEvent.getTextId().toString(), customBossEvent.save());
        }

        return compoundTag;
    }

    public void load(CompoundTag nbt) {
        for(String string : nbt.getAllKeys()) {
            ResourceLocation resourceLocation = new ResourceLocation(string);
            this.events.put(resourceLocation, CustomBossEvent.load(nbt.getCompound(string), resourceLocation));
        }

    }

    public void onPlayerConnect(ServerPlayer player) {
        for(CustomBossEvent customBossEvent : this.events.values()) {
            customBossEvent.onPlayerConnect(player);
        }

    }

    public void onPlayerDisconnect(ServerPlayer player) {
        for(CustomBossEvent customBossEvent : this.events.values()) {
            customBossEvent.onPlayerDisconnect(player);
        }

    }
}
