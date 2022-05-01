package net.minecraft.network.protocol.game;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class ClientboundPlayerInfoPacket implements Packet<ClientGamePacketListener> {
    private final ClientboundPlayerInfoPacket.Action action;
    private final List<ClientboundPlayerInfoPacket.PlayerUpdate> entries;

    public ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action action, ServerPlayer... players) {
        this.action = action;
        this.entries = Lists.newArrayListWithCapacity(players.length);

        for(ServerPlayer serverPlayer : players) {
            this.entries.add(new ClientboundPlayerInfoPacket.PlayerUpdate(serverPlayer.getGameProfile(), serverPlayer.latency, serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer.getTabListDisplayName()));
        }

    }

    public ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action action, Collection<ServerPlayer> players) {
        this.action = action;
        this.entries = Lists.newArrayListWithCapacity(players.size());

        for(ServerPlayer serverPlayer : players) {
            this.entries.add(new ClientboundPlayerInfoPacket.PlayerUpdate(serverPlayer.getGameProfile(), serverPlayer.latency, serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer.getTabListDisplayName()));
        }

    }

    public ClientboundPlayerInfoPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(ClientboundPlayerInfoPacket.Action.class);
        this.entries = buf.readList(this.action::read);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        buf.writeCollection(this.entries, this.action::write);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handlePlayerInfo(this);
    }

    public List<ClientboundPlayerInfoPacket.PlayerUpdate> getEntries() {
        return this.entries;
    }

    public ClientboundPlayerInfoPacket.Action getAction() {
        return this.action;
    }

    @Nullable
    static Component readDisplayName(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readComponent() : null;
    }

    static void writeDisplayName(FriendlyByteBuf buf, @Nullable Component text) {
        if (text == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeComponent(text);
        }

    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("action", this.action).add("entries", this.entries).toString();
    }

    public static enum Action {
        ADD_PLAYER {
            @Override
            protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), buf.readUtf(16));
                PropertyMap propertyMap = gameProfile.getProperties();
                buf.readWithCount((bufx) -> {
                    String string = bufx.readUtf();
                    String string2 = bufx.readUtf();
                    if (bufx.readBoolean()) {
                        String string3 = bufx.readUtf();
                        propertyMap.put(string, new Property(string, string2, string3));
                    } else {
                        propertyMap.put(string, new Property(string, string2));
                    }

                });
                GameType gameType = GameType.byId(buf.readVarInt());
                int i = buf.readVarInt();
                Component component = ClientboundPlayerInfoPacket.readDisplayName(buf);
                return new ClientboundPlayerInfoPacket.PlayerUpdate(gameProfile, i, gameType, component);
            }

            @Override
            protected void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry) {
                buf.writeUUID(entry.getProfile().getId());
                buf.writeUtf(entry.getProfile().getName());
                buf.writeCollection(entry.getProfile().getProperties().values(), (bufx, property) -> {
                    bufx.writeUtf(property.getName());
                    bufx.writeUtf(property.getValue());
                    if (property.hasSignature()) {
                        bufx.writeBoolean(true);
                        bufx.writeUtf(property.getSignature());
                    } else {
                        bufx.writeBoolean(false);
                    }

                });
                buf.writeVarInt(entry.getGameMode().getId());
                buf.writeVarInt(entry.getLatency());
                ClientboundPlayerInfoPacket.writeDisplayName(buf, entry.getDisplayName());
            }
        },
        UPDATE_GAME_MODE {
            @Override
            protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                GameType gameType = GameType.byId(buf.readVarInt());
                return new ClientboundPlayerInfoPacket.PlayerUpdate(gameProfile, 0, gameType, (Component)null);
            }

            @Override
            protected void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry) {
                buf.writeUUID(entry.getProfile().getId());
                buf.writeVarInt(entry.getGameMode().getId());
            }
        },
        UPDATE_LATENCY {
            @Override
            protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                int i = buf.readVarInt();
                return new ClientboundPlayerInfoPacket.PlayerUpdate(gameProfile, i, (GameType)null, (Component)null);
            }

            @Override
            protected void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry) {
                buf.writeUUID(entry.getProfile().getId());
                buf.writeVarInt(entry.getLatency());
            }
        },
        UPDATE_DISPLAY_NAME {
            @Override
            protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                Component component = ClientboundPlayerInfoPacket.readDisplayName(buf);
                return new ClientboundPlayerInfoPacket.PlayerUpdate(gameProfile, 0, (GameType)null, component);
            }

            @Override
            protected void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry) {
                buf.writeUUID(entry.getProfile().getId());
                ClientboundPlayerInfoPacket.writeDisplayName(buf, entry.getDisplayName());
            }
        },
        REMOVE_PLAYER {
            @Override
            protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                return new ClientboundPlayerInfoPacket.PlayerUpdate(gameProfile, 0, (GameType)null, (Component)null);
            }

            @Override
            protected void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry) {
                buf.writeUUID(entry.getProfile().getId());
            }
        };

        protected abstract ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf buf);

        protected abstract void write(FriendlyByteBuf buf, ClientboundPlayerInfoPacket.PlayerUpdate entry);
    }

    public static class PlayerUpdate {
        private final int latency;
        private final GameType gameMode;
        private final GameProfile profile;
        @Nullable
        private final Component displayName;

        public PlayerUpdate(GameProfile profile, int latency, @Nullable GameType gameMode, @Nullable Component displayName) {
            this.profile = profile;
            this.latency = latency;
            this.gameMode = gameMode;
            this.displayName = displayName;
        }

        public GameProfile getProfile() {
            return this.profile;
        }

        public int getLatency() {
            return this.latency;
        }

        public GameType getGameMode() {
            return this.gameMode;
        }

        @Nullable
        public Component getDisplayName() {
            return this.displayName;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("latency", this.latency).add("gameMode", this.gameMode).add("profile", this.profile).add("displayName", this.displayName == null ? null : Component.Serializer.toJson(this.displayName)).toString();
        }
    }
}
