package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.RandomSource;

public enum RandomSpreadType implements StringRepresentable {
    LINEAR("linear"),
    TRIANGULAR("triangular");

    private static final RandomSpreadType[] VALUES = values();
    public static final Codec<RandomSpreadType> CODEC = StringRepresentable.fromEnum(() -> {
        return VALUES;
    }, RandomSpreadType::byName);
    private final String id;

    private RandomSpreadType(String name) {
        this.id = name;
    }

    public static RandomSpreadType byName(String name) {
        for(RandomSpreadType randomSpreadType : VALUES) {
            if (randomSpreadType.getSerializedName().equals(name)) {
                return randomSpreadType;
            }
        }

        throw new IllegalArgumentException("Unknown Random Spread type: " + name);
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public int evaluate(RandomSource random, int bound) {
        int var10000;
        switch(this) {
        case LINEAR:
            var10000 = random.nextInt(bound);
            break;
        case TRIANGULAR:
            var10000 = (random.nextInt(bound) + random.nextInt(bound)) / 2;
            break;
        default:
            throw new IncompatibleClassChangeError();
        }

        return var10000;
    }
}
