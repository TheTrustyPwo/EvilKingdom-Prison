package net.minecraft.world.level.levelgen;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class PatrolSpawner implements CustomSpawner {
    private int nextTick;

    @Override
    public int tick(ServerLevel world, boolean spawnMonsters, boolean spawnAnimals) {
        if (!spawnMonsters) {
            return 0;
        } else if (!world.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
            return 0;
        } else {
            Random random = world.random;
            --this.nextTick;
            if (this.nextTick > 0) {
                return 0;
            } else {
                this.nextTick += 12000 + random.nextInt(1200);
                long l = world.getDayTime() / 24000L;
                if (l >= 5L && world.isDay()) {
                    if (random.nextInt(5) != 0) {
                        return 0;
                    } else {
                        int i = world.players().size();
                        if (i < 1) {
                            return 0;
                        } else {
                            Player player = world.players().get(random.nextInt(i));
                            if (player.isSpectator()) {
                                return 0;
                            } else if (world.isCloseToVillage(player.blockPosition(), 2)) {
                                return 0;
                            } else {
                                int j = (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
                                int k = (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
                                BlockPos.MutableBlockPos mutableBlockPos = player.blockPosition().mutable().move(j, 0, k);
                                int m = 10;
                                if (!world.hasChunksAt(mutableBlockPos.getX() - 10, mutableBlockPos.getZ() - 10, mutableBlockPos.getX() + 10, mutableBlockPos.getZ() + 10)) {
                                    return 0;
                                } else {
                                    Holder<Biome> holder = world.getBiome(mutableBlockPos);
                                    Biome.BiomeCategory biomeCategory = Biome.getBiomeCategory(holder);
                                    if (biomeCategory == Biome.BiomeCategory.MUSHROOM) {
                                        return 0;
                                    } else {
                                        int n = 0;
                                        int o = (int)Math.ceil((double)world.getCurrentDifficultyAt(mutableBlockPos).getEffectiveDifficulty()) + 1;

                                        for(int p = 0; p < o; ++p) {
                                            ++n;
                                            mutableBlockPos.setY(world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mutableBlockPos).getY());
                                            if (p == 0) {
                                                if (!this.spawnPatrolMember(world, mutableBlockPos, random, true)) {
                                                    break;
                                                }
                                            } else {
                                                this.spawnPatrolMember(world, mutableBlockPos, random, false);
                                            }

                                            mutableBlockPos.setX(mutableBlockPos.getX() + random.nextInt(5) - random.nextInt(5));
                                            mutableBlockPos.setZ(mutableBlockPos.getZ() + random.nextInt(5) - random.nextInt(5));
                                        }

                                        return n;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    return 0;
                }
            }
        }
    }

    private boolean spawnPatrolMember(ServerLevel world, BlockPos pos, Random random, boolean captain) {
        BlockState blockState = world.getBlockState(pos);
        if (!NaturalSpawner.isValidEmptySpawnBlock(world, pos, blockState, blockState.getFluidState(), EntityType.PILLAGER)) {
            return false;
        } else if (!PatrollingMonster.checkPatrollingMonsterSpawnRules(EntityType.PILLAGER, world, MobSpawnType.PATROL, pos, random)) {
            return false;
        } else {
            PatrollingMonster patrollingMonster = EntityType.PILLAGER.create(world);
            if (patrollingMonster != null) {
                if (captain) {
                    patrollingMonster.setPatrolLeader(true);
                    patrollingMonster.findPatrolTarget();
                }

                patrollingMonster.setPos((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
                patrollingMonster.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), MobSpawnType.PATROL, (SpawnGroupData)null, (CompoundTag)null);
                world.addFreshEntityWithPassengers(patrollingMonster);
                return true;
            } else {
                return false;
            }
        }
    }
}
