package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;

public class LightPredicate {
    public static final LightPredicate ANY = new LightPredicate(MinMaxBounds.Ints.ANY);
    private final MinMaxBounds.Ints composite;

    LightPredicate(MinMaxBounds.Ints range) {
        this.composite = range;
    }

    public boolean matches(ServerLevel world, BlockPos pos) {
        if (this == ANY) {
            return true;
        } else if (!world.isLoaded(pos)) {
            return false;
        } else {
            return this.composite.matches(world.getMaxLocalRawBrightness(pos));
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("light", this.composite.serializeToJson());
            return jsonObject;
        }
    }

    public static LightPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "light");
            MinMaxBounds.Ints ints = MinMaxBounds.Ints.fromJson(jsonObject.get("light"));
            return new LightPredicate(ints);
        } else {
            return ANY;
        }
    }

    public static class Builder {
        private MinMaxBounds.Ints composite = MinMaxBounds.Ints.ANY;

        public static LightPredicate.Builder light() {
            return new LightPredicate.Builder();
        }

        public LightPredicate.Builder setComposite(MinMaxBounds.Ints light) {
            this.composite = light;
            return this;
        }

        public LightPredicate build() {
            return new LightPredicate(this.composite);
        }
    }
}
