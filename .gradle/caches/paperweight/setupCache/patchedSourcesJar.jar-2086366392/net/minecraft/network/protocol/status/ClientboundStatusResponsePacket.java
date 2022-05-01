package net.minecraft.network.protocol.status;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LowerCaseEnumTypeAdapterFactory;

public class ClientboundStatusResponsePacket implements Packet<ClientStatusPacketListener> {
    private static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(ServerStatus.Version.class, new ServerStatus.Version.Serializer()).registerTypeAdapter(ServerStatus.Players.class, new ServerStatus.Players.Serializer()).registerTypeAdapter(ServerStatus.class, new ServerStatus.Serializer()).registerTypeHierarchyAdapter(Component.class, new Component.Serializer()).registerTypeHierarchyAdapter(Style.class, new Style.Serializer()).registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory())
        .registerTypeAdapter(io.papermc.paper.adventure.AdventureComponent.class, new io.papermc.paper.adventure.AdventureComponent.Serializer())
        .create();
    private final ServerStatus status;

    public ClientboundStatusResponsePacket(ServerStatus metadata) {
        this.status = metadata;
    }

    public ClientboundStatusResponsePacket(FriendlyByteBuf buf) {
        this.status = GsonHelper.fromJson(GSON, buf.readUtf(32767), ServerStatus.class);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(GSON.toJson(this.status));
    }

    @Override
    public void handle(ClientStatusPacketListener listener) {
        listener.handleStatusResponse(this);
    }

    public ServerStatus getStatus() {
        return this.status;
    }
}
