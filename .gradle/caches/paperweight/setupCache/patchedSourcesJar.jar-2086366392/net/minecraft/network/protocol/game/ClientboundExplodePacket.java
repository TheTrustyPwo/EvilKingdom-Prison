package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ClientboundExplodePacket implements Packet<ClientGamePacketListener> {
    private final double x;
    private final double y;
    private final double z;
    private final float power;
    private final List<BlockPos> toBlow;
    private final float knockbackX;
    private final float knockbackY;
    private final float knockbackZ;

    public ClientboundExplodePacket(double x, double y, double z, float radius, List<BlockPos> affectedBlocks, @Nullable Vec3 playerVelocity) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.power = radius;
        this.toBlow = Lists.newArrayList(affectedBlocks);
        if (playerVelocity != null) {
            this.knockbackX = (float)playerVelocity.x;
            this.knockbackY = (float)playerVelocity.y;
            this.knockbackZ = (float)playerVelocity.z;
        } else {
            this.knockbackX = 0.0F;
            this.knockbackY = 0.0F;
            this.knockbackZ = 0.0F;
        }

    }

    public ClientboundExplodePacket(FriendlyByteBuf buf) {
        this.x = (double)buf.readFloat();
        this.y = (double)buf.readFloat();
        this.z = (double)buf.readFloat();
        this.power = buf.readFloat();
        int i = Mth.floor(this.x);
        int j = Mth.floor(this.y);
        int k = Mth.floor(this.z);
        this.toBlow = buf.readList((friendlyByteBuf) -> {
            int l = friendlyByteBuf.readByte() + i;
            int m = friendlyByteBuf.readByte() + j;
            int n = friendlyByteBuf.readByte() + k;
            return new BlockPos(l, m, n);
        });
        this.knockbackX = buf.readFloat();
        this.knockbackY = buf.readFloat();
        this.knockbackZ = buf.readFloat();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat((float)this.x);
        buf.writeFloat((float)this.y);
        buf.writeFloat((float)this.z);
        buf.writeFloat(this.power);
        int i = Mth.floor(this.x);
        int j = Mth.floor(this.y);
        int k = Mth.floor(this.z);
        buf.writeCollection(this.toBlow, (bufx, pos) -> {
            int l = pos.getX() - i;
            int m = pos.getY() - j;
            int n = pos.getZ() - k;
            bufx.writeByte(l);
            bufx.writeByte(m);
            bufx.writeByte(n);
        });
        buf.writeFloat(this.knockbackX);
        buf.writeFloat(this.knockbackY);
        buf.writeFloat(this.knockbackZ);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleExplosion(this);
    }

    public float getKnockbackX() {
        return this.knockbackX;
    }

    public float getKnockbackY() {
        return this.knockbackY;
    }

    public float getKnockbackZ() {
        return this.knockbackZ;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public float getPower() {
        return this.power;
    }

    public List<BlockPos> getToBlow() {
        return this.toBlow;
    }
}
