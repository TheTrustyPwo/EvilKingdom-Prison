package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

public abstract class BanListEntry<T> extends StoredUserEntry<T> {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    public static final String EXPIRES_NEVER = "forever";
    protected final Date created;
    protected final String source;
    @Nullable
    protected final Date expires;
    protected final String reason;

    public BanListEntry(T key, @Nullable Date creationDate, @Nullable String source, @Nullable Date expiryDate, @Nullable String reason) {
        super(key);
        this.created = creationDate == null ? new Date() : creationDate;
        this.source = source == null ? "(Unknown)" : source;
        this.expires = expiryDate;
        this.reason = reason == null ? "Banned by an operator." : reason;
    }

    protected BanListEntry(T key, JsonObject json) {
        super(key);

        Date date;
        try {
            date = json.has("created") ? DATE_FORMAT.parse(json.get("created").getAsString()) : new Date();
        } catch (ParseException var7) {
            date = new Date();
        }

        this.created = date;
        this.source = json.has("source") ? json.get("source").getAsString() : "(Unknown)";

        Date date3;
        try {
            date3 = json.has("expires") ? DATE_FORMAT.parse(json.get("expires").getAsString()) : null;
        } catch (ParseException var6) {
            date3 = null;
        }

        this.expires = date3;
        this.reason = json.has("reason") ? json.get("reason").getAsString() : "Banned by an operator.";
    }

    public Date getCreated() {
        return this.created;
    }

    public String getSource() {
        return this.source;
    }

    @Nullable
    public Date getExpires() {
        return this.expires;
    }

    public String getReason() {
        return this.reason;
    }

    public abstract Component getDisplayName();

    @Override
    boolean hasExpired() {
        return this.expires == null ? false : this.expires.before(new Date());
    }

    @Override
    protected void serialize(JsonObject json) {
        json.addProperty("created", DATE_FORMAT.format(this.created));
        json.addProperty("source", this.source);
        json.addProperty("expires", this.expires == null ? "forever" : DATE_FORMAT.format(this.expires));
        json.addProperty("reason", this.reason);
    }
}
