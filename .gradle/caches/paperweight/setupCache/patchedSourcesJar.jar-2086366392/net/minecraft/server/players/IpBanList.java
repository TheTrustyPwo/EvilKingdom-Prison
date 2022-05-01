package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.net.SocketAddress;
import javax.annotation.Nullable;

public class IpBanList extends StoredUserList<String, IpBanListEntry> {
    public IpBanList(File file) {
        super(file);
    }

    @Override
    protected StoredUserEntry<String> createEntry(JsonObject json) {
        return new IpBanListEntry(json);
    }

    public boolean isBanned(SocketAddress ip) {
        String string = this.getIpFromAddress(ip);
        return this.contains(string);
    }

    public boolean isBanned(String ip) {
        return this.contains(ip);
    }

    @Nullable
    public IpBanListEntry get(SocketAddress address) {
        String string = this.getIpFromAddress(address);
        return this.get(string);
    }

    private String getIpFromAddress(SocketAddress address) {
        String string = address.toString();
        if (string.contains("/")) {
            string = string.substring(string.indexOf(47) + 1);
        }

        if (string.contains(":")) {
            string = string.substring(0, string.indexOf(58));
        }

        return string;
    }
}
