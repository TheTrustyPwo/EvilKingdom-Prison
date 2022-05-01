package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class LocalCoordinates implements Coordinates {
    public static final char PREFIX_LOCAL_COORDINATE = '^';
    private final double left;
    private final double up;
    private final double forwards;

    public LocalCoordinates(double x, double y, double z) {
        this.left = x;
        this.up = y;
        this.forwards = z;
    }

    @Override
    public Vec3 getPosition(CommandSourceStack source) {
        Vec2 vec2 = source.getRotation();
        Vec3 vec3 = source.getAnchor().apply(source);
        float f = Mth.cos((vec2.y + 90.0F) * ((float)Math.PI / 180F));
        float g = Mth.sin((vec2.y + 90.0F) * ((float)Math.PI / 180F));
        float h = Mth.cos(-vec2.x * ((float)Math.PI / 180F));
        float i = Mth.sin(-vec2.x * ((float)Math.PI / 180F));
        float j = Mth.cos((-vec2.x + 90.0F) * ((float)Math.PI / 180F));
        float k = Mth.sin((-vec2.x + 90.0F) * ((float)Math.PI / 180F));
        Vec3 vec32 = new Vec3((double)(f * h), (double)i, (double)(g * h));
        Vec3 vec33 = new Vec3((double)(f * j), (double)k, (double)(g * j));
        Vec3 vec34 = vec32.cross(vec33).scale(-1.0D);
        double d = vec32.x * this.forwards + vec33.x * this.up + vec34.x * this.left;
        double e = vec32.y * this.forwards + vec33.y * this.up + vec34.y * this.left;
        double l = vec32.z * this.forwards + vec33.z * this.up + vec34.z * this.left;
        return new Vec3(vec3.x + d, vec3.y + e, vec3.z + l);
    }

    @Override
    public Vec2 getRotation(CommandSourceStack source) {
        return Vec2.ZERO;
    }

    @Override
    public boolean isXRelative() {
        return true;
    }

    @Override
    public boolean isYRelative() {
        return true;
    }

    @Override
    public boolean isZRelative() {
        return true;
    }

    public static LocalCoordinates parse(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        double d = readDouble(reader, i);
        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            double e = readDouble(reader, i);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                double f = readDouble(reader, i);
                return new LocalCoordinates(d, e, f);
            } else {
                reader.setCursor(i);
                throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
            }
        } else {
            reader.setCursor(i);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
        }
    }

    private static double readDouble(StringReader reader, int startingCursorPos) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(reader);
        } else if (reader.peek() != '^') {
            reader.setCursor(startingCursorPos);
            throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(reader);
        } else {
            reader.skip();
            return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0D;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof LocalCoordinates)) {
            return false;
        } else {
            LocalCoordinates localCoordinates = (LocalCoordinates)object;
            return this.left == localCoordinates.left && this.up == localCoordinates.up && this.forwards == localCoordinates.forwards;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.left, this.up, this.forwards);
    }
}
