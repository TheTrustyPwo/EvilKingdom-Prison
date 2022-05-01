package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundResourcePackPacket implements Packet<ClientGamePacketListener> {
    public static final int MAX_HASH_LENGTH = 40;
    private final String url;
    private final String hash;
    private final boolean required;
    @Nullable
    private final Component prompt;

    public ClientboundResourcePackPacket(String url, String hash, boolean required, @Nullable Component prompt) {
        if (hash.length() > 40) {
            throw new IllegalArgumentException("Hash is too long (max 40, was " + hash.length() + ")");
        } else {
            this.url = url;
            this.hash = hash;
            this.required = required;
            this.prompt = prompt;
        }
    }

    public ClientboundResourcePackPacket(FriendlyByteBuf buf) {
        this.url = buf.readUtf();
        this.hash = buf.readUtf(40);
        this.required = buf.readBoolean();
        if (buf.readBoolean()) {
            this.prompt = buf.readComponent();
        } else {
            this.prompt = null;
        }

    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.url);
        buf.writeUtf(this.hash);
        buf.writeBoolean(this.required);
        if (this.prompt != null) {
            buf.writeBoolean(true);
            buf.writeComponent(this.prompt);
        } else {
            buf.writeBoolean(false);
        }

    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleResourcePack(this);
    }

    public String getUrl() {
        return this.url;
    }

    public String getHash() {
        return this.hash;
    }

    public boolean isRequired() {
        return this.required;
    }

    @Nullable
    public Component getPrompt() {
        return this.prompt;
    }
}
