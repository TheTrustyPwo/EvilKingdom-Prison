package net.minecraft.world.entity.decoration;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LeashFenceKnotEntity extends HangingEntity {
    public static final double OFFSET_Y = 0.375D;

    public LeashFenceKnotEntity(EntityType<? extends LeashFenceKnotEntity> type, Level world) {
        super(type, world);
    }

    public LeashFenceKnotEntity(Level world, BlockPos pos) {
        super(EntityType.LEASH_KNOT, world, pos);
        this.setPos((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
    }

    @Override
    protected void recalculateBoundingBox() {
        this.setPosRaw((double)this.pos.getX() + 0.5D, (double)this.pos.getY() + 0.375D, (double)this.pos.getZ() + 0.5D);
        double d = (double)this.getType().getWidth() / 2.0D;
        double e = (double)this.getType().getHeight();
        this.setBoundingBox(new AABB(this.getX() - d, this.getY(), this.getZ() - d, this.getX() + d, this.getY() + e, this.getZ() + d));
    }

    @Override
    public void setDirection(Direction facing) {
    }

    @Override
    public int getWidth() {
        return 9;
    }

    @Override
    public int getHeight() {
        return 9;
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0.0625F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 1024.0D;
    }

    @Override
    public void dropItem(@Nullable Entity entity) {
        this.playSound(SoundEvents.LEASH_KNOT_BREAK, 1.0F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            boolean bl = false;
            double d = 7.0D;
            List<Mob> list = this.level.getEntitiesOfClass(Mob.class, new AABB(this.getX() - 7.0D, this.getY() - 7.0D, this.getZ() - 7.0D, this.getX() + 7.0D, this.getY() + 7.0D, this.getZ() + 7.0D));

            for(Mob mob : list) {
                if (mob.getLeashHolder() == player) {
                    mob.setLeashedTo(this, true);
                    bl = true;
                }
            }

            if (!bl) {
                this.discard();
                if (player.getAbilities().instabuild) {
                    for(Mob mob2 : list) {
                        if (mob2.isLeashed() && mob2.getLeashHolder() == this) {
                            mob2.dropLeash(true, false);
                        }
                    }
                }
            }

            return InteractionResult.CONSUME;
        }
    }

    @Override
    public boolean survives() {
        return this.level.getBlockState(this.pos).is(BlockTags.FENCES);
    }

    public static LeashFenceKnotEntity getOrCreateKnot(Level world, BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();

        for(LeashFenceKnotEntity leashFenceKnotEntity : world.getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB((double)i - 1.0D, (double)j - 1.0D, (double)k - 1.0D, (double)i + 1.0D, (double)j + 1.0D, (double)k + 1.0D))) {
            if (leashFenceKnotEntity.getPos().equals(pos)) {
                return leashFenceKnotEntity;
            }
        }

        LeashFenceKnotEntity leashFenceKnotEntity2 = new LeashFenceKnotEntity(world, pos);
        world.addFreshEntity(leashFenceKnotEntity2);
        return leashFenceKnotEntity2;
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0F, 1.0F);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, this.getType(), 0, this.getPos());
    }

    @Override
    public Vec3 getRopeHoldPosition(float delta) {
        return this.getPosition(delta).add(0.0D, 0.2D, 0.0D);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.LEAD);
    }
}
