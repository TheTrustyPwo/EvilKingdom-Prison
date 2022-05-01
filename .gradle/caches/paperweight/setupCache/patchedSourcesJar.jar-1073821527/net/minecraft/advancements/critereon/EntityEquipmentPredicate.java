package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Items;

public class EntityEquipmentPredicate {
    public static final EntityEquipmentPredicate ANY = new EntityEquipmentPredicate(ItemPredicate.ANY, ItemPredicate.ANY, ItemPredicate.ANY, ItemPredicate.ANY, ItemPredicate.ANY, ItemPredicate.ANY);
    public static final EntityEquipmentPredicate CAPTAIN = new EntityEquipmentPredicate(ItemPredicate.Builder.item().of(Items.WHITE_BANNER).hasNbt(Raid.getLeaderBannerInstance().getTag()).build(), ItemPredicate.ANY, ItemPredicate.ANY, ItemPredicate.ANY, ItemPredicate.ANY, ItemPredicate.ANY);
    private final ItemPredicate head;
    private final ItemPredicate chest;
    private final ItemPredicate legs;
    private final ItemPredicate feet;
    private final ItemPredicate mainhand;
    private final ItemPredicate offhand;

    public EntityEquipmentPredicate(ItemPredicate head, ItemPredicate chest, ItemPredicate legs, ItemPredicate feet, ItemPredicate mainhand, ItemPredicate offhand) {
        this.head = head;
        this.chest = chest;
        this.legs = legs;
        this.feet = feet;
        this.mainhand = mainhand;
        this.offhand = offhand;
    }

    public boolean matches(@Nullable Entity entity) {
        if (this == ANY) {
            return true;
        } else if (!(entity instanceof LivingEntity)) {
            return false;
        } else {
            LivingEntity livingEntity = (LivingEntity)entity;
            if (!this.head.matches(livingEntity.getItemBySlot(EquipmentSlot.HEAD))) {
                return false;
            } else if (!this.chest.matches(livingEntity.getItemBySlot(EquipmentSlot.CHEST))) {
                return false;
            } else if (!this.legs.matches(livingEntity.getItemBySlot(EquipmentSlot.LEGS))) {
                return false;
            } else if (!this.feet.matches(livingEntity.getItemBySlot(EquipmentSlot.FEET))) {
                return false;
            } else if (!this.mainhand.matches(livingEntity.getItemBySlot(EquipmentSlot.MAINHAND))) {
                return false;
            } else {
                return this.offhand.matches(livingEntity.getItemBySlot(EquipmentSlot.OFFHAND));
            }
        }
    }

    public static EntityEquipmentPredicate fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "equipment");
            ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("head"));
            ItemPredicate itemPredicate2 = ItemPredicate.fromJson(jsonObject.get("chest"));
            ItemPredicate itemPredicate3 = ItemPredicate.fromJson(jsonObject.get("legs"));
            ItemPredicate itemPredicate4 = ItemPredicate.fromJson(jsonObject.get("feet"));
            ItemPredicate itemPredicate5 = ItemPredicate.fromJson(jsonObject.get("mainhand"));
            ItemPredicate itemPredicate6 = ItemPredicate.fromJson(jsonObject.get("offhand"));
            return new EntityEquipmentPredicate(itemPredicate, itemPredicate2, itemPredicate3, itemPredicate4, itemPredicate5, itemPredicate6);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("head", this.head.serializeToJson());
            jsonObject.add("chest", this.chest.serializeToJson());
            jsonObject.add("legs", this.legs.serializeToJson());
            jsonObject.add("feet", this.feet.serializeToJson());
            jsonObject.add("mainhand", this.mainhand.serializeToJson());
            jsonObject.add("offhand", this.offhand.serializeToJson());
            return jsonObject;
        }
    }

    public static class Builder {
        private ItemPredicate head = ItemPredicate.ANY;
        private ItemPredicate chest = ItemPredicate.ANY;
        private ItemPredicate legs = ItemPredicate.ANY;
        private ItemPredicate feet = ItemPredicate.ANY;
        private ItemPredicate mainhand = ItemPredicate.ANY;
        private ItemPredicate offhand = ItemPredicate.ANY;

        public static EntityEquipmentPredicate.Builder equipment() {
            return new EntityEquipmentPredicate.Builder();
        }

        public EntityEquipmentPredicate.Builder head(ItemPredicate head) {
            this.head = head;
            return this;
        }

        public EntityEquipmentPredicate.Builder chest(ItemPredicate chest) {
            this.chest = chest;
            return this;
        }

        public EntityEquipmentPredicate.Builder legs(ItemPredicate legs) {
            this.legs = legs;
            return this;
        }

        public EntityEquipmentPredicate.Builder feet(ItemPredicate feet) {
            this.feet = feet;
            return this;
        }

        public EntityEquipmentPredicate.Builder mainhand(ItemPredicate mainhand) {
            this.mainhand = mainhand;
            return this;
        }

        public EntityEquipmentPredicate.Builder offhand(ItemPredicate offhand) {
            this.offhand = offhand;
            return this;
        }

        public EntityEquipmentPredicate build() {
            return new EntityEquipmentPredicate(this.head, this.chest, this.legs, this.feet, this.mainhand, this.offhand);
        }
    }
}
