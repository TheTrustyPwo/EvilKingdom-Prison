package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

public class BlockRotProcessor extends StructureProcessor {
    public static final Codec<BlockRotProcessor> CODEC = Codec.FLOAT.fieldOf("integrity").orElse(1.0F).xmap(BlockRotProcessor::new, (blockRotProcessor) -> {
        return blockRotProcessor.integrity;
    }).codec();
    private final float integrity;

    public BlockRotProcessor(float integrity) {
        this.integrity = integrity;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo structureBlockInfo, StructureTemplate.StructureBlockInfo structureBlockInfo2, StructurePlaceSettings data) {
        Random random = data.getRandom(structureBlockInfo2.pos);
        return !(this.integrity >= 1.0F) && !(random.nextFloat() <= this.integrity) ? null : structureBlockInfo2;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.BLOCK_ROT;
    }
}
