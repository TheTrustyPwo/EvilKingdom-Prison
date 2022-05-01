package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.TranslatableComponent;

public class WorldCoordinate {
    private static final char PREFIX_RELATIVE = '~';
    public static final SimpleCommandExceptionType ERROR_EXPECTED_DOUBLE = new SimpleCommandExceptionType(new TranslatableComponent("argument.pos.missing.double"));
    public static final SimpleCommandExceptionType ERROR_EXPECTED_INT = new SimpleCommandExceptionType(new TranslatableComponent("argument.pos.missing.int"));
    private final boolean relative;
    private final double value;

    public WorldCoordinate(boolean relative, double value) {
        this.relative = relative;
        this.value = value;
    }

    public double get(double offset) {
        return this.relative ? this.value + offset : this.value;
    }

    public static WorldCoordinate parseDouble(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '^') {
            throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(reader);
        } else if (!reader.canRead()) {
            throw ERROR_EXPECTED_DOUBLE.createWithContext(reader);
        } else {
            boolean bl = isRelative(reader);
            int i = reader.getCursor();
            double d = reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0D;
            String string = reader.getString().substring(i, reader.getCursor());
            if (bl && string.isEmpty()) {
                return new WorldCoordinate(true, 0.0D);
            } else {
                if (!string.contains(".") && !bl && centerIntegers) {
                    d += 0.5D;
                }

                return new WorldCoordinate(bl, d);
            }
        }
    }

    public static WorldCoordinate parseInt(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '^') {
            throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(reader);
        } else if (!reader.canRead()) {
            throw ERROR_EXPECTED_INT.createWithContext(reader);
        } else {
            boolean bl = isRelative(reader);
            double d;
            if (reader.canRead() && reader.peek() != ' ') {
                d = bl ? reader.readDouble() : (double)reader.readInt();
            } else {
                d = 0.0D;
            }

            return new WorldCoordinate(bl, d);
        }
    }

    public static boolean isRelative(StringReader reader) {
        boolean bl;
        if (reader.peek() == '~') {
            bl = true;
            reader.skip();
        } else {
            bl = false;
        }

        return bl;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof WorldCoordinate)) {
            return false;
        } else {
            WorldCoordinate worldCoordinate = (WorldCoordinate)object;
            if (this.relative != worldCoordinate.relative) {
                return false;
            } else {
                return Double.compare(worldCoordinate.value, this.value) == 0;
            }
        }
    }

    @Override
    public int hashCode() {
        int i = this.relative ? 1 : 0;
        long l = Double.doubleToLongBits(this.value);
        return 31 * i + (int)(l ^ l >>> 32);
    }

    public boolean isRelative() {
        return this.relative;
    }
}
