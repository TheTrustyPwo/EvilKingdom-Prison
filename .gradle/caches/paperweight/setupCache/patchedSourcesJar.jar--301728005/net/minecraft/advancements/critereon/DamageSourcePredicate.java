package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

public class DamageSourcePredicate {
    public static final DamageSourcePredicate ANY = DamageSourcePredicate.Builder.damageType().build();
    @Nullable
    private final Boolean isProjectile;
    @Nullable
    private final Boolean isExplosion;
    @Nullable
    private final Boolean bypassesArmor;
    @Nullable
    private final Boolean bypassesInvulnerability;
    @Nullable
    private final Boolean bypassesMagic;
    @Nullable
    private final Boolean isFire;
    @Nullable
    private final Boolean isMagic;
    @Nullable
    private final Boolean isLightning;
    private final EntityPredicate directEntity;
    private final EntityPredicate sourceEntity;

    public DamageSourcePredicate(@Nullable Boolean isProjectile, @Nullable Boolean isExplosion, @Nullable Boolean bypassesArmor, @Nullable Boolean bypassesInvulnerability, @Nullable Boolean bypassesMagic, @Nullable Boolean isFire, @Nullable Boolean isMagic, @Nullable Boolean isLightning, EntityPredicate directEntity, EntityPredicate sourceEntity) {
        this.isProjectile = isProjectile;
        this.isExplosion = isExplosion;
        this.bypassesArmor = bypassesArmor;
        this.bypassesInvulnerability = bypassesInvulnerability;
        this.bypassesMagic = bypassesMagic;
        this.isFire = isFire;
        this.isMagic = isMagic;
        this.isLightning = isLightning;
        this.directEntity = directEntity;
        this.sourceEntity = sourceEntity;
    }

    public boolean matches(ServerPlayer player, DamageSource damageSource) {
        return this.matches(player.getLevel(), player.position(), damageSource);
    }

    public boolean matches(ServerLevel world, Vec3 pos, DamageSource damageSource) {
        if (this == ANY) {
            return true;
        } else if (this.isProjectile != null && this.isProjectile != damageSource.isProjectile()) {
            return false;
        } else if (this.isExplosion != null && this.isExplosion != damageSource.isExplosion()) {
            return false;
        } else if (this.bypassesArmor != null && this.bypassesArmor != damageSource.isBypassArmor()) {
            return false;
        } else if (this.bypassesInvulnerability != null && this.bypassesInvulnerability != damageSource.isBypassInvul()) {
            return false;
        } else if (this.bypassesMagic != null && this.bypassesMagic != damageSource.isBypassMagic()) {
            return false;
        } else if (this.isFire != null && this.isFire != damageSource.isFire()) {
            return false;
        } else if (this.isMagic != null && this.isMagic != damageSource.isMagic()) {
            return false;
        } else if (this.isLightning != null && this.isLightning != (damageSource == DamageSource.LIGHTNING_BOLT)) {
            return false;
        } else if (!this.directEntity.matches(world, pos, damageSource.getDirectEntity())) {
            return false;
        } else {
            return this.sourceEntity.matches(world, pos, damageSource.getEntity());
        }
    }

    public static DamageSourcePredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "damage type");
            Boolean boolean_ = getOptionalBoolean(jsonObject, "is_projectile");
            Boolean boolean2 = getOptionalBoolean(jsonObject, "is_explosion");
            Boolean boolean3 = getOptionalBoolean(jsonObject, "bypasses_armor");
            Boolean boolean4 = getOptionalBoolean(jsonObject, "bypasses_invulnerability");
            Boolean boolean5 = getOptionalBoolean(jsonObject, "bypasses_magic");
            Boolean boolean6 = getOptionalBoolean(jsonObject, "is_fire");
            Boolean boolean7 = getOptionalBoolean(jsonObject, "is_magic");
            Boolean boolean8 = getOptionalBoolean(jsonObject, "is_lightning");
            EntityPredicate entityPredicate = EntityPredicate.fromJson(jsonObject.get("direct_entity"));
            EntityPredicate entityPredicate2 = EntityPredicate.fromJson(jsonObject.get("source_entity"));
            return new DamageSourcePredicate(boolean_, boolean2, boolean3, boolean4, boolean5, boolean6, boolean7, boolean8, entityPredicate, entityPredicate2);
        } else {
            return ANY;
        }
    }

    @Nullable
    private static Boolean getOptionalBoolean(JsonObject obj, String name) {
        return obj.has(name) ? GsonHelper.getAsBoolean(obj, name) : null;
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            this.addOptionally(jsonObject, "is_projectile", this.isProjectile);
            this.addOptionally(jsonObject, "is_explosion", this.isExplosion);
            this.addOptionally(jsonObject, "bypasses_armor", this.bypassesArmor);
            this.addOptionally(jsonObject, "bypasses_invulnerability", this.bypassesInvulnerability);
            this.addOptionally(jsonObject, "bypasses_magic", this.bypassesMagic);
            this.addOptionally(jsonObject, "is_fire", this.isFire);
            this.addOptionally(jsonObject, "is_magic", this.isMagic);
            this.addOptionally(jsonObject, "is_lightning", this.isLightning);
            jsonObject.add("direct_entity", this.directEntity.serializeToJson());
            jsonObject.add("source_entity", this.sourceEntity.serializeToJson());
            return jsonObject;
        }
    }

    private void addOptionally(JsonObject json, String key, @Nullable Boolean value) {
        if (value != null) {
            json.addProperty(key, value);
        }

    }

    public static class Builder {
        @Nullable
        private Boolean isProjectile;
        @Nullable
        private Boolean isExplosion;
        @Nullable
        private Boolean bypassesArmor;
        @Nullable
        private Boolean bypassesInvulnerability;
        @Nullable
        private Boolean bypassesMagic;
        @Nullable
        private Boolean isFire;
        @Nullable
        private Boolean isMagic;
        @Nullable
        private Boolean isLightning;
        private EntityPredicate directEntity = EntityPredicate.ANY;
        private EntityPredicate sourceEntity = EntityPredicate.ANY;

        public static DamageSourcePredicate.Builder damageType() {
            return new DamageSourcePredicate.Builder();
        }

        public DamageSourcePredicate.Builder isProjectile(Boolean projectile) {
            this.isProjectile = projectile;
            return this;
        }

        public DamageSourcePredicate.Builder isExplosion(Boolean explosion) {
            this.isExplosion = explosion;
            return this;
        }

        public DamageSourcePredicate.Builder bypassesArmor(Boolean bypassesArmor) {
            this.bypassesArmor = bypassesArmor;
            return this;
        }

        public DamageSourcePredicate.Builder bypassesInvulnerability(Boolean bypassesInvulnerability) {
            this.bypassesInvulnerability = bypassesInvulnerability;
            return this;
        }

        public DamageSourcePredicate.Builder bypassesMagic(Boolean bypassesMagic) {
            this.bypassesMagic = bypassesMagic;
            return this;
        }

        public DamageSourcePredicate.Builder isFire(Boolean fire) {
            this.isFire = fire;
            return this;
        }

        public DamageSourcePredicate.Builder isMagic(Boolean magic) {
            this.isMagic = magic;
            return this;
        }

        public DamageSourcePredicate.Builder isLightning(Boolean lightning) {
            this.isLightning = lightning;
            return this;
        }

        public DamageSourcePredicate.Builder direct(EntityPredicate entity) {
            this.directEntity = entity;
            return this;
        }

        public DamageSourcePredicate.Builder direct(EntityPredicate.Builder entity) {
            this.directEntity = entity.build();
            return this;
        }

        public DamageSourcePredicate.Builder source(EntityPredicate entity) {
            this.sourceEntity = entity;
            return this;
        }

        public DamageSourcePredicate.Builder source(EntityPredicate.Builder entity) {
            this.sourceEntity = entity.build();
            return this;
        }

        public DamageSourcePredicate build() {
            return new DamageSourcePredicate(this.isProjectile, this.isExplosion, this.bypassesArmor, this.bypassesInvulnerability, this.bypassesMagic, this.isFire, this.isMagic, this.isLightning, this.directEntity, this.sourceEntity);
        }
    }
}
