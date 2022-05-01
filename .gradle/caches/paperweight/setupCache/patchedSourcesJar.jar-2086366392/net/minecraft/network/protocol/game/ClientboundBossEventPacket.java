package net.minecraft.network.protocol.game;

import java.util.UUID;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.BossEvent;

public class ClientboundBossEventPacket implements Packet<ClientGamePacketListener> {
    private static final int FLAG_DARKEN = 1;
    private static final int FLAG_MUSIC = 2;
    private static final int FLAG_FOG = 4;
    private final UUID id;
    private final ClientboundBossEventPacket.Operation operation;
    static final ClientboundBossEventPacket.Operation REMOVE_OPERATION = new ClientboundBossEventPacket.Operation() {
        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.REMOVE;
        }

        @Override
        public void dispatch(UUID uuid, ClientboundBossEventPacket.Handler consumer) {
            consumer.remove(uuid);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
        }
    };

    private ClientboundBossEventPacket(UUID uuid, ClientboundBossEventPacket.Operation action) {
        this.id = uuid;
        this.operation = action;
    }

    public ClientboundBossEventPacket(FriendlyByteBuf buf) {
        this.id = buf.readUUID();
        ClientboundBossEventPacket.OperationType operationType = buf.readEnum(ClientboundBossEventPacket.OperationType.class);
        this.operation = operationType.reader.apply(buf);
    }

    public static ClientboundBossEventPacket createAddPacket(BossEvent bar) {
        return new ClientboundBossEventPacket(bar.getId(), new ClientboundBossEventPacket.AddOperation(bar));
    }

    public static ClientboundBossEventPacket createRemovePacket(UUID uuid) {
        return new ClientboundBossEventPacket(uuid, REMOVE_OPERATION);
    }

    public static ClientboundBossEventPacket createUpdateProgressPacket(BossEvent bar) {
        return new ClientboundBossEventPacket(bar.getId(), new ClientboundBossEventPacket.UpdateProgressOperation(bar.getProgress()));
    }

    public static ClientboundBossEventPacket createUpdateNamePacket(BossEvent bar) {
        return new ClientboundBossEventPacket(bar.getId(), new ClientboundBossEventPacket.UpdateNameOperation(bar.getName()));
    }

    public static ClientboundBossEventPacket createUpdateStylePacket(BossEvent bar) {
        return new ClientboundBossEventPacket(bar.getId(), new ClientboundBossEventPacket.UpdateStyleOperation(bar.getColor(), bar.getOverlay()));
    }

    public static ClientboundBossEventPacket createUpdatePropertiesPacket(BossEvent bar) {
        return new ClientboundBossEventPacket(bar.getId(), new ClientboundBossEventPacket.UpdatePropertiesOperation(bar.shouldDarkenScreen(), bar.shouldPlayBossMusic(), bar.shouldCreateWorldFog()));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.id);
        buf.writeEnum(this.operation.getType());
        this.operation.write(buf);
    }

    static int encodeProperties(boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
        int i = 0;
        if (darkenSky) {
            i |= 1;
        }

        if (dragonMusic) {
            i |= 2;
        }

        if (thickenFog) {
            i |= 4;
        }

        return i;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleBossUpdate(this);
    }

    public void dispatch(ClientboundBossEventPacket.Handler consumer) {
        this.operation.dispatch(this.id, consumer);
    }

    static class AddOperation implements ClientboundBossEventPacket.Operation {
        private final Component name;
        private final float progress;
        private final BossEvent.BossBarColor color;
        private final BossEvent.BossBarOverlay overlay;
        private final boolean darkenScreen;
        private final boolean playMusic;
        private final boolean createWorldFog;

        AddOperation(BossEvent bar) {
            this.name = bar.getName();
            this.progress = bar.getProgress();
            this.color = bar.getColor();
            this.overlay = bar.getOverlay();
            this.darkenScreen = bar.shouldDarkenScreen();
            this.playMusic = bar.shouldPlayBossMusic();
            this.createWorldFog = bar.shouldCreateWorldFog();
        }

        private AddOperation(FriendlyByteBuf buf) {
            this.name = buf.readComponent();
            this.progress = buf.readFloat();
            this.color = buf.readEnum(BossEvent.BossBarColor.class);
            this.overlay = buf.readEnum(BossEvent.BossBarOverlay.class);
            int i = buf.readUnsignedByte();
            this.darkenScreen = (i & 1) > 0;
            this.playMusic = (i & 2) > 0;
            this.createWorldFog = (i & 4) > 0;
        }

        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.ADD;
        }

        @Override
        public void dispatch(UUID uuid, ClientboundBossEventPacket.Handler consumer) {
            consumer.add(uuid, this.name, this.progress, this.color, this.overlay, this.darkenScreen, this.playMusic, this.createWorldFog);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeComponent(this.name);
            buf.writeFloat(this.progress);
            buf.writeEnum(this.color);
            buf.writeEnum(this.overlay);
            buf.writeByte(ClientboundBossEventPacket.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
        }
    }

    public interface Handler {
        default void add(UUID uuid, Component name, float percent, BossEvent.BossBarColor color, BossEvent.BossBarOverlay style, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
        }

        default void remove(UUID uuid) {
        }

        default void updateProgress(UUID uuid, float percent) {
        }

        default void updateName(UUID uuid, Component name) {
        }

        default void updateStyle(UUID id, BossEvent.BossBarColor color, BossEvent.BossBarOverlay style) {
        }

        default void updateProperties(UUID uuid, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
        }
    }

    interface Operation {
        ClientboundBossEventPacket.OperationType getType();

        void dispatch(UUID uuid, ClientboundBossEventPacket.Handler consumer);

        void write(FriendlyByteBuf buf);
    }

    static enum OperationType {
        ADD(ClientboundBossEventPacket.AddOperation::new),
        REMOVE((buf) -> {
            return ClientboundBossEventPacket.REMOVE_OPERATION;
        }),
        UPDATE_PROGRESS(ClientboundBossEventPacket.UpdateProgressOperation::new),
        UPDATE_NAME(ClientboundBossEventPacket.UpdateNameOperation::new),
        UPDATE_STYLE(ClientboundBossEventPacket.UpdateStyleOperation::new),
        UPDATE_PROPERTIES(ClientboundBossEventPacket.UpdatePropertiesOperation::new);

        final Function<FriendlyByteBuf, ClientboundBossEventPacket.Operation> reader;

        private OperationType(Function<FriendlyByteBuf, ClientboundBossEventPacket.Operation> parser) {
            this.reader = parser;
        }
    }

    static class UpdateNameOperation implements ClientboundBossEventPacket.Operation {
        private final Component name;

        UpdateNameOperation(Component name) {
            this.name = name;
        }

        private UpdateNameOperation(FriendlyByteBuf buf) {
            this.name = buf.readComponent();
        }

        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.UPDATE_NAME;
        }

        @Override
        public void dispatch(UUID uuid, ClientboundBossEventPacket.Handler consumer) {
            consumer.updateName(uuid, this.name);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeComponent(this.name);
        }
    }

    static class UpdateProgressOperation implements ClientboundBossEventPacket.Operation {
        private final float progress;

        UpdateProgressOperation(float percent) {
            this.progress = percent;
        }

        private UpdateProgressOperation(FriendlyByteBuf buf) {
            this.progress = buf.readFloat();
        }

        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.UPDATE_PROGRESS;
        }

        @Override
        public void dispatch(UUID uuid, ClientboundBossEventPacket.Handler consumer) {
            consumer.updateProgress(uuid, this.progress);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeFloat(this.progress);
        }
    }

    static class UpdatePropertiesOperation implements ClientboundBossEventPacket.Operation {
        private final boolean darkenScreen;
        private final boolean playMusic;
        private final boolean createWorldFog;

        UpdatePropertiesOperation(boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
            this.darkenScreen = darkenSky;
            this.playMusic = dragonMusic;
            this.createWorldFog = thickenFog;
        }

        private UpdatePropertiesOperation(FriendlyByteBuf buf) {
            int i = buf.readUnsignedByte();
            this.darkenScreen = (i & 1) > 0;
            this.playMusic = (i & 2) > 0;
            this.createWorldFog = (i & 4) > 0;
        }

        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.UPDATE_PROPERTIES;
        }

        @Override
        public void dispatch(UUID uuid, ClientboundBossEventPacket.Handler consumer) {
            consumer.updateProperties(uuid, this.darkenScreen, this.playMusic, this.createWorldFog);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeByte(ClientboundBossEventPacket.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
        }
    }

    static class UpdateStyleOperation implements ClientboundBossEventPacket.Operation {
        private final BossEvent.BossBarColor color;
        private final BossEvent.BossBarOverlay overlay;

        UpdateStyleOperation(BossEvent.BossBarColor color, BossEvent.BossBarOverlay style) {
            this.color = color;
            this.overlay = style;
        }

        private UpdateStyleOperation(FriendlyByteBuf buf) {
            this.color = buf.readEnum(BossEvent.BossBarColor.class);
            this.overlay = buf.readEnum(BossEvent.BossBarOverlay.class);
        }

        @Override
        public ClientboundBossEventPacket.OperationType getType() {
            return ClientboundBossEventPacket.OperationType.UPDATE_STYLE;
        }

        @Override
        public void dispatch(UUID uuid, ClientboundBossEventPacket.Handler consumer) {
            consumer.updateStyle(uuid, this.color, this.overlay);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeEnum(this.color);
            buf.writeEnum(this.overlay);
        }
    }
}
