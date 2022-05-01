package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ClientboundSelectAdvancementsTabPacket implements Packet<ClientGamePacketListener> {
    @Nullable
    private final ResourceLocation tab;

    public ClientboundSelectAdvancementsTabPacket(@Nullable ResourceLocation tabId) {
        this.tab = tabId;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSelectAdvancementsTab(this);
    }

    public ClientboundSelectAdvancementsTabPacket(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            this.tab = buf.readResourceLocation();
        } else {
            this.tab = null;
        }

    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.tab != null);
        if (this.tab != null) {
            buf.writeResourceLocation(this.tab);
        }

    }

    @Nullable
    public ResourceLocation getTab() {
        return this.tab;
    }
}
