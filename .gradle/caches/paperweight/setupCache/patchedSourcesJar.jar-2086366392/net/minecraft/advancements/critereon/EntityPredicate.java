package net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

public class EntityPredicate {
    public static final EntityPredicate ANY = new EntityPredicate(EntityTypePredicate.ANY, DistancePredicate.ANY, LocationPredicate.ANY, LocationPredicate.ANY, MobEffectsPredicate.ANY, NbtPredicate.ANY, EntityFlagsPredicate.ANY, EntityEquipmentPredicate.ANY, PlayerPredicate.ANY, FishingHookPredicate.ANY, LighthingBoltPredicate.ANY, (String)null, (ResourceLocation)null);
    private final EntityTypePredicate entityType;
    private final DistancePredicate distanceToPlayer;
    private final LocationPredicate location;
    private final LocationPredicate steppingOnLocation;
    private final MobEffectsPredicate effects;
    private final NbtPredicate nbt;
    private final EntityFlagsPredicate flags;
    private final EntityEquipmentPredicate equipment;
    private final PlayerPredicate player;
    private final FishingHookPredicate fishingHook;
    private final LighthingBoltPredicate lighthingBolt;
    private final EntityPredicate vehicle;
    private final EntityPredicate passenger;
    private final EntityPredicate targetedEntity;
    @Nullable
    private final String team;
    @Nullable
    private final ResourceLocation catType;

    private EntityPredicate(EntityTypePredicate type, DistancePredicate distance, LocationPredicate location, LocationPredicate steppingOn, MobEffectsPredicate effects, NbtPredicate nbt, EntityFlagsPredicate flags, EntityEquipmentPredicate equipment, PlayerPredicate player, FishingHookPredicate fishingHook, LighthingBoltPredicate lightningBolt, @Nullable String team, @Nullable ResourceLocation catType) {
        this.entityType = type;
        this.distanceToPlayer = distance;
        this.location = location;
        this.steppingOnLocation = steppingOn;
        this.effects = effects;
        this.nbt = nbt;
        this.flags = flags;
        this.equipment = equipment;
        this.player = player;
        this.fishingHook = fishingHook;
        this.lighthingBolt = lightningBolt;
        this.passenger = this;
        this.vehicle = this;
        this.targetedEntity = this;
        this.team = team;
        this.catType = catType;
    }

    EntityPredicate(EntityTypePredicate type, DistancePredicate distance, LocationPredicate location, LocationPredicate steppingOn, MobEffectsPredicate effects, NbtPredicate nbt, EntityFlagsPredicate flags, EntityEquipmentPredicate equipment, PlayerPredicate player, FishingHookPredicate fishingHook, LighthingBoltPredicate lightningBolt, EntityPredicate vehicle, EntityPredicate passenger, EntityPredicate targetedEntity, @Nullable String team, @Nullable ResourceLocation catType) {
        this.entityType = type;
        this.distanceToPlayer = distance;
        this.location = location;
        this.steppingOnLocation = steppingOn;
        this.effects = effects;
        this.nbt = nbt;
        this.flags = flags;
        this.equipment = equipment;
        this.player = player;
        this.fishingHook = fishingHook;
        this.lighthingBolt = lightningBolt;
        this.vehicle = vehicle;
        this.passenger = passenger;
        this.targetedEntity = targetedEntity;
        this.team = team;
        this.catType = catType;
    }

    public boolean matches(ServerPlayer player, @Nullable Entity entity) {
        return this.matches(player.getLevel(), player.position(), entity);
    }

    public boolean matches(ServerLevel world, @Nullable Vec3 pos, @Nullable Entity entity) {
        if (this == ANY) {
            return true;
        } else if (entity == null) {
            return false;
        } else if (!this.entityType.matches(entity.getType())) {
            return false;
        } else {
            if (pos == null) {
                if (this.distanceToPlayer != DistancePredicate.ANY) {
                    return false;
                }
            } else if (!this.distanceToPlayer.matches(pos.x, pos.y, pos.z, entity.getX(), entity.getY(), entity.getZ())) {
                return false;
            }

            if (!this.location.matches(world, entity.getX(), entity.getY(), entity.getZ())) {
                return false;
            } else {
                if (this.steppingOnLocation != LocationPredicate.ANY) {
                    Vec3 vec3 = Vec3.atCenterOf(entity.getOnPos());
                    if (!this.steppingOnLocation.matches(world, vec3.x(), vec3.y(), vec3.z())) {
                        return false;
                    }
                }

                if (!this.effects.matches(entity)) {
                    return false;
                } else if (!this.nbt.matches(entity)) {
                    return false;
                } else if (!this.flags.matches(entity)) {
                    return false;
                } else if (!this.equipment.matches(entity)) {
                    return false;
                } else if (!this.player.matches(entity)) {
                    return false;
                } else if (!this.fishingHook.matches(entity)) {
                    return false;
                } else if (!this.lighthingBolt.matches(entity, world, pos)) {
                    return false;
                } else if (!this.vehicle.matches(world, pos, entity.getVehicle())) {
                    return false;
                } else if (this.passenger != ANY && entity.getPassengers().stream().noneMatch((entityx) -> {
                    return this.passenger.matches(world, pos, entityx);
                })) {
                    return false;
                } else if (!this.targetedEntity.matches(world, pos, entity instanceof Mob ? ((Mob)entity).getTarget() : null)) {
                    return false;
                } else {
                    if (this.team != null) {
                        Team team = entity.getTeam();
                        if (team == null || !this.team.equals(team.getName())) {
                            return false;
                        }
                    }

                    return this.catType == null || entity instanceof Cat && ((Cat)entity).getResourceLocation().equals(this.catType);
                }
            }
        }
    }

    public static EntityPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "entity");
            EntityTypePredicate entityTypePredicate = EntityTypePredicate.fromJson(jsonObject.get("type"));
            DistancePredicate distancePredicate = DistancePredicate.fromJson(jsonObject.get("distance"));
            LocationPredicate locationPredicate = LocationPredicate.fromJson(jsonObject.get("location"));
            LocationPredicate locationPredicate2 = LocationPredicate.fromJson(jsonObject.get("stepping_on"));
            MobEffectsPredicate mobEffectsPredicate = MobEffectsPredicate.fromJson(jsonObject.get("effects"));
            NbtPredicate nbtPredicate = NbtPredicate.fromJson(jsonObject.get("nbt"));
            EntityFlagsPredicate entityFlagsPredicate = EntityFlagsPredicate.fromJson(jsonObject.get("flags"));
            EntityEquipmentPredicate entityEquipmentPredicate = EntityEquipmentPredicate.fromJson(jsonObject.get("equipment"));
            PlayerPredicate playerPredicate = PlayerPredicate.fromJson(jsonObject.get("player"));
            FishingHookPredicate fishingHookPredicate = FishingHookPredicate.fromJson(jsonObject.get("fishing_hook"));
            EntityPredicate entityPredicate = fromJson(jsonObject.get("vehicle"));
            EntityPredicate entityPredicate2 = fromJson(jsonObject.get("passenger"));
            EntityPredicate entityPredicate3 = fromJson(jsonObject.get("targeted_entity"));
            LighthingBoltPredicate lighthingBoltPredicate = LighthingBoltPredicate.fromJson(jsonObject.get("lightning_bolt"));
            String string = GsonHelper.getAsString(jsonObject, "team", (String)null);
            ResourceLocation resourceLocation = jsonObject.has("catType") ? new ResourceLocation(GsonHelper.getAsString(jsonObject, "catType")) : null;
            return (new EntityPredicate.Builder()).entityType(entityTypePredicate).distance(distancePredicate).located(locationPredicate).steppingOn(locationPredicate2).effects(mobEffectsPredicate).nbt(nbtPredicate).flags(entityFlagsPredicate).equipment(entityEquipmentPredicate).player(playerPredicate).fishingHook(fishingHookPredicate).lighthingBolt(lighthingBoltPredicate).team(string).vehicle(entityPredicate).passenger(entityPredicate2).targetedEntity(entityPredicate3).catType(resourceLocation).build();
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("type", this.entityType.serializeToJson());
            jsonObject.add("distance", this.distanceToPlayer.serializeToJson());
            jsonObject.add("location", this.location.serializeToJson());
            jsonObject.add("stepping_on", this.steppingOnLocation.serializeToJson());
            jsonObject.add("effects", this.effects.serializeToJson());
            jsonObject.add("nbt", this.nbt.serializeToJson());
            jsonObject.add("flags", this.flags.serializeToJson());
            jsonObject.add("equipment", this.equipment.serializeToJson());
            jsonObject.add("player", this.player.serializeToJson());
            jsonObject.add("fishing_hook", this.fishingHook.serializeToJson());
            jsonObject.add("lightning_bolt", this.lighthingBolt.serializeToJson());
            jsonObject.add("vehicle", this.vehicle.serializeToJson());
            jsonObject.add("passenger", this.passenger.serializeToJson());
            jsonObject.add("targeted_entity", this.targetedEntity.serializeToJson());
            jsonObject.addProperty("team", this.team);
            if (this.catType != null) {
                jsonObject.addProperty("catType", this.catType.toString());
            }

            return jsonObject;
        }
    }

    public static LootContext createContext(ServerPlayer player, Entity target) {
        return (new LootContext.Builder(player.getLevel())).withParameter(LootContextParams.THIS_ENTITY, target).withParameter(LootContextParams.ORIGIN, player.position()).withRandom(player.getRandom()).create(LootContextParamSets.ADVANCEMENT_ENTITY);
    }

    public static class Builder {
        private EntityTypePredicate entityType = EntityTypePredicate.ANY;
        private DistancePredicate distanceToPlayer = DistancePredicate.ANY;
        private LocationPredicate location = LocationPredicate.ANY;
        private LocationPredicate steppingOnLocation = LocationPredicate.ANY;
        private MobEffectsPredicate effects = MobEffectsPredicate.ANY;
        private NbtPredicate nbt = NbtPredicate.ANY;
        private EntityFlagsPredicate flags = EntityFlagsPredicate.ANY;
        private EntityEquipmentPredicate equipment = EntityEquipmentPredicate.ANY;
        private PlayerPredicate player = PlayerPredicate.ANY;
        private FishingHookPredicate fishingHook = FishingHookPredicate.ANY;
        private LighthingBoltPredicate lighthingBolt = LighthingBoltPredicate.ANY;
        private EntityPredicate vehicle = EntityPredicate.ANY;
        private EntityPredicate passenger = EntityPredicate.ANY;
        private EntityPredicate targetedEntity = EntityPredicate.ANY;
        @Nullable
        private String team;
        @Nullable
        private ResourceLocation catType;

        public static EntityPredicate.Builder entity() {
            return new EntityPredicate.Builder();
        }

        public EntityPredicate.Builder of(EntityType<?> type) {
            this.entityType = EntityTypePredicate.of(type);
            return this;
        }

        public EntityPredicate.Builder of(TagKey<EntityType<?>> tag) {
            this.entityType = EntityTypePredicate.of(tag);
            return this;
        }

        public EntityPredicate.Builder of(ResourceLocation catType) {
            this.catType = catType;
            return this;
        }

        public EntityPredicate.Builder entityType(EntityTypePredicate type) {
            this.entityType = type;
            return this;
        }

        public EntityPredicate.Builder distance(DistancePredicate distance) {
            this.distanceToPlayer = distance;
            return this;
        }

        public EntityPredicate.Builder located(LocationPredicate location) {
            this.location = location;
            return this;
        }

        public EntityPredicate.Builder steppingOn(LocationPredicate location) {
            this.steppingOnLocation = location;
            return this;
        }

        public EntityPredicate.Builder effects(MobEffectsPredicate effects) {
            this.effects = effects;
            return this;
        }

        public EntityPredicate.Builder nbt(NbtPredicate nbt) {
            this.nbt = nbt;
            return this;
        }

        public EntityPredicate.Builder flags(EntityFlagsPredicate flags) {
            this.flags = flags;
            return this;
        }

        public EntityPredicate.Builder equipment(EntityEquipmentPredicate equipment) {
            this.equipment = equipment;
            return this;
        }

        public EntityPredicate.Builder player(PlayerPredicate player) {
            this.player = player;
            return this;
        }

        public EntityPredicate.Builder fishingHook(FishingHookPredicate fishHook) {
            this.fishingHook = fishHook;
            return this;
        }

        public EntityPredicate.Builder lighthingBolt(LighthingBoltPredicate lightningBolt) {
            this.lighthingBolt = lightningBolt;
            return this;
        }

        public EntityPredicate.Builder vehicle(EntityPredicate vehicle) {
            this.vehicle = vehicle;
            return this;
        }

        public EntityPredicate.Builder passenger(EntityPredicate passenger) {
            this.passenger = passenger;
            return this;
        }

        public EntityPredicate.Builder targetedEntity(EntityPredicate targetedEntity) {
            this.targetedEntity = targetedEntity;
            return this;
        }

        public EntityPredicate.Builder team(@Nullable String team) {
            this.team = team;
            return this;
        }

        public EntityPredicate.Builder catType(@Nullable ResourceLocation catType) {
            this.catType = catType;
            return this;
        }

        public EntityPredicate build() {
            return new EntityPredicate(this.entityType, this.distanceToPlayer, this.location, this.steppingOnLocation, this.effects, this.nbt, this.flags, this.equipment, this.player, this.fishingHook, this.lighthingBolt, this.vehicle, this.passenger, this.targetedEntity, this.team, this.catType);
        }
    }

    public static class Composite {
        public static final EntityPredicate.Composite ANY = new EntityPredicate.Composite(new LootItemCondition[0]);
        private final LootItemCondition[] conditions;
        private final Predicate<LootContext> compositePredicates;

        private Composite(LootItemCondition[] conditions) {
            this.conditions = conditions;
            this.compositePredicates = LootItemConditions.andConditions(conditions);
        }

        public static EntityPredicate.Composite create(LootItemCondition... conditions) {
            return new EntityPredicate.Composite(conditions);
        }

        public static EntityPredicate.Composite fromJson(JsonObject root, String key, DeserializationContext predicateDeserializer) {
            JsonElement jsonElement = root.get(key);
            return fromElement(key, predicateDeserializer, jsonElement);
        }

        public static EntityPredicate.Composite[] fromJsonArray(JsonObject root, String key, DeserializationContext predicateDeserializer) {
            JsonElement jsonElement = root.get(key);
            if (jsonElement != null && !jsonElement.isJsonNull()) {
                JsonArray jsonArray = GsonHelper.convertToJsonArray(jsonElement, key);
                EntityPredicate.Composite[] composites = new EntityPredicate.Composite[jsonArray.size()];

                for(int i = 0; i < jsonArray.size(); ++i) {
                    composites[i] = fromElement(key + "[" + i + "]", predicateDeserializer, jsonArray.get(i));
                }

                return composites;
            } else {
                return new EntityPredicate.Composite[0];
            }
        }

        private static EntityPredicate.Composite fromElement(String key, DeserializationContext predicateDeserializer, @Nullable JsonElement json) {
            if (json != null && json.isJsonArray()) {
                LootItemCondition[] lootItemConditions = predicateDeserializer.deserializeConditions(json.getAsJsonArray(), predicateDeserializer.getAdvancementId() + "/" + key, LootContextParamSets.ADVANCEMENT_ENTITY);
                return new EntityPredicate.Composite(lootItemConditions);
            } else {
                EntityPredicate entityPredicate = EntityPredicate.fromJson(json);
                return wrap(entityPredicate);
            }
        }

        public static EntityPredicate.Composite wrap(EntityPredicate predicate) {
            if (predicate == EntityPredicate.ANY) {
                return ANY;
            } else {
                LootItemCondition lootItemCondition = LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, predicate).build();
                return new EntityPredicate.Composite(new LootItemCondition[]{lootItemCondition});
            }
        }

        public boolean matches(LootContext context) {
            return this.compositePredicates.test(context);
        }

        public JsonElement toJson(SerializationContext predicateSerializer) {
            return (JsonElement)(this.conditions.length == 0 ? JsonNull.INSTANCE : predicateSerializer.serializeConditions(this.conditions));
        }

        public static JsonElement toJson(EntityPredicate.Composite[] predicates, SerializationContext predicateSerializer) {
            if (predicates.length == 0) {
                return JsonNull.INSTANCE;
            } else {
                JsonArray jsonArray = new JsonArray();

                for(EntityPredicate.Composite composite : predicates) {
                    jsonArray.add(composite.toJson(predicateSerializer));
                }

                return jsonArray;
            }
        }
    }
}
