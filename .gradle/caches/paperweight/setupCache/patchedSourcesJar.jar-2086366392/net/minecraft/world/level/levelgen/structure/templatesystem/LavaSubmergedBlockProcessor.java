package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class LavaSubmergedBlockProcessor extends StructureProcessor {
    public static final Codec<LavaSubmergedBlockProcessor> CODEC = Codec.unit(() -> {
        return LavaSubmergedBlockProcessor.INSTANCE;
    });
    public static final LavaSubmergedBlockProcessor INSTANCE = new LavaSubmergedBlockProcessor();

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo structureBlockInfo, StructureTemplate.StructureBlockInfo structureBlockInfo2, StructurePlaceSettings data) {
        BlockPos blockPos = structureBlockInfo2.pos;
        boolean bl = world.getBlockState(blockPos).is(Blocks.LAVA);
        return bl && !Block.isShapeFullBlock(structureBlockInfo2.state.getShape(world, blockPos)) ? new StructureTemplate.StructureBlockInfo(blockPos, Blocks.LAVA.defaultBlockState(), structureBlockInfo2.nbt) : structureBlockInfo2;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.LAVA_SUBMERGED_BLOCK;
    }
}
