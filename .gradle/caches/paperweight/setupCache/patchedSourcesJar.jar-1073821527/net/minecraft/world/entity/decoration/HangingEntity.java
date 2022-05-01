package net.minecraft.world.entity.decoration;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

public abstract class HangingEntity extends Entity {
    protected static final Predicate<Entity> HANGING_ENTITY = (entity) -> {
        return entity instanceof HangingEntity;
    };
    private int checkInterval;
    public BlockPos pos;
    protected Direction direction = Direction.SOUTH;

    protected HangingEntity(EntityType<? extends HangingEntity> type, Level world) {
        super(type, world);
    }

    protected HangingEntity(EntityType<? extends HangingEntity> type, Level world, BlockPos pos) {
        this(type, world);
        this.pos = pos;
    }

    @Override
    protected void defineSynchedData() {
    }

    public void setDirection(Direction facing) {
        Validate.notNull(facing);
        Validate.isTrue(facing.getAxis().isHorizontal());
        this.direction = facing;
        this.setYRot((float)(this.direction.get2DDataValue() * 90));
        this.yRotO = this.getYRot();
        this.recalculateBoundingBox();
    }

    protected void recalculateBoundingBox() {
        if (this.direction != null) {
            double d = (double)this.pos.getX() + 0.5D;
            double e = (double)this.pos.getY() + 0.5D;
            double f = (double)this.pos.getZ() + 0.5D;
            double g = 0.46875D;
            double h = this.offs(this.getWidth());
            double i = this.offs(this.getHeight());
            d -= (double)this.direction.getStepX() * 0.46875D;
            f -= (double)this.direction.getStepZ() * 0.46875D;
            e += i;
            Direction direction = this.direction.getCounterClockWise();
            d += h * (double)direction.getStepX();
            f += h * (double)direction.getStepZ();
            this.setPosRaw(d, e, f);
            double j = (double)this.getWidth();
            double k = (double)this.getHeight();
            double l = (double)this.getWidth();
            if (this.direction.getAxis() == Direction.Axis.Z) {
                l = 1.0D;
            } else {
                j = 1.0D;
            }

            j /= 32.0D;
            k /= 32.0D;
            l /= 32.0D;
            this.setBoundingBox(new AABB(d - j, e - k, f - l, d + j, e + k, f + l));
        }
    }

    private double offs(int i) {
        return i % 32 == 0 ? 0.5D : 0.0D;
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide) {
            this.checkOutOfWorld();
            if (this.checkInterval++ == 100) {
                this.checkInterval = 0;
                if (!this.isRemoved() && !this.survives()) {
                    this.discard();
                    this.dropItem((Entity)null);
                }
            }
        }

    }

    public boolean survives() {
        if (!this.level.noCollision(this)) {
            return false;
        } else {
            int i = Math.max(1, this.getWidth() / 16);
            int j = Math.max(1, this.getHeight() / 16);
            BlockPos blockPos = this.pos.relative(this.direction.getOpposite());
            Direction direction = this.direction.getCounterClockWise();
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for(int k = 0; k < i; ++k) {
                for(int l = 0; l < j; ++l) {
                    int m = (i - 1) / -2;
                    int n = (j - 1) / -2;
                    mutableBlockPos.set(blockPos).move(direction, k + m).move(Direction.UP, l + n);
                    BlockState blockState = this.level.getBlockState(mutableBlockPos);
                    if (!blockState.getMaterial().isSolid() && !DiodeBlock.isDiode(blockState)) {
                        return false;
                    }
                }
            }

            return this.level.getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player) {
            Player player = (Player)attacker;
            return !this.level.mayInteract(player, this.pos) ? true : this.hurt(DamageSource.playerAttack(player), 0.0F);
        } else {
            return false;
        }
    }

    @Override
    public Direction getDirection() {
        return this.direction;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            if (!this.isRemoved() && !this.level.isClientSide) {
                this.kill();
                this.markHurt();
                this.dropItem(source.getEntity());
            }

            return true;
        }
    }

    @Override
    public void move(MoverType movementType, Vec3 movement) {
        if (!this.level.isClientSide && !this.isRemoved() && movement.lengthSqr() > 0.0D) {
            this.kill();
            this.dropItem((Entity)null);
        }

    }

    @Override
    public void push(double deltaX, double deltaY, double deltaZ) {
        if (!this.level.isClientSide && !this.isRemoved() && deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 0.0D) {
            this.kill();
            this.dropItem((Entity)null);
        }

    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        BlockPos blockPos = this.getPos();
        nbt.putInt("TileX", blockPos.getX());
        nbt.putInt("TileY", blockPos.getY());
        nbt.putInt("TileZ", blockPos.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        this.pos = new BlockPos(nbt.getInt("TileX"), nbt.getInt("TileY"), nbt.getInt("TileZ"));
    }

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract void dropItem(@Nullable Entity entity);

    public abstract void playPlacementSound();

    @Override
    public ItemEntity spawnAtLocation(ItemStack stack, float yOffset) {
        ItemEntity itemEntity = new ItemEntity(this.level, this.getX() + (double)((float)this.direction.getStepX() * 0.15F), this.getY() + (double)yOffset, this.getZ() + (double)((float)this.direction.getStepZ() * 0.15F), stack);
        itemEntity.setDefaultPickUpDelay();
        this.level.addFreshEntity(itemEntity);
        return itemEntity;
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    public void setPos(double x, double y, double z) {
        this.pos = new BlockPos(x, y, z);
        this.recalculateBoundingBox();
        this.hasImpulse = true;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public float rotate(Rotation rotation) {
        if (this.direction.getAxis() != Direction.Axis.Y) {
            switch(rotation) {
            case CLOCKWISE_180:
                this.direction = this.direction.getOpposite();
                break;
            case COUNTERCLOCKWISE_90:
                this.direction = this.direction.getCounterClockWise();
                break;
            case CLOCKWISE_90:
                this.direction = this.direction.getClockWise();
            }
        }

        float f = Mth.wrapDegrees(this.getYRot());
        switch(rotation) {
        case CLOCKWISE_180:
            return f + 180.0F;
        case COUNTERCLOCKWISE_90:
            return f + 90.0F;
        case CLOCKWISE_90:
            return f + 270.0F;
        default:
            return f;
        }
    }

    @Override
    public float mirror(Mirror mirror) {
        return this.rotate(mirror.getRotation(this.direction));
    }

    @Override
    public void thunderHit(ServerLevel world, LightningBolt lightning) {
    }

    @Override
    public void refreshDimensions() {
    }
}
