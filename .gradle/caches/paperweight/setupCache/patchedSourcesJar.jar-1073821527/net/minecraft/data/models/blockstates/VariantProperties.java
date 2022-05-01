package net.minecraft.data.models.blockstates;

import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

public class VariantProperties {
    public static final VariantProperty<VariantProperties.Rotation> X_ROT = new VariantProperty<>("x", (rotation) -> {
        return new JsonPrimitive(rotation.value);
    });
    public static final VariantProperty<VariantProperties.Rotation> Y_ROT = new VariantProperty<>("y", (rotation) -> {
        return new JsonPrimitive(rotation.value);
    });
    public static final VariantProperty<ResourceLocation> MODEL = new VariantProperty<>("model", (id) -> {
        return new JsonPrimitive(id.toString());
    });
    public static final VariantProperty<Boolean> UV_LOCK = new VariantProperty<>("uvlock", JsonPrimitive::new);
    public static final VariantProperty<Integer> WEIGHT = new VariantProperty<>("weight", JsonPrimitive::new);

    public static enum Rotation {
        R0(0),
        R90(90),
        R180(180),
        R270(270);

        final int value;

        private Rotation(int degrees) {
            this.value = degrees;
        }
    }
}
