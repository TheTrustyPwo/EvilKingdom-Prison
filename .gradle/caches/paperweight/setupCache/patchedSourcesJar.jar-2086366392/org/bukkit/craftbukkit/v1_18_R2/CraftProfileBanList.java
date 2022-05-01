package org.bukkit.craftbukkit.v1_18_R2;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;

public class CraftProfileBanList implements org.bukkit.BanList {
    private final UserBanList list;

    public CraftProfileBanList(UserBanList list) {
        this.list = list;
    }

    @Override
    public org.bukkit.BanEntry getBanEntry(String target) {
        Validate.notNull(target, "Target cannot be null");

        GameProfile profile = this.getProfile(target);
        if (profile == null) {
            return null;
        }

        UserBanListEntry entry = (UserBanListEntry) this.list.get(profile);
        if (entry == null) {
            return null;
        }

        return new CraftProfileBanEntry(profile, entry, this.list);
    }

    @Override
    public org.bukkit.BanEntry addBan(String target, String reason, Date expires, String source) {
        Validate.notNull(target, "Ban target cannot be null");

        GameProfile profile = this.getProfile(target);
        if (profile == null) {
            return null;
        }

        UserBanListEntry entry = new UserBanListEntry(profile, new Date(),
                StringUtils.isBlank(source) ? null : source, expires,
                StringUtils.isBlank(reason) ? null : reason);

        this.list.add(entry);

        try {
            this.list.save();
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save banned-players.json, {0}", ex.getMessage());
        }

        return new CraftProfileBanEntry(profile, entry, this.list);
    }

    @Override
    public Set<org.bukkit.BanEntry> getBanEntries() {
        ImmutableSet.Builder<org.bukkit.BanEntry> builder = ImmutableSet.builder();

        for (StoredUserEntry entry : this.list.getValues()) {
            GameProfile profile = (GameProfile) entry.getUser();
            builder.add(new CraftProfileBanEntry(profile, (UserBanListEntry) entry, this.list));
        }

        return builder.build();
    }

    @Override
    public boolean isBanned(String target) {
        Validate.notNull(target, "Target cannot be null");

        GameProfile profile = this.getProfile(target);
        if (profile == null) {
            return false;
        }

        return this.list.isBanned(profile);
    }

    @Override
    public void pardon(String target) {
        Validate.notNull(target, "Target cannot be null");

        GameProfile profile = this.getProfile(target);
        this.list.remove(profile);
    }

    private GameProfile getProfile(String target) {
        UUID uuid = null;

        try {
            uuid = UUID.fromString(target);
        } catch (IllegalArgumentException ex) {
            //
        }

        return ((uuid != null) ? MinecraftServer.getServer().getProfileCache().get(uuid) : MinecraftServer.getServer().getProfileCache().get(target)).orElse(null);
    }
}
