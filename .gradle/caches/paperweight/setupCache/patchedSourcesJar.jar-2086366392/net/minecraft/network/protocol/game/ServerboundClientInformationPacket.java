package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;

public record ServerboundClientInformationPacket(String language, int viewDistance, ChatVisiblity chatVisibility, boolean chatColors, int modelCustomisation, HumanoidArm mainHand, boolean textFilteringEnabled, boolean allowsListing) implements Packet<ServerGamePacketListener> {
    public final String language;
    public final int viewDistance;
    public static final int MAX_LANGUAGE_LENGTH = 16;

    public ServerboundClientInformationPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(16), buf.readByte(), buf.readEnum(ChatVisiblity.class), buf.readBoolean(), buf.readUnsignedByte(), buf.readEnum(HumanoidArm.class), buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.language);
        buf.writeByte(this.viewDistance);
        buf.writeEnum(this.chatVisibility);
        buf.writeBoolean(this.chatColors);
        buf.writeByte(this.modelCustomisation);
        buf.writeEnum(this.mainHand);
        buf.writeBoolean(this.textFilteringEnabled);
        buf.writeBoolean(this.allowsListing);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleClientInformation(this);
    }
}
