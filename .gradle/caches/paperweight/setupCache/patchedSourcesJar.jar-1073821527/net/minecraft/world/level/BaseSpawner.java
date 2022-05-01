package net.minecraft.world.level;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public abstract class BaseSpawner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int EVENT_SPAWN = 1;
    public int spawnDelay = 20;
    public SimpleWeightedRandomList<SpawnData> spawnPotentials = SimpleWeightedRandomList.empty();
    public SpawnData nextSpawnData = new SpawnData();
    private double spin;
    private double oSpin;
    public int minSpawnDelay = 200;
    public int maxSpawnDelay = 800;
    public int spawnCount = 4;
    @Nullable
    private Entity displayEntity;
    public int maxNearbyEntities = 6;
    public int requiredPlayerRange = 16;
    public int spawnRange = 4;
    private final Random random = new Random();

    public void setEntityId(EntityType<?> type) {
        this.nextSpawnData.getEntityToSpawn().putString("id", Registry.ENTITY_TYPE.getKey(type).toString());
    }

    public boolean isNearPlayer(Level world, BlockPos pos) {
        return world.hasNearbyAlivePlayer((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, (double)this.requiredPlayerRange);
    }

    public void clientTick(Level world, BlockPos pos) {
        if (!this.isNearPlayer(world, pos)) {
            this.oSpin = this.spin;
        } else {
            double d = (double)pos.getX() + world.random.nextDouble();
            double e = (double)pos.getY() + world.random.nextDouble();
            double f = (double)pos.getZ() + world.random.nextDouble();
            world.addParticle(ParticleTypes.SMOKE, d, e, f, 0.0D, 0.0D, 0.0D);
            world.addParticle(ParticleTypes.FLAME, d, e, f, 0.0D, 0.0D, 0.0D);
            if (this.spawnDelay > 0) {
                --this.spawnDelay;
            }

            this.oSpin = this.spin;
            this.spin = (this.spin + (double)(1000.0F / ((float)this.spawnDelay + 200.0F))) % 360.0D;
        }

    }

    public void serverTick(ServerLevel world, BlockPos pos) {
        if (this.isNearPlayer(world, pos)) {
            if (this.spawnDelay == -1) {
                this.delay(world, pos);
            }

            if (this.spawnDelay > 0) {
                --this.spawnDelay;
            } else {
                boolean bl = false;

                for(int i = 0; i < this.spawnCount; ++i) {
                    CompoundTag compoundTag = this.nextSpawnData.getEntityToSpawn();
                    Optional<EntityType<?>> optional = EntityType.by(compoundTag);
                    if (optional.isEmpty()) {
                        this.delay(world, pos);
                        return;
                    }

                    ListTag listTag = compoundTag.getList("Pos", 6);
                    int j = listTag.size();
                    double d = j >= 1 ? listTag.getDouble(0) : (double)pos.getX() + (world.random.nextDouble() - world.random.nextDouble()) * (double)this.spawnRange + 0.5D;
                    double e = j >= 2 ? listTag.getDouble(1) : (double)(pos.getY() + world.random.nextInt(3) - 1);
                    double f = j >= 3 ? listTag.getDouble(2) : (double)pos.getZ() + (world.random.nextDouble() - world.random.nextDouble()) * (double)this.spawnRange + 0.5D;
                    if (world.noCollision(optional.get().getAABB(d, e, f))) {
                        BlockPos blockPos = new BlockPos(d, e, f);
                        if (this.nextSpawnData.getCustomSpawnRules().isPresent()) {
                            if (!optional.get().getCategory().isFriendly() && world.getDifficulty() == Difficulty.PEACEFUL) {
                                continue;
                            }

                            SpawnData.CustomSpawnRules customSpawnRules = this.nextSpawnData.getCustomSpawnRules().get();
                            if (!customSpawnRules.blockLightLimit().isValueInRange(world.getBrightness(LightLayer.BLOCK, blockPos)) || !customSpawnRules.skyLightLimit().isValueInRange(world.getBrightness(LightLayer.SKY, blockPos))) {
                                continue;
                            }
                        } else if (!SpawnPlacements.checkSpawnRules(optional.get(), world, MobSpawnType.SPAWNER, blockPos, world.getRandom())) {
                            continue;
                        }

                        Entity entity = EntityType.loadEntityRecursive(compoundTag, world, (entityx) -> {
                            entityx.moveTo(d, e, f, entityx.getYRot(), entityx.getXRot());
                            return entityx;
                        });
                        if (entity == null) {
                            this.delay(world, pos);
                            return;
                        }

                        int k = world.getEntitiesOfClass(entity.getClass(), (new AABB((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1))).inflate((double)this.spawnRange)).size();
                        if (k >= this.maxNearbyEntities) {
                            this.delay(world, pos);
                            return;
                        }

                        entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), world.random.nextFloat() * 360.0F, 0.0F);
                        if (entity instanceof Mob) {
                            Mob mob = (Mob)entity;
                            if (this.nextSpawnData.getCustomSpawnRules().isEmpty() && !mob.checkSpawnRules(world, MobSpawnType.SPAWNER) || !mob.checkSpawnObstruction(world)) {
                                continue;
                            }

                            if (this.nextSpawnData.getEntityToSpawn().size() == 1 && this.nextSpawnData.getEntityToSpawn().contains("id", 8)) {
                                ((Mob)entity).finalizeSpawn(world, world.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, (SpawnGroupData)null, (CompoundTag)null);
                            }
                        }

                        if (!world.tryAddFreshEntityWithPassengers(entity)) {
                            this.delay(world, pos);
                            return;
                        }

                        world.levelEvent(2004, pos, 0);
                        if (entity instanceof Mob) {
                            ((Mob)entity).spawnAnim();
                        }

                        bl = true;
                    }
                }

                if (bl) {
                    this.delay(world, pos);
                }

            }
        }
    }

    public void delay(Level world, BlockPos pos) {
        if (this.maxSpawnDelay <= this.minSpawnDelay) {
            this.spawnDelay = this.minSpawnDelay;
        } else {
            this.spawnDelay = this.minSpawnDelay + this.random.nextInt(this.maxSpawnDelay - this.minSpawnDelay);
        }

        this.spawnPotentials.getRandom(this.random).ifPresent((wrapper) -> {
            this.setNextSpawnData(world, pos, wrapper.getData());
        });
        this.broadcastEvent(world, pos, 1);
    }

    public void load(@Nullable Level world, BlockPos pos, CompoundTag nbt) {
        this.spawnDelay = nbt.getShort("Delay");
        boolean bl = nbt.contains("SpawnPotentials", 9);
        boolean bl2 = nbt.contains("SpawnData", 10);
        if (!bl) {
            SpawnData spawnData;
            if (bl2) {
                spawnData = SpawnData.CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("SpawnData")).resultOrPartial((string) -> {
                    LOGGER.warn("Invalid SpawnData: {}", (Object)string);
                }).orElseGet(SpawnData::new);
            } else {
                spawnData = new SpawnData();
            }

            this.spawnPotentials = SimpleWeightedRandomList.single(spawnData);
            this.setNextSpawnData(world, pos, spawnData);
        } else {
            ListTag listTag = nbt.getList("SpawnPotentials", 10);
            this.spawnPotentials = SpawnData.LIST_CODEC.parse(NbtOps.INSTANCE, listTag).resultOrPartial((string) -> {
                LOGGER.warn("Invalid SpawnPotentials list: {}", (Object)string);
            }).orElseGet(SimpleWeightedRandomList::empty);
            if (bl2) {
                SpawnData spawnData3 = SpawnData.CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("SpawnData")).resultOrPartial((string) -> {
                    LOGGER.warn("Invalid SpawnData: {}", (Object)string);
                }).orElseGet(SpawnData::new);
                this.setNextSpawnData(world, pos, spawnData3);
            } else {
                this.spawnPotentials.getRandom(this.random).ifPresent((wrapper) -> {
                    this.setNextSpawnData(world, pos, wrapper.getData());
                });
            }
        }

        if (nbt.contains("MinSpawnDelay", 99)) {
            this.minSpawnDelay = nbt.getShort("MinSpawnDelay");
            this.maxSpawnDelay = nbt.getShort("MaxSpawnDelay");
            this.spawnCount = nbt.getShort("SpawnCount");
        }

        if (nbt.contains("MaxNearbyEntities", 99)) {
            this.maxNearbyEntities = nbt.getShort("MaxNearbyEntities");
            this.requiredPlayerRange = nbt.getShort("RequiredPlayerRange");
        }

        if (nbt.contains("SpawnRange", 99)) {
            this.spawnRange = nbt.getShort("SpawnRange");
        }

        this.displayEntity = null;
    }

    public CompoundTag save(CompoundTag nbt) {
        nbt.putShort("Delay", (short)this.spawnDelay);
        nbt.putShort("MinSpawnDelay", (short)this.minSpawnDelay);
        nbt.putShort("MaxSpawnDelay", (short)this.maxSpawnDelay);
        nbt.putShort("SpawnCount", (short)this.spawnCount);
        nbt.putShort("MaxNearbyEntities", (short)this.maxNearbyEntities);
        nbt.putShort("RequiredPlayerRange", (short)this.requiredPlayerRange);
        nbt.putShort("SpawnRange", (short)this.spawnRange);
        nbt.put("SpawnData", SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, this.nextSpawnData).result().orElseThrow(() -> {
            return new IllegalStateException("Invalid SpawnData");
        }));
        nbt.put("SpawnPotentials", SpawnData.LIST_CODEC.encodeStart(NbtOps.INSTANCE, this.spawnPotentials).result().orElseThrow());
        return nbt;
    }

    @Nullable
    public Entity getOrCreateDisplayEntity(Level world) {
        if (this.displayEntity == null) {
            this.displayEntity = EntityType.loadEntityRecursive(this.nextSpawnData.getEntityToSpawn(), world, Function.identity());
            if (this.nextSpawnData.getEntityToSpawn().size() == 1 && this.nextSpawnData.getEntityToSpawn().contains("id", 8) && this.displayEntity instanceof Mob) {
            }
        }

        return this.displayEntity;
    }

    public boolean onEventTriggered(Level level, int i) {
        if (i == 1) {
            if (level.isClientSide) {
                this.spawnDelay = this.minSpawnDelay;
            }

            return true;
        } else {
            return false;
        }
    }

    public void setNextSpawnData(@Nullable Level world, BlockPos pos, SpawnData spawnEntry) {
        this.nextSpawnData = spawnEntry;
    }

    public abstract void broadcastEvent(Level world, BlockPos pos, int i);

    public double getSpin() {
        return this.spin;
    }

    public double getoSpin() {
        return this.oSpin;
    }
}
