package net.minecraft.world.level.block.piston;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class PistonMath {
    public static AABB getMovementArea(AABB box, Direction direction, double length) {
        double d = length * (double)direction.getAxisDirection().getStep();
        double e = Math.min(d, 0.0D);
        double f = Math.max(d, 0.0D);
        switch(direction) {
        case WEST:
            return new AABB(box.minX + e, box.minY, box.minZ, box.minX + f, box.maxY, box.maxZ);
        case EAST:
            return new AABB(box.maxX + e, box.minY, box.minZ, box.maxX + f, box.maxY, box.maxZ);
        case DOWN:
            return new AABB(box.minX, box.minY + e, box.minZ, box.maxX, box.minY + f, box.maxZ);
        case UP:
        default:
            return new AABB(box.minX, box.maxY + e, box.minZ, box.maxX, box.maxY + f, box.maxZ);
        case NORTH:
            return new AABB(box.minX, box.minY, box.minZ + e, box.maxX, box.maxY, box.minZ + f);
        case SOUTH:
            return new AABB(box.minX, box.minY, box.maxZ + e, box.maxX, box.maxY, box.maxZ + f);
        }
    }
}
