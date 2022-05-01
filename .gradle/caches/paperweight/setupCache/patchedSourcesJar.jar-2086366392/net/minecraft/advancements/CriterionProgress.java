package net.minecraft.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;

public class CriterionProgress {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    @Nullable
    private Date obtained;

    public boolean isDone() {
        return this.obtained != null;
    }

    public void grant() {
        this.obtained = new Date();
    }

    public void revoke() {
        this.obtained = null;
    }

    @Nullable
    public Date getObtained() {
        return this.obtained;
    }

    @Override
    public String toString() {
        return "CriterionProgress{obtained=" + (this.obtained == null ? "false" : this.obtained) + "}";
    }

    public void serializeToNetwork(FriendlyByteBuf buf) {
        buf.writeBoolean(this.obtained != null);
        if (this.obtained != null) {
            buf.writeDate(this.obtained);
        }

    }

    public JsonElement serializeToJson() {
        return (JsonElement)(this.obtained != null ? new JsonPrimitive(DATE_FORMAT.format(this.obtained)) : JsonNull.INSTANCE);
    }

    public static CriterionProgress fromNetwork(FriendlyByteBuf buf) {
        CriterionProgress criterionProgress = new CriterionProgress();
        if (buf.readBoolean()) {
            criterionProgress.obtained = buf.readDate();
        }

        return criterionProgress;
    }

    public static CriterionProgress fromJson(String datetime) {
        CriterionProgress criterionProgress = new CriterionProgress();

        try {
            criterionProgress.obtained = DATE_FORMAT.parse(datetime);
            return criterionProgress;
        } catch (ParseException var3) {
            throw new JsonSyntaxException("Invalid datetime: " + datetime, var3);
        }
    }
}
