package net.minecraft.world.level.levelgen.feature;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.slf4j.Logger;

public class MonsterRoomFeature extends Feature<NoneFeatureConfiguration> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityType<?>[] MOBS = new EntityType[]{EntityType.SKELETON, EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.SPIDER};
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

    public MonsterRoomFeature(Codec<NoneFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        Predicate<BlockState> predicate = Feature.isReplaceable(BlockTags.FEATURES_CANNOT_REPLACE);
        BlockPos blockPos = context.origin();
        Random random = context.random();
        WorldGenLevel worldGenLevel = context.level();
        int i = 3;
        int j = random.nextInt(2) + 2;
        int k = -j - 1;
        int l = j + 1;
        int m = -1;
        int n = 4;
        int o = random.nextInt(2) + 2;
        int p = -o - 1;
        int q = o + 1;
        int r = 0;

        for(int s = k; s <= l; ++s) {
            for(int t = -1; t <= 4; ++t) {
                for(int u = p; u <= q; ++u) {
                    BlockPos blockPos2 = blockPos.offset(s, t, u);
                    Material material = worldGenLevel.getBlockState(blockPos2).getMaterial();
                    boolean bl = material.isSolid();
                    if (t == -1 && !bl) {
                        return false;
                    }

                    if (t == 4 && !bl) {
                        return false;
                    }

                    if ((s == k || s == l || u == p || u == q) && t == 0 && worldGenLevel.isEmptyBlock(blockPos2) && worldGenLevel.isEmptyBlock(blockPos2.above())) {
                        ++r;
                    }
                }
            }
        }

        if (r >= 1 && r <= 5) {
            for(int v = k; v <= l; ++v) {
                for(int w = 3; w >= -1; --w) {
                    for(int x = p; x <= q; ++x) {
                        BlockPos blockPos3 = blockPos.offset(v, w, x);
                        BlockState blockState = worldGenLevel.getBlockState(blockPos3);
                        if (v != k && w != -1 && x != p && v != l && w != 4 && x != q) {
                            if (!blockState.is(Blocks.CHEST) && !blockState.is(Blocks.SPAWNER)) {
                                this.safeSetBlock(worldGenLevel, blockPos3, AIR, predicate);
                            }
                        } else if (blockPos3.getY() >= worldGenLevel.getMinBuildHeight() && !worldGenLevel.getBlockState(blockPos3.below()).getMaterial().isSolid()) {
                            worldGenLevel.setBlock(blockPos3, AIR, 2);
                        } else if (blockState.getMaterial().isSolid() && !blockState.is(Blocks.CHEST)) {
                            if (w == -1 && random.nextInt(4) != 0) {
                                this.safeSetBlock(worldGenLevel, blockPos3, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), predicate);
                            } else {
                                this.safeSetBlock(worldGenLevel, blockPos3, Blocks.COBBLESTONE.defaultBlockState(), predicate);
                            }
                        }
                    }
                }
            }

            for(int y = 0; y < 2; ++y) {
                for(int z = 0; z < 3; ++z) {
                    int aa = blockPos.getX() + random.nextInt(j * 2 + 1) - j;
                    int ab = blockPos.getY();
                    int ac = blockPos.getZ() + random.nextInt(o * 2 + 1) - o;
                    BlockPos blockPos4 = new BlockPos(aa, ab, ac);
                    if (worldGenLevel.isEmptyBlock(blockPos4)) {
                        int ad = 0;

                        for(Direction direction : Direction.Plane.HORIZONTAL) {
                            if (worldGenLevel.getBlockState(blockPos4.relative(direction)).getMaterial().isSolid()) {
                                ++ad;
                            }
                        }

                        if (ad == 1) {
                            this.safeSetBlock(worldGenLevel, blockPos4, StructurePiece.reorient(worldGenLevel, blockPos4, Blocks.CHEST.defaultBlockState()), predicate);
                            RandomizableContainerBlockEntity.setLootTable(worldGenLevel, random, blockPos4, BuiltInLootTables.SIMPLE_DUNGEON);
                            break;
                        }
                    }
                }
            }

            this.safeSetBlock(worldGenLevel, blockPos, Blocks.SPAWNER.defaultBlockState(), predicate);
            BlockEntity blockEntity = worldGenLevel.getBlockEntity(blockPos);
            if (blockEntity instanceof SpawnerBlockEntity) {
                ((SpawnerBlockEntity)blockEntity).getSpawner().setEntityId(this.randomEntityId(random));
            } else {
                LOGGER.error("Failed to fetch mob spawner entity at ({}, {}, {})", blockPos.getX(), blockPos.getY(), blockPos.getZ());
            }

            return true;
        } else {
            return false;
        }
    }

    private EntityType<?> randomEntityId(Random random) {
        return Util.getRandom(MOBS, random);
    }
}
