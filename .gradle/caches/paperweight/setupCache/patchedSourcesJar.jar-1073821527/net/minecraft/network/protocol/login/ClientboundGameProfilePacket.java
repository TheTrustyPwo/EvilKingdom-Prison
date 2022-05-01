package net.minecraft.network.protocol.login;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.SerializableUUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundGameProfilePacket implements Packet<ClientLoginPacketListener> {
    private final GameProfile gameProfile;

    public ClientboundGameProfilePacket(GameProfile profile) {
        this.gameProfile = profile;
    }

    public ClientboundGameProfilePacket(FriendlyByteBuf buf) {
        int[] is = new int[4];

        for(int i = 0; i < is.length; ++i) {
            is[i] = buf.readInt();
        }

        UUID uUID = SerializableUUID.uuidFromIntArray(is);
        String string = buf.readUtf(16);
        this.gameProfile = new GameProfile(uUID, string);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        for(int i : SerializableUUID.uuidToIntArray(this.gameProfile.getId())) {
            buf.writeInt(i);
        }

        buf.writeUtf(this.gameProfile.getName());
    }

    @Override
    public void handle(ClientLoginPacketListener listener) {
        listener.handleGameProfile(this);
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }
}
