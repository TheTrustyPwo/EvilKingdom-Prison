package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class UserBanListEntry extends BanListEntry<GameProfile> {
    public UserBanListEntry(GameProfile profile) {
        this(profile, (Date)null, (String)null, (Date)null, (String)null);
    }

    public UserBanListEntry(GameProfile profile, @Nullable Date created, @Nullable String source, @Nullable Date expiry, @Nullable String reason) {
        super(profile, created, source, expiry, reason);
    }

    public UserBanListEntry(JsonObject json) {
        super(createGameProfile(json), json);
    }

    @Override
    protected void serialize(JsonObject json) {
        if (this.getUser() != null) {
            json.addProperty("uuid", this.getUser().getId() == null ? "" : this.getUser().getId().toString());
            json.addProperty("name", this.getUser().getName());
            super.serialize(json);
        }
    }

    @Override
    public Component getDisplayName() {
        GameProfile gameProfile = this.getUser();
        return new TextComponent(gameProfile.getName() != null ? gameProfile.getName() : Objects.toString(gameProfile.getId(), "(Unknown)"));
    }

    private static GameProfile createGameProfile(JsonObject json) {
        if (json.has("uuid") && json.has("name")) {
            String string = json.get("uuid").getAsString();

            UUID uUID;
            try {
                uUID = UUID.fromString(string);
            } catch (Throwable var4) {
                return null;
            }

            return new GameProfile(uUID, json.get("name").getAsString());
        } else {
            return null;
        }
    }
}
