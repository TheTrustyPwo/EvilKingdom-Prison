package net.minecraft.commands.arguments.coordinates;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public interface Coordinates {
    Vec3 getPosition(CommandSourceStack source);

    Vec2 getRotation(CommandSourceStack source);

    default BlockPos getBlockPos(CommandSourceStack source) {
        return new BlockPos(this.getPosition(source));
    }

    boolean isXRelative();

    boolean isYRelative();

    boolean isZRelative();
}
