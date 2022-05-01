package net.minecraft.world.level.levelgen.structure;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

public abstract class TemplateStructurePiece extends StructurePiece {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final String templateName;
    protected StructureTemplate template;
    protected StructurePlaceSettings placeSettings;
    protected BlockPos templatePosition;

    public TemplateStructurePiece(StructurePieceType type, int length, StructureManager structureManager, ResourceLocation id, String template, StructurePlaceSettings placementData, BlockPos pos) {
        super(type, length, structureManager.getOrCreate(id).getBoundingBox(placementData, pos));
        this.setOrientation(Direction.NORTH);
        this.templateName = template;
        this.templatePosition = pos;
        this.template = structureManager.getOrCreate(id);
        this.placeSettings = placementData;
    }

    public TemplateStructurePiece(StructurePieceType type, CompoundTag nbt, StructureManager structureManager, Function<ResourceLocation, StructurePlaceSettings> placementDataGetter) {
        super(type, nbt);
        this.setOrientation(Direction.NORTH);
        this.templateName = nbt.getString("Template");
        this.templatePosition = new BlockPos(nbt.getInt("TPX"), nbt.getInt("TPY"), nbt.getInt("TPZ"));
        ResourceLocation resourceLocation = this.makeTemplateLocation();
        this.template = structureManager.getOrCreate(resourceLocation);
        this.placeSettings = placementDataGetter.apply(resourceLocation);
        this.boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
    }

    protected ResourceLocation makeTemplateLocation() {
        return new ResourceLocation(this.templateName);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        nbt.putInt("TPX", this.templatePosition.getX());
        nbt.putInt("TPY", this.templatePosition.getY());
        nbt.putInt("TPZ", this.templatePosition.getZ());
        nbt.putString("Template", this.templateName);
    }

    @Override
    public void postProcess(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        this.placeSettings.setBoundingBox(chunkBox);
        this.boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
        if (this.template.placeInWorld(world, this.templatePosition, pos, this.placeSettings, random, 2)) {
            for(StructureTemplate.StructureBlockInfo structureBlockInfo : this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.STRUCTURE_BLOCK)) {
                if (structureBlockInfo.nbt != null) {
                    StructureMode structureMode = StructureMode.valueOf(structureBlockInfo.nbt.getString("mode"));
                    if (structureMode == StructureMode.DATA) {
                        this.handleDataMarker(structureBlockInfo.nbt.getString("metadata"), structureBlockInfo.pos, world, random, chunkBox);
                    }
                }
            }

            for(StructureTemplate.StructureBlockInfo structureBlockInfo2 : this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.JIGSAW)) {
                if (structureBlockInfo2.nbt != null) {
                    String string = structureBlockInfo2.nbt.getString("final_state");
                    BlockStateParser blockStateParser = new BlockStateParser(new StringReader(string), false);
                    BlockState blockState = Blocks.AIR.defaultBlockState();

                    try {
                        blockStateParser.parse(true);
                        BlockState blockState2 = blockStateParser.getState();
                        if (blockState2 != null) {
                            blockState = blockState2;
                        } else {
                            LOGGER.error("Error while parsing blockstate {} in jigsaw block @ {}", string, structureBlockInfo2.pos);
                        }
                    } catch (CommandSyntaxException var16) {
                        LOGGER.error("Error while parsing blockstate {} in jigsaw block @ {}", string, structureBlockInfo2.pos);
                    }

                    world.setBlock(structureBlockInfo2.pos, blockState, 3);
                }
            }
        }

    }

    protected abstract void handleDataMarker(String metadata, BlockPos pos, ServerLevelAccessor world, Random random, BoundingBox boundingBox);

    /** @deprecated */
    @Deprecated
    @Override
    public void move(int x, int y, int z) {
        super.move(x, y, z);
        this.templatePosition = this.templatePosition.offset(x, y, z);
    }

    @Override
    public Rotation getRotation() {
        return this.placeSettings.getRotation();
    }
}
