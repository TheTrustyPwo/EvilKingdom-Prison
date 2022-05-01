package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class JigsawReplacementProcessor extends StructureProcessor {
    public static final Codec<JigsawReplacementProcessor> CODEC = Codec.unit(() -> {
        return JigsawReplacementProcessor.INSTANCE;
    });
    public static final JigsawReplacementProcessor INSTANCE = new JigsawReplacementProcessor();

    private JigsawReplacementProcessor() {
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo structureBlockInfo, StructureTemplate.StructureBlockInfo structureBlockInfo2, StructurePlaceSettings data) {
        BlockState blockState = structureBlockInfo2.state;
        if (blockState.is(Blocks.JIGSAW)) {
            String string = structureBlockInfo2.nbt.getString("final_state");
            BlockStateParser blockStateParser = new BlockStateParser(new StringReader(string), false);

            try {
                blockStateParser.parse(true);
            } catch (CommandSyntaxException var11) {
                throw new RuntimeException(var11);
            }

            return blockStateParser.getState().is(Blocks.STRUCTURE_VOID) ? null : new StructureTemplate.StructureBlockInfo(structureBlockInfo2.pos, blockStateParser.getState(), (CompoundTag)null);
        } else {
            return structureBlockInfo2;
        }
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.JIGSAW_REPLACEMENT;
    }
}
