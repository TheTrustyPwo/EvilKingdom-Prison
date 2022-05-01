package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.mutable.MutableInt;

public class FossilFeature extends Feature<FossilFeatureConfiguration> {
    public FossilFeature(Codec<FossilFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<FossilFeatureConfiguration> context) {
        Random random = context.random();
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        Rotation rotation = Rotation.getRandom(random);
        FossilFeatureConfiguration fossilFeatureConfiguration = context.config();
        int i = random.nextInt(fossilFeatureConfiguration.fossilStructures.size());
        StructureManager structureManager = worldGenLevel.getLevel().getServer().getStructureManager();
        StructureTemplate structureTemplate = structureManager.getOrCreate(fossilFeatureConfiguration.fossilStructures.get(i));
        StructureTemplate structureTemplate2 = structureManager.getOrCreate(fossilFeatureConfiguration.overlayStructures.get(i));
        ChunkPos chunkPos = new ChunkPos(blockPos);
        BoundingBox boundingBox = new BoundingBox(chunkPos.getMinBlockX() - 16, worldGenLevel.getMinBuildHeight(), chunkPos.getMinBlockZ() - 16, chunkPos.getMaxBlockX() + 16, worldGenLevel.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 16);
        StructurePlaceSettings structurePlaceSettings = (new StructurePlaceSettings()).setRotation(rotation).setBoundingBox(boundingBox).setRandom(random);
        Vec3i vec3i = structureTemplate.getSize(rotation);
        BlockPos blockPos2 = blockPos.offset(-vec3i.getX() / 2, 0, -vec3i.getZ() / 2);
        int j = blockPos.getY();

        for(int k = 0; k < vec3i.getX(); ++k) {
            for(int l = 0; l < vec3i.getZ(); ++l) {
                j = Math.min(j, worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, blockPos2.getX() + k, blockPos2.getZ() + l));
            }
        }

        int m = Math.max(j - 15 - random.nextInt(10), worldGenLevel.getMinBuildHeight() + 10);
        BlockPos blockPos3 = structureTemplate.getZeroPositionWithTransform(blockPos2.atY(m), Mirror.NONE, rotation);
        if (countEmptyCorners(worldGenLevel, structureTemplate.getBoundingBox(structurePlaceSettings, blockPos3)) > fossilFeatureConfiguration.maxEmptyCornersAllowed) {
            return false;
        } else {
            structurePlaceSettings.clearProcessors();
            fossilFeatureConfiguration.fossilProcessors.value().list().forEach(structurePlaceSettings::addProcessor);
            structureTemplate.placeInWorld(worldGenLevel, blockPos3, blockPos3, structurePlaceSettings, random, 4);
            structurePlaceSettings.clearProcessors();
            fossilFeatureConfiguration.overlayProcessors.value().list().forEach(structurePlaceSettings::addProcessor);
            structureTemplate2.placeInWorld(worldGenLevel, blockPos3, blockPos3, structurePlaceSettings, random, 4);
            return true;
        }
    }

    private static int countEmptyCorners(WorldGenLevel world, BoundingBox box) {
        MutableInt mutableInt = new MutableInt(0);
        box.forAllCorners((pos) -> {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isAir() || blockState.is(Blocks.LAVA) || blockState.is(Blocks.WATER)) {
                mutableInt.add(1);
            }

        });
        return mutableInt.getValue();
    }
}
