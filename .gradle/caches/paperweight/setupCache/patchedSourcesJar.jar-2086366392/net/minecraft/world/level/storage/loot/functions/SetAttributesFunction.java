package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class SetAttributesFunction extends LootItemConditionalFunction {
    final List<SetAttributesFunction.Modifier> modifiers;

    SetAttributesFunction(LootItemCondition[] conditions, List<SetAttributesFunction.Modifier> attributes) {
        super(conditions);
        this.modifiers = ImmutableList.copyOf(attributes);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_ATTRIBUTES;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.modifiers.stream().flatMap((attribute) -> {
            return attribute.amount.getReferencedContextParams().stream();
        }).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        Random random = context.getRandom();

        for(SetAttributesFunction.Modifier modifier : this.modifiers) {
            UUID uUID = modifier.id;
            if (uUID == null) {
                uUID = UUID.randomUUID();
            }

            EquipmentSlot equipmentSlot = Util.getRandom(modifier.slots, random);
            stack.addAttributeModifier(modifier.attribute, new AttributeModifier(uUID, modifier.name, (double)modifier.amount.getFloat(context), modifier.operation), equipmentSlot);
        }

        return stack;
    }

    public static SetAttributesFunction.ModifierBuilder modifier(String name, Attribute attribute, AttributeModifier.Operation operation, NumberProvider amountRange) {
        return new SetAttributesFunction.ModifierBuilder(name, attribute, operation, amountRange);
    }

    public static SetAttributesFunction.Builder setAttributes() {
        return new SetAttributesFunction.Builder();
    }

    public static class Builder extends LootItemConditionalFunction.Builder<SetAttributesFunction.Builder> {
        private final List<SetAttributesFunction.Modifier> modifiers = Lists.newArrayList();

        @Override
        protected SetAttributesFunction.Builder getThis() {
            return this;
        }

        public SetAttributesFunction.Builder withModifier(SetAttributesFunction.ModifierBuilder attribute) {
            this.modifiers.add(attribute.build());
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetAttributesFunction(this.getConditions(), this.modifiers);
        }
    }

    static class Modifier {
        final String name;
        final Attribute attribute;
        final AttributeModifier.Operation operation;
        final NumberProvider amount;
        @Nullable
        final UUID id;
        final EquipmentSlot[] slots;

        Modifier(String name, Attribute attribute, AttributeModifier.Operation operation, NumberProvider amount, EquipmentSlot[] slots, @Nullable UUID id) {
            this.name = name;
            this.attribute = attribute;
            this.operation = operation;
            this.amount = amount;
            this.id = id;
            this.slots = slots;
        }

        public JsonObject serialize(JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", this.name);
            jsonObject.addProperty("attribute", Registry.ATTRIBUTE.getKey(this.attribute).toString());
            jsonObject.addProperty("operation", operationToString(this.operation));
            jsonObject.add("amount", context.serialize(this.amount));
            if (this.id != null) {
                jsonObject.addProperty("id", this.id.toString());
            }

            if (this.slots.length == 1) {
                jsonObject.addProperty("slot", this.slots[0].getName());
            } else {
                JsonArray jsonArray = new JsonArray();

                for(EquipmentSlot equipmentSlot : this.slots) {
                    jsonArray.add(new JsonPrimitive(equipmentSlot.getName()));
                }

                jsonObject.add("slot", jsonArray);
            }

            return jsonObject;
        }

        public static SetAttributesFunction.Modifier deserialize(JsonObject json, JsonDeserializationContext context) {
            String string = GsonHelper.getAsString(json, "name");
            ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(json, "attribute"));
            Attribute attribute = Registry.ATTRIBUTE.get(resourceLocation);
            if (attribute == null) {
                throw new JsonSyntaxException("Unknown attribute: " + resourceLocation);
            } else {
                AttributeModifier.Operation operation = operationFromString(GsonHelper.getAsString(json, "operation"));
                NumberProvider numberProvider = GsonHelper.getAsObject(json, "amount", context, NumberProvider.class);
                UUID uUID = null;
                EquipmentSlot[] equipmentSlots;
                if (GsonHelper.isStringValue(json, "slot")) {
                    equipmentSlots = new EquipmentSlot[]{EquipmentSlot.byName(GsonHelper.getAsString(json, "slot"))};
                } else {
                    if (!GsonHelper.isArrayNode(json, "slot")) {
                        throw new JsonSyntaxException("Invalid or missing attribute modifier slot; must be either string or array of strings.");
                    }

                    JsonArray jsonArray = GsonHelper.getAsJsonArray(json, "slot");
                    equipmentSlots = new EquipmentSlot[jsonArray.size()];
                    int i = 0;

                    for(JsonElement jsonElement : jsonArray) {
                        equipmentSlots[i++] = EquipmentSlot.byName(GsonHelper.convertToString(jsonElement, "slot"));
                    }

                    if (equipmentSlots.length == 0) {
                        throw new JsonSyntaxException("Invalid attribute modifier slot; must contain at least one entry.");
                    }
                }

                if (json.has("id")) {
                    String string2 = GsonHelper.getAsString(json, "id");

                    try {
                        uUID = UUID.fromString(string2);
                    } catch (IllegalArgumentException var13) {
                        throw new JsonSyntaxException("Invalid attribute modifier id '" + string2 + "' (must be UUID format, with dashes)");
                    }
                }

                return new SetAttributesFunction.Modifier(string, attribute, operation, numberProvider, equipmentSlots, uUID);
            }
        }

        private static String operationToString(AttributeModifier.Operation operation) {
            switch(operation) {
            case ADDITION:
                return "addition";
            case MULTIPLY_BASE:
                return "multiply_base";
            case MULTIPLY_TOTAL:
                return "multiply_total";
            default:
                throw new IllegalArgumentException("Unknown operation " + operation);
            }
        }

        private static AttributeModifier.Operation operationFromString(String name) {
            switch(name) {
            case "addition":
                return AttributeModifier.Operation.ADDITION;
            case "multiply_base":
                return AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total":
                return AttributeModifier.Operation.MULTIPLY_TOTAL;
            default:
                throw new JsonSyntaxException("Unknown attribute modifier operation " + name);
            }
        }
    }

    public static class ModifierBuilder {
        private final String name;
        private final Attribute attribute;
        private final AttributeModifier.Operation operation;
        private final NumberProvider amount;
        @Nullable
        private UUID id;
        private final Set<EquipmentSlot> slots = EnumSet.noneOf(EquipmentSlot.class);

        public ModifierBuilder(String name, Attribute attribute, AttributeModifier.Operation operation, NumberProvider amount) {
            this.name = name;
            this.attribute = attribute;
            this.operation = operation;
            this.amount = amount;
        }

        public SetAttributesFunction.ModifierBuilder forSlot(EquipmentSlot slot) {
            this.slots.add(slot);
            return this;
        }

        public SetAttributesFunction.ModifierBuilder withUuid(UUID uuid) {
            this.id = uuid;
            return this;
        }

        public SetAttributesFunction.Modifier build() {
            return new SetAttributesFunction.Modifier(this.name, this.attribute, this.operation, this.amount, this.slots.toArray(new EquipmentSlot[0]), this.id);
        }
    }

    public static class Serializer extends LootItemConditionalFunction.Serializer<SetAttributesFunction> {
        @Override
        public void serialize(JsonObject json, SetAttributesFunction object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            JsonArray jsonArray = new JsonArray();

            for(SetAttributesFunction.Modifier modifier : object.modifiers) {
                jsonArray.add(modifier.serialize(context));
            }

            json.add("modifiers", jsonArray);
        }

        @Override
        public SetAttributesFunction deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "modifiers");
            List<SetAttributesFunction.Modifier> list = Lists.newArrayListWithExpectedSize(jsonArray.size());

            for(JsonElement jsonElement : jsonArray) {
                list.add(SetAttributesFunction.Modifier.deserialize(GsonHelper.convertToJsonObject(jsonElement, "modifier"), jsonDeserializationContext));
            }

            if (list.isEmpty()) {
                throw new JsonSyntaxException("Invalid attribute modifiers array; cannot be empty");
            } else {
                return new SetAttributesFunction(lootItemConditions, list);
            }
        }
    }
}
