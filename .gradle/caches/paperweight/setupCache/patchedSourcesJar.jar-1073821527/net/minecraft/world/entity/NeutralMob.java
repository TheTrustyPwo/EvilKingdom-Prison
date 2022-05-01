package net.minecraft.world.entity;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public interface NeutralMob {
    String TAG_ANGER_TIME = "AngerTime";
    String TAG_ANGRY_AT = "AngryAt";

    int getRemainingPersistentAngerTime();

    void setRemainingPersistentAngerTime(int angerTime);

    @Nullable
    UUID getPersistentAngerTarget();

    void setPersistentAngerTarget(@Nullable UUID angryAt);

    void startPersistentAngerTimer();

    default void addPersistentAngerSaveData(CompoundTag nbt) {
        nbt.putInt("AngerTime", this.getRemainingPersistentAngerTime());
        if (this.getPersistentAngerTarget() != null) {
            nbt.putUUID("AngryAt", this.getPersistentAngerTarget());
        }

    }

    default void readPersistentAngerSaveData(Level world, CompoundTag nbt) {
        this.setRemainingPersistentAngerTime(nbt.getInt("AngerTime"));
        if (world instanceof ServerLevel) {
            if (!nbt.hasUUID("AngryAt")) {
                this.setPersistentAngerTarget((UUID)null);
            } else {
                UUID uUID = nbt.getUUID("AngryAt");
                this.setPersistentAngerTarget(uUID);
                Entity entity = ((ServerLevel)world).getEntity(uUID);
                if (entity != null) {
                    if (entity instanceof Mob) {
                        this.setLastHurtByMob((Mob)entity);
                    }

                    if (entity.getType() == EntityType.PLAYER) {
                        this.setLastHurtByPlayer((Player)entity);
                    }

                }
            }
        }
    }

    default void updatePersistentAnger(ServerLevel world, boolean angerPersistent) {
        LivingEntity livingEntity = this.getTarget();
        UUID uUID = this.getPersistentAngerTarget();
        if ((livingEntity == null || livingEntity.isDeadOrDying()) && uUID != null && world.getEntity(uUID) instanceof Mob) {
            this.stopBeingAngry();
        } else {
            if (livingEntity != null && !Objects.equals(uUID, livingEntity.getUUID())) {
                this.setPersistentAngerTarget(livingEntity.getUUID());
                this.startPersistentAngerTimer();
            }

            if (this.getRemainingPersistentAngerTime() > 0 && (livingEntity == null || livingEntity.getType() != EntityType.PLAYER || !angerPersistent)) {
                this.setRemainingPersistentAngerTime(this.getRemainingPersistentAngerTime() - 1);
                if (this.getRemainingPersistentAngerTime() == 0) {
                    this.stopBeingAngry();
                }
            }

        }
    }

    default boolean isAngryAt(LivingEntity entity) {
        if (!this.canAttack(entity)) {
            return false;
        } else {
            return entity.getType() == EntityType.PLAYER && this.isAngryAtAllPlayers(entity.level) ? true : entity.getUUID().equals(this.getPersistentAngerTarget());
        }
    }

    default boolean isAngryAtAllPlayers(Level world) {
        return world.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER) && this.isAngry() && this.getPersistentAngerTarget() == null;
    }

    default boolean isAngry() {
        return this.getRemainingPersistentAngerTime() > 0;
    }

    default void playerDied(Player player) {
        if (player.level.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            if (player.getUUID().equals(this.getPersistentAngerTarget())) {
                this.stopBeingAngry();
            }
        }
    }

    default void forgetCurrentTargetAndRefreshUniversalAnger() {
        this.stopBeingAngry();
        this.startPersistentAngerTimer();
    }

    default void stopBeingAngry() {
        this.setLastHurtByMob((LivingEntity)null);
        this.setPersistentAngerTarget((UUID)null);
        this.setTarget((LivingEntity)null);
        this.setRemainingPersistentAngerTime(0);
    }

    @Nullable
    LivingEntity getLastHurtByMob();

    void setLastHurtByMob(@Nullable LivingEntity attacker);

    void setLastHurtByPlayer(@Nullable Player attacking);

    void setTarget(@Nullable LivingEntity target);

    boolean canAttack(LivingEntity target);

    @Nullable
    LivingEntity getTarget();
}
