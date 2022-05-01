package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.phys.Vec3;

public class LlamaFollowCaravanGoal extends Goal {
    public final Llama llama;
    private double speedModifier;
    private static final int CARAVAN_LIMIT = 8;
    private int distCheckCounter;

    public LlamaFollowCaravanGoal(Llama llama, double speed) {
        this.llama = llama;
        this.speedModifier = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.llama.isLeashed() && !this.llama.inCaravan()) {
            List<Entity> list = this.llama.level.getEntities(this.llama, this.llama.getBoundingBox().inflate(9.0D, 4.0D, 9.0D), (entity) -> {
                EntityType<?> entityType = entity.getType();
                return entityType == EntityType.LLAMA || entityType == EntityType.TRADER_LLAMA;
            });
            Llama llama = null;
            double d = Double.MAX_VALUE;

            for(Entity entity : list) {
                Llama llama2 = (Llama)entity;
                if (llama2.inCaravan() && !llama2.hasCaravanTail()) {
                    double e = this.llama.distanceToSqr(llama2);
                    if (!(e > d)) {
                        d = e;
                        llama = llama2;
                    }
                }
            }

            if (llama == null) {
                for(Entity entity2 : list) {
                    Llama llama3 = (Llama)entity2;
                    if (llama3.isLeashed() && !llama3.hasCaravanTail()) {
                        double f = this.llama.distanceToSqr(llama3);
                        if (!(f > d)) {
                            d = f;
                            llama = llama3;
                        }
                    }
                }
            }

            if (llama == null) {
                return false;
            } else if (d < 4.0D) {
                return false;
            } else if (!llama.isLeashed() && !this.firstIsLeashed(llama, 1)) {
                return false;
            } else {
                this.llama.joinCaravan(llama);
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.llama.inCaravan() && this.llama.getCaravanHead().isAlive() && this.firstIsLeashed(this.llama, 0)) {
            double d = this.llama.distanceToSqr(this.llama.getCaravanHead());
            if (d > 676.0D) {
                if (this.speedModifier <= 3.0D) {
                    this.speedModifier *= 1.2D;
                    this.distCheckCounter = reducedTickDelay(40);
                    return true;
                }

                if (this.distCheckCounter == 0) {
                    return false;
                }
            }

            if (this.distCheckCounter > 0) {
                --this.distCheckCounter;
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void stop() {
        this.llama.leaveCaravan();
        this.speedModifier = 2.1D;
    }

    @Override
    public void tick() {
        if (this.llama.inCaravan()) {
            if (!(this.llama.getLeashHolder() instanceof LeashFenceKnotEntity)) {
                Llama llama = this.llama.getCaravanHead();
                double d = (double)this.llama.distanceTo(llama);
                float f = 2.0F;
                Vec3 vec3 = (new Vec3(llama.getX() - this.llama.getX(), llama.getY() - this.llama.getY(), llama.getZ() - this.llama.getZ())).normalize().scale(Math.max(d - 2.0D, 0.0D));
                this.llama.getNavigation().moveTo(this.llama.getX() + vec3.x, this.llama.getY() + vec3.y, this.llama.getZ() + vec3.z, this.speedModifier);
            }
        }
    }

    private boolean firstIsLeashed(Llama llama, int length) {
        if (length > 8) {
            return false;
        } else if (llama.inCaravan()) {
            if (llama.getCaravanHead().isLeashed()) {
                return true;
            } else {
                Llama var10001 = llama.getCaravanHead();
                ++length;
                return this.firstIsLeashed(var10001, length);
            }
        } else {
            return false;
        }
    }
}
