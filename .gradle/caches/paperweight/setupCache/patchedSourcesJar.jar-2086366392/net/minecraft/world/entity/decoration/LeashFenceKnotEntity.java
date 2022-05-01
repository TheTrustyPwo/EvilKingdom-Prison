package net.minecraft.world.entity.decoration;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerPlayer;
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
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
// CraftBukkit end

public class LeashFenceKnotEntity extends HangingEntity {

    public static final double OFFSET_Y = 0.375D;

    public LeashFenceKnotEntity(EntityType<? extends LeashFenceKnotEntity> type, Level world) {
        super(type, world);
    }

    public LeashFenceKnotEntity(Level world, BlockPos pos) {
        super(EntityType.LEASH_KNOT, world, pos);
        this.setPos((double) pos.getX(), (double) pos.getY(), (double) pos.getZ());
    }

    @Override
    protected void recalculateBoundingBox() {
        this.setPosRaw((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.375D, (double) this.pos.getZ() + 0.5D);
        double d0 = (double) this.getType().getWidth() / 2.0D;
        double d1 = (double) this.getType().getHeight();

        this.setBoundingBox(new AABB(this.getX() - d0, this.getY(), this.getZ() - d0, this.getX() + d0, this.getY() + d1, this.getZ() + d0));
    }

    @Override
    public void setDirection(Direction facing) {}

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
    public void addAdditionalSaveData(CompoundTag nbt) {}

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {}

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            boolean flag = false;
            double d0 = 7.0D;
            List<Mob> list = this.level.getEntitiesOfClass(Mob.class, new AABB(this.getX() - 7.0D, this.getY() - 7.0D, this.getZ() - 7.0D, this.getX() + 7.0D, this.getY() + 7.0D, this.getZ() + 7.0D));
            Iterator iterator = list.iterator();

            Mob entityinsentient;

            while (iterator.hasNext()) {
                entityinsentient = (Mob) iterator.next();
                if (entityinsentient.getLeashHolder() == player) {
                    // CraftBukkit start
                    if (CraftEventFactory.callPlayerLeashEntityEvent(entityinsentient, this, player).isCancelled()) {
                        ((ServerPlayer) player).connection.send(new ClientboundSetEntityLinkPacket(entityinsentient, entityinsentient.getLeashHolder()));
                        continue;
                    }
                    // CraftBukkit end
                    entityinsentient.setLeashedTo(this, true);
                    flag = true;
                }
            }

            if (!flag) {
                // CraftBukkit start - Move below
                // this.discard();
                boolean die = true;
                // CraftBukkit end
                if (true || player.getAbilities().instabuild) { // CraftBukkit - Process for non-creative as well
                    iterator = list.iterator();

                    while (iterator.hasNext()) {
                        entityinsentient = (Mob) iterator.next();
                        if (entityinsentient.isLeashed() && entityinsentient.getLeashHolder() == this) {
                            // CraftBukkit start
                            // Paper start - drop leash variable
                            org.bukkit.event.player.PlayerUnleashEntityEvent event = CraftEventFactory.callPlayerUnleashEntityEvent(entityinsentient, player, !player.getAbilities().instabuild);
                            if (event.isCancelled()) {
                                // Paper end
                                die = false;
                                continue;
                            }
                            entityinsentient.dropLeash(true, event.isDropLeash()); // false -> survival mode boolean // Paper - drop leash variable
                            // CraftBukkit end
                        }
                    }
                    // CraftBukkit start
                    if (die) {
                        this.discard();
                    }
                    // CraftBukkit end
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
        List<LeashFenceKnotEntity> list = world.getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB((double) i - 1.0D, (double) j - 1.0D, (double) k - 1.0D, (double) i + 1.0D, (double) j + 1.0D, (double) k + 1.0D));
        Iterator iterator = list.iterator();

        LeashFenceKnotEntity entityleash;

        do {
            if (!iterator.hasNext()) {
                LeashFenceKnotEntity entityleash1 = new LeashFenceKnotEntity(world, pos);

                world.addFreshEntity(entityleash1);
                return entityleash1;
            }

            entityleash = (LeashFenceKnotEntity) iterator.next();
        } while (!entityleash.getPos().equals(pos));

        return entityleash;
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
