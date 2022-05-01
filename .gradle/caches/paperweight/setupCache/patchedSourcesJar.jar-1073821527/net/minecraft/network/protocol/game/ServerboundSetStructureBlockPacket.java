package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;

public class ServerboundSetStructureBlockPacket implements Packet<ServerGamePacketListener> {
    private static final int FLAG_IGNORE_ENTITIES = 1;
    private static final int FLAG_SHOW_AIR = 2;
    private static final int FLAG_SHOW_BOUNDING_BOX = 4;
    private final BlockPos pos;
    private final StructureBlockEntity.UpdateType updateType;
    private final StructureMode mode;
    private final String name;
    private final BlockPos offset;
    private final Vec3i size;
    private final Mirror mirror;
    private final Rotation rotation;
    private final String data;
    private final boolean ignoreEntities;
    private final boolean showAir;
    private final boolean showBoundingBox;
    private final float integrity;
    private final long seed;

    public ServerboundSetStructureBlockPacket(BlockPos pos, StructureBlockEntity.UpdateType action, StructureMode mode, String structureName, BlockPos offset, Vec3i size, Mirror mirror, Rotation rotation, String metadata, boolean ignoreEntities, boolean showAir, boolean showBoundingBox, float integrity, long seed) {
        this.pos = pos;
        this.updateType = action;
        this.mode = mode;
        this.name = structureName;
        this.offset = offset;
        this.size = size;
        this.mirror = mirror;
        this.rotation = rotation;
        this.data = metadata;
        this.ignoreEntities = ignoreEntities;
        this.showAir = showAir;
        this.showBoundingBox = showBoundingBox;
        this.integrity = integrity;
        this.seed = seed;
    }

    public ServerboundSetStructureBlockPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.updateType = buf.readEnum(StructureBlockEntity.UpdateType.class);
        this.mode = buf.readEnum(StructureMode.class);
        this.name = buf.readUtf();
        int i = 48;
        this.offset = new BlockPos(Mth.clamp((int)buf.readByte(), (int)-48, (int)48), Mth.clamp((int)buf.readByte(), (int)-48, (int)48), Mth.clamp((int)buf.readByte(), (int)-48, (int)48));
        int j = 48;
        this.size = new Vec3i(Mth.clamp((int)buf.readByte(), (int)0, (int)48), Mth.clamp((int)buf.readByte(), (int)0, (int)48), Mth.clamp((int)buf.readByte(), (int)0, (int)48));
        this.mirror = buf.readEnum(Mirror.class);
        this.rotation = buf.readEnum(Rotation.class);
        this.data = buf.readUtf(128);
        this.integrity = Mth.clamp(buf.readFloat(), 0.0F, 1.0F);
        this.seed = buf.readVarLong();
        int k = buf.readByte();
        this.ignoreEntities = (k & 1) != 0;
        this.showAir = (k & 2) != 0;
        this.showBoundingBox = (k & 4) != 0;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeEnum(this.updateType);
        buf.writeEnum(this.mode);
        buf.writeUtf(this.name);
        buf.writeByte(this.offset.getX());
        buf.writeByte(this.offset.getY());
        buf.writeByte(this.offset.getZ());
        buf.writeByte(this.size.getX());
        buf.writeByte(this.size.getY());
        buf.writeByte(this.size.getZ());
        buf.writeEnum(this.mirror);
        buf.writeEnum(this.rotation);
        buf.writeUtf(this.data);
        buf.writeFloat(this.integrity);
        buf.writeVarLong(this.seed);
        int i = 0;
        if (this.ignoreEntities) {
            i |= 1;
        }

        if (this.showAir) {
            i |= 2;
        }

        if (this.showBoundingBox) {
            i |= 4;
        }

        buf.writeByte(i);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleSetStructureBlock(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public StructureBlockEntity.UpdateType getUpdateType() {
        return this.updateType;
    }

    public StructureMode getMode() {
        return this.mode;
    }

    public String getName() {
        return this.name;
    }

    public BlockPos getOffset() {
        return this.offset;
    }

    public Vec3i getSize() {
        return this.size;
    }

    public Mirror getMirror() {
        return this.mirror;
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public String getData() {
        return this.data;
    }

    public boolean isIgnoreEntities() {
        return this.ignoreEntities;
    }

    public boolean isShowAir() {
        return this.showAir;
    }

    public boolean isShowBoundingBox() {
        return this.showBoundingBox;
    }

    public float getIntegrity() {
        return this.integrity;
    }

    public long getSeed() {
        return this.seed;
    }
}
