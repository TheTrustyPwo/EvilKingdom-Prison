package net.minecraft.world.entity.ai.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MoveControl implements Control {
    public static final float MIN_SPEED = 5.0E-4F;
    public static final float MIN_SPEED_SQR = 2.5000003E-7F;
    protected static final int MAX_TURN = 90;
    protected final Mob mob;
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;
    protected double speedModifier;
    protected float strafeForwards;
    protected float strafeRight;
    protected MoveControl.Operation operation = MoveControl.Operation.WAIT;

    public MoveControl(Mob entity) {
        this.mob = entity;
    }

    public boolean hasWanted() {
        return this.operation == MoveControl.Operation.MOVE_TO;
    }

    public double getSpeedModifier() {
        return this.speedModifier;
    }

    public void setWantedPosition(double x, double y, double z, double speed) {
        this.wantedX = x;
        this.wantedY = y;
        this.wantedZ = z;
        this.speedModifier = speed;
        if (this.operation != MoveControl.Operation.JUMPING) {
            this.operation = MoveControl.Operation.MOVE_TO;
        }

    }

    public void strafe(float forward, float sideways) {
        this.operation = MoveControl.Operation.STRAFE;
        this.strafeForwards = forward;
        this.strafeRight = sideways;
        this.speedModifier = 0.25D;
    }

    public void tick() {
        if (this.operation == MoveControl.Operation.STRAFE) {
            float f = (float)this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            float g = (float)this.speedModifier * f;
            float h = this.strafeForwards;
            float i = this.strafeRight;
            float j = Mth.sqrt(h * h + i * i);
            if (j < 1.0F) {
                j = 1.0F;
            }

            j = g / j;
            h *= j;
            i *= j;
            float k = Mth.sin(this.mob.getYRot() * ((float)Math.PI / 180F));
            float l = Mth.cos(this.mob.getYRot() * ((float)Math.PI / 180F));
            float m = h * l - i * k;
            float n = i * l + h * k;
            if (!this.isWalkable(m, n)) {
                this.strafeForwards = 1.0F;
                this.strafeRight = 0.0F;
            }

            this.mob.setSpeed(g);
            this.mob.setZza(this.strafeForwards);
            this.mob.setXxa(this.strafeRight);
            this.operation = MoveControl.Operation.WAIT;
        } else if (this.operation == MoveControl.Operation.MOVE_TO) {
            this.operation = MoveControl.Operation.WAIT;
            double d = this.wantedX - this.mob.getX();
            double e = this.wantedZ - this.mob.getZ();
            double o = this.wantedY - this.mob.getY();
            double p = d * d + o * o + e * e;
            if (p < (double)2.5000003E-7F) {
                this.mob.setZza(0.0F);
                return;
            }

            float q = (float)(Mth.atan2(e, d) * (double)(180F / (float)Math.PI)) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), q, 90.0F));
            this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            BlockPos blockPos = this.mob.blockPosition();
            BlockState blockState = this.mob.level.getBlockState(blockPos);
            VoxelShape voxelShape = blockState.getCollisionShape(this.mob.level, blockPos);
            if (o > (double)this.mob.maxUpStep && d * d + e * e < (double)Math.max(1.0F, this.mob.getBbWidth()) || !voxelShape.isEmpty() && this.mob.getY() < voxelShape.max(Direction.Axis.Y) + (double)blockPos.getY() && !blockState.is(BlockTags.DOORS) && !blockState.is(BlockTags.FENCES)) {
                this.mob.getJumpControl().jump();
                this.operation = MoveControl.Operation.JUMPING;
            }
        } else if (this.operation == MoveControl.Operation.JUMPING) {
            this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            if (this.mob.isOnGround()) {
                this.operation = MoveControl.Operation.WAIT;
            }
        } else {
            this.mob.setZza(0.0F);
        }

    }

    private boolean isWalkable(float f, float g) {
        PathNavigation pathNavigation = this.mob.getNavigation();
        if (pathNavigation != null) {
            NodeEvaluator nodeEvaluator = pathNavigation.getNodeEvaluator();
            if (nodeEvaluator != null && nodeEvaluator.getBlockPathType(this.mob.level, Mth.floor(this.mob.getX() + (double)f), this.mob.getBlockY(), Mth.floor(this.mob.getZ() + (double)g)) != BlockPathTypes.WALKABLE) {
                return false;
            }
        }

        return true;
    }

    protected float rotlerp(float from, float to, float max) {
        float f = Mth.wrapDegrees(to - from);
        if (f > max) {
            f = max;
        }

        if (f < -max) {
            f = -max;
        }

        float g = from + f;
        if (g < 0.0F) {
            g += 360.0F;
        } else if (g > 360.0F) {
            g -= 360.0F;
        }

        return g;
    }

    public double getWantedX() {
        return this.wantedX;
    }

    public double getWantedY() {
        return this.wantedY;
    }

    public double getWantedZ() {
        return this.wantedZ;
    }

    public static enum Operation {
        WAIT,
        MOVE_TO,
        STRAFE,
        JUMPING;
    }
}
