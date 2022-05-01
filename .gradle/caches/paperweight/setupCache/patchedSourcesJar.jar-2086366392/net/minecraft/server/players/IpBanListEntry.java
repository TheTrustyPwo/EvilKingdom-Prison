package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.util.Date;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class IpBanListEntry extends BanListEntry<String> {
    public IpBanListEntry(String ip) {
        this(ip, (Date)null, (String)null, (Date)null, (String)null);
    }

    public IpBanListEntry(String ip, @Nullable Date created, @Nullable String source, @Nullable Date expiry, @Nullable String reason) {
        super(ip, created, source, expiry, reason);
    }

    @Override
    public Component getDisplayName() {
        return new TextComponent(String.valueOf(this.getUser()));
    }

    public IpBanListEntry(JsonObject json) {
        super(createIpInfo(json), json);
    }

    private static String createIpInfo(JsonObject json) {
        return json.has("ip") ? json.get("ip").getAsString() : null;
    }

    @Override
    protected void serialize(JsonObject json) {
        if (this.getUser() != null) {
            json.addProperty("ip", this.getUser());
            super.serialize(json);
        }
    }
}
