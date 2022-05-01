package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

public class DistancePredicate {
    public static final DistancePredicate ANY = new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY);
    private final MinMaxBounds.Doubles x;
    private final MinMaxBounds.Doubles y;
    private final MinMaxBounds.Doubles z;
    private final MinMaxBounds.Doubles horizontal;
    private final MinMaxBounds.Doubles absolute;

    public DistancePredicate(MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z, MinMaxBounds.Doubles horizontal, MinMaxBounds.Doubles absolute) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.horizontal = horizontal;
        this.absolute = absolute;
    }

    public static DistancePredicate horizontal(MinMaxBounds.Doubles horizontal) {
        return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, horizontal, MinMaxBounds.Doubles.ANY);
    }

    public static DistancePredicate vertical(MinMaxBounds.Doubles y) {
        return new DistancePredicate(MinMaxBounds.Doubles.ANY, y, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY);
    }

    public static DistancePredicate absolute(MinMaxBounds.Doubles absolute) {
        return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, absolute);
    }

    public boolean matches(double x0, double y0, double z0, double x1, double y1, double z1) {
        float f = (float)(x0 - x1);
        float g = (float)(y0 - y1);
        float h = (float)(z0 - z1);
        if (this.x.matches((double)Mth.abs(f)) && this.y.matches((double)Mth.abs(g)) && this.z.matches((double)Mth.abs(h))) {
            if (!this.horizontal.matchesSqr((double)(f * f + h * h))) {
                return false;
            } else {
                return this.absolute.matchesSqr((double)(f * f + g * g + h * h));
            }
        } else {
            return false;
        }
    }

    public static DistancePredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "distance");
            MinMaxBounds.Doubles doubles = MinMaxBounds.Doubles.fromJson(jsonObject.get("x"));
            MinMaxBounds.Doubles doubles2 = MinMaxBounds.Doubles.fromJson(jsonObject.get("y"));
            MinMaxBounds.Doubles doubles3 = MinMaxBounds.Doubles.fromJson(jsonObject.get("z"));
            MinMaxBounds.Doubles doubles4 = MinMaxBounds.Doubles.fromJson(jsonObject.get("horizontal"));
            MinMaxBounds.Doubles doubles5 = MinMaxBounds.Doubles.fromJson(jsonObject.get("absolute"));
            return new DistancePredicate(doubles, doubles2, doubles3, doubles4, doubles5);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("x", this.x.serializeToJson());
            jsonObject.add("y", this.y.serializeToJson());
            jsonObject.add("z", this.z.serializeToJson());
            jsonObject.add("horizontal", this.horizontal.serializeToJson());
            jsonObject.add("absolute", this.absolute.serializeToJson());
            return jsonObject;
        }
    }
}
