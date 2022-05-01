package net.minecraft.world.entity.ai.village;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class VillageSiege implements CustomSpawner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean hasSetupSiege;
    private VillageSiege.State siegeState = VillageSiege.State.SIEGE_DONE;
    private int zombiesToSpawn;
    private int nextSpawnTime;
    private int spawnX;
    private int spawnY;
    private int spawnZ;

    @Override
    public int tick(ServerLevel world, boolean spawnMonsters, boolean spawnAnimals) {
        if (!world.isDay() && spawnMonsters) {
            float f = world.getTimeOfDay(0.0F);
            if ((double)f == 0.5D) {
                this.siegeState = world.random.nextInt(10) == 0 ? VillageSiege.State.SIEGE_TONIGHT : VillageSiege.State.SIEGE_DONE;
            }

            if (this.siegeState == VillageSiege.State.SIEGE_DONE) {
                return 0;
            } else {
                if (!this.hasSetupSiege) {
                    if (!this.tryToSetupSiege(world)) {
                        return 0;
                    }

                    this.hasSetupSiege = true;
                }

                if (this.nextSpawnTime > 0) {
                    --this.nextSpawnTime;
                    return 0;
                } else {
                    this.nextSpawnTime = 2;
                    if (this.zombiesToSpawn > 0) {
                        this.trySpawn(world);
                        --this.zombiesToSpawn;
                    } else {
                        this.siegeState = VillageSiege.State.SIEGE_DONE;
                    }

                    return 1;
                }
            }
        } else {
            this.siegeState = VillageSiege.State.SIEGE_DONE;
            this.hasSetupSiege = false;
            return 0;
        }
    }

    private boolean tryToSetupSiege(ServerLevel world) {
        for(Player player : world.players()) {
            if (!player.isSpectator()) {
                BlockPos blockPos = player.blockPosition();
                if (world.isVillage(blockPos) && Biome.getBiomeCategory(world.getBiome(blockPos)) != Biome.BiomeCategory.MUSHROOM) {
                    for(int i = 0; i < 10; ++i) {
                        float f = world.random.nextFloat() * ((float)Math.PI * 2F);
                        this.spawnX = blockPos.getX() + Mth.floor(Mth.cos(f) * 32.0F);
                        this.spawnY = blockPos.getY();
                        this.spawnZ = blockPos.getZ() + Mth.floor(Mth.sin(f) * 32.0F);
                        if (this.findRandomSpawnPos(world, new BlockPos(this.spawnX, this.spawnY, this.spawnZ)) != null) {
                            this.nextSpawnTime = 0;
                            this.zombiesToSpawn = 20;
                            break;
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private void trySpawn(ServerLevel world) {
        Vec3 vec3 = this.findRandomSpawnPos(world, new BlockPos(this.spawnX, this.spawnY, this.spawnZ));
        if (vec3 != null) {
            Zombie zombie;
            try {
                zombie = new Zombie(world);
                zombie.finalizeSpawn(world, world.getCurrentDifficultyAt(zombie.blockPosition()), MobSpawnType.EVENT, (SpawnGroupData)null, (CompoundTag)null);
            } catch (Exception var5) {
                LOGGER.warn("Failed to create zombie for village siege at {}", vec3, var5);
                return;
            }

            zombie.moveTo(vec3.x, vec3.y, vec3.z, world.random.nextFloat() * 360.0F, 0.0F);
            world.addFreshEntityWithPassengers(zombie);
        }
    }

    @Nullable
    private Vec3 findRandomSpawnPos(ServerLevel world, BlockPos pos) {
        for(int i = 0; i < 10; ++i) {
            int j = pos.getX() + world.random.nextInt(16) - 8;
            int k = pos.getZ() + world.random.nextInt(16) - 8;
            int l = world.getHeight(Heightmap.Types.WORLD_SURFACE, j, k);
            BlockPos blockPos = new BlockPos(j, l, k);
            if (world.isVillage(blockPos) && Monster.checkMonsterSpawnRules(EntityType.ZOMBIE, world, MobSpawnType.EVENT, blockPos, world.random)) {
                return Vec3.atBottomCenterOf(blockPos);
            }
        }

        return null;
    }

    static enum State {
        SIEGE_CAN_ACTIVATE,
        SIEGE_TONIGHT,
        SIEGE_DONE;
    }
}
