package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

public class UserWhiteListEntry extends StoredUserEntry<GameProfile> {
    public UserWhiteListEntry(GameProfile profile) {
        super(profile);
    }

    public UserWhiteListEntry(JsonObject json) {
        super(createGameProfile(json));
    }

    @Override
    protected void serialize(JsonObject json) {
        if (this.getUser() != null) {
            json.addProperty("uuid", this.getUser().getId() == null ? "" : this.getUser().getId().toString());
            json.addProperty("name", this.getUser().getName());
        }
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
