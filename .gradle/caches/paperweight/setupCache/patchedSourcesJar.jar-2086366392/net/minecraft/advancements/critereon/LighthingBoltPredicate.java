package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public class LighthingBoltPredicate {
    public static final LighthingBoltPredicate ANY = new LighthingBoltPredicate(MinMaxBounds.Ints.ANY, EntityPredicate.ANY);
    private static final String BLOCKS_SET_ON_FIRE_KEY = "blocks_set_on_fire";
    private static final String ENTITY_STRUCK_KEY = "entity_struck";
    private final MinMaxBounds.Ints blocksSetOnFire;
    private final EntityPredicate entityStruck;

    private LighthingBoltPredicate(MinMaxBounds.Ints blocksSetOnFire, EntityPredicate entityStruck) {
        this.blocksSetOnFire = blocksSetOnFire;
        this.entityStruck = entityStruck;
    }

    public static LighthingBoltPredicate blockSetOnFire(MinMaxBounds.Ints blocksSetOnFire) {
        return new LighthingBoltPredicate(blocksSetOnFire, EntityPredicate.ANY);
    }

    public static LighthingBoltPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "lightning");
            return new LighthingBoltPredicate(MinMaxBounds.Ints.fromJson(jsonObject.get("blocks_set_on_fire")), EntityPredicate.fromJson(jsonObject.get("entity_struck")));
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("blocks_set_on_fire", this.blocksSetOnFire.serializeToJson());
            jsonObject.add("entity_struck", this.entityStruck.serializeToJson());
            return jsonObject;
        }
    }

    public boolean matches(Entity lightningBolt, ServerLevel world, @Nullable Vec3 vec3) {
        if (this == ANY) {
            return true;
        } else if (!(lightningBolt instanceof LightningBolt)) {
            return false;
        } else {
            LightningBolt lightningBolt2 = (LightningBolt)lightningBolt;
            return this.blocksSetOnFire.matches(lightningBolt2.getBlocksSetOnFire()) && (this.entityStruck == EntityPredicate.ANY || lightningBolt2.getHitEntities().anyMatch((entity) -> {
                return this.entityStruck.matches(world, vec3, entity);
            }));
        }
    }
}
