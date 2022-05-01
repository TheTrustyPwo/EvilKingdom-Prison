package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;

public class DamagePredicate {
    public static final DamagePredicate ANY = DamagePredicate.Builder.damageInstance().build();
    private final MinMaxBounds.Doubles dealtDamage;
    private final MinMaxBounds.Doubles takenDamage;
    private final EntityPredicate sourceEntity;
    @Nullable
    private final Boolean blocked;
    private final DamageSourcePredicate type;

    public DamagePredicate() {
        this.dealtDamage = MinMaxBounds.Doubles.ANY;
        this.takenDamage = MinMaxBounds.Doubles.ANY;
        this.sourceEntity = EntityPredicate.ANY;
        this.blocked = null;
        this.type = DamageSourcePredicate.ANY;
    }

    public DamagePredicate(MinMaxBounds.Doubles dealt, MinMaxBounds.Doubles taken, EntityPredicate sourceEntity, @Nullable Boolean blocked, DamageSourcePredicate type) {
        this.dealtDamage = dealt;
        this.takenDamage = taken;
        this.sourceEntity = sourceEntity;
        this.blocked = blocked;
        this.type = type;
    }

    public boolean matches(ServerPlayer player, DamageSource source, float dealt, float taken, boolean blocked) {
        if (this == ANY) {
            return true;
        } else if (!this.dealtDamage.matches((double)dealt)) {
            return false;
        } else if (!this.takenDamage.matches((double)taken)) {
            return false;
        } else if (!this.sourceEntity.matches(player, source.getEntity())) {
            return false;
        } else if (this.blocked != null && this.blocked != blocked) {
            return false;
        } else {
            return this.type.matches(player, source);
        }
    }

    public static DamagePredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "damage");
            MinMaxBounds.Doubles doubles = MinMaxBounds.Doubles.fromJson(jsonObject.get("dealt"));
            MinMaxBounds.Doubles doubles2 = MinMaxBounds.Doubles.fromJson(jsonObject.get("taken"));
            Boolean boolean_ = jsonObject.has("blocked") ? GsonHelper.getAsBoolean(jsonObject, "blocked") : null;
            EntityPredicate entityPredicate = EntityPredicate.fromJson(jsonObject.get("source_entity"));
            DamageSourcePredicate damageSourcePredicate = DamageSourcePredicate.fromJson(jsonObject.get("type"));
            return new DamagePredicate(doubles, doubles2, entityPredicate, boolean_, damageSourcePredicate);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("dealt", this.dealtDamage.serializeToJson());
            jsonObject.add("taken", this.takenDamage.serializeToJson());
            jsonObject.add("source_entity", this.sourceEntity.serializeToJson());
            jsonObject.add("type", this.type.serializeToJson());
            if (this.blocked != null) {
                jsonObject.addProperty("blocked", this.blocked);
            }

            return jsonObject;
        }
    }

    public static class Builder {
        private MinMaxBounds.Doubles dealtDamage = MinMaxBounds.Doubles.ANY;
        private MinMaxBounds.Doubles takenDamage = MinMaxBounds.Doubles.ANY;
        private EntityPredicate sourceEntity = EntityPredicate.ANY;
        @Nullable
        private Boolean blocked;
        private DamageSourcePredicate type = DamageSourcePredicate.ANY;

        public static DamagePredicate.Builder damageInstance() {
            return new DamagePredicate.Builder();
        }

        public DamagePredicate.Builder dealtDamage(MinMaxBounds.Doubles dealt) {
            this.dealtDamage = dealt;
            return this;
        }

        public DamagePredicate.Builder takenDamage(MinMaxBounds.Doubles taken) {
            this.takenDamage = taken;
            return this;
        }

        public DamagePredicate.Builder sourceEntity(EntityPredicate sourceEntity) {
            this.sourceEntity = sourceEntity;
            return this;
        }

        public DamagePredicate.Builder blocked(Boolean blocked) {
            this.blocked = blocked;
            return this;
        }

        public DamagePredicate.Builder type(DamageSourcePredicate type) {
            this.type = type;
            return this;
        }

        public DamagePredicate.Builder type(DamageSourcePredicate.Builder builder) {
            this.type = builder.build();
            return this;
        }

        public DamagePredicate build() {
            return new DamagePredicate(this.dealtDamage, this.takenDamage, this.sourceEntity, this.blocked, this.type);
        }
    }
}
