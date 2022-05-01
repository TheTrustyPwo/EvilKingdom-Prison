package net.minecraft.network.protocol.game;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ClientboundUpdateAdvancementsPacket implements Packet<ClientGamePacketListener> {
    private final boolean reset;
    private final Map<ResourceLocation, Advancement.Builder> added;
    private final Set<ResourceLocation> removed;
    private final Map<ResourceLocation, AdvancementProgress> progress;

    public ClientboundUpdateAdvancementsPacket(boolean clearCurrent, Collection<Advancement> toEarn, Set<ResourceLocation> toRemove, Map<ResourceLocation, AdvancementProgress> toSetProgress) {
        this.reset = clearCurrent;
        Builder<ResourceLocation, Advancement.Builder> builder = ImmutableMap.builder();

        for(Advancement advancement : toEarn) {
            builder.put(advancement.getId(), advancement.deconstruct());
        }

        this.added = builder.build();
        this.removed = ImmutableSet.copyOf(toRemove);
        this.progress = ImmutableMap.copyOf(toSetProgress);
    }

    public ClientboundUpdateAdvancementsPacket(FriendlyByteBuf buf) {
        this.reset = buf.readBoolean();
        this.added = buf.readMap(FriendlyByteBuf::readResourceLocation, Advancement.Builder::fromNetwork);
        this.removed = buf.readCollection(Sets::newLinkedHashSetWithExpectedSize, FriendlyByteBuf::readResourceLocation);
        this.progress = buf.readMap(FriendlyByteBuf::readResourceLocation, AdvancementProgress::fromNetwork);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.reset);
        buf.writeMap(this.added, FriendlyByteBuf::writeResourceLocation, (bufx, task) -> {
            task.serializeToNetwork(bufx);
        });
        buf.writeCollection(this.removed, FriendlyByteBuf::writeResourceLocation);
        buf.writeMap(this.progress, FriendlyByteBuf::writeResourceLocation, (bufx, progress) -> {
            progress.serializeToNetwork(bufx);
        });
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleUpdateAdvancementsPacket(this);
    }

    public Map<ResourceLocation, Advancement.Builder> getAdded() {
        return this.added;
    }

    public Set<ResourceLocation> getRemoved() {
        return this.removed;
    }

    public Map<ResourceLocation, AdvancementProgress> getProgress() {
        return this.progress;
    }

    public boolean shouldReset() {
        return this.reset;
    }
}
