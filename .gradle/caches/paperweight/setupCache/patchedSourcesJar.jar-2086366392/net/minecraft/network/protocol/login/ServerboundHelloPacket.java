package net.minecraft.network.protocol.login;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundHelloPacket implements Packet<ServerLoginPacketListener> {
    private final GameProfile gameProfile;

    public ServerboundHelloPacket(GameProfile profile) {
        this.gameProfile = profile;
    }

    public ServerboundHelloPacket(FriendlyByteBuf buf) {
        this.gameProfile = new GameProfile((UUID)null, buf.readUtf(16));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.gameProfile.getName());
    }

    @Override
    public void handle(ServerLoginPacketListener listener) {
        listener.handleHello(this);
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }
}
