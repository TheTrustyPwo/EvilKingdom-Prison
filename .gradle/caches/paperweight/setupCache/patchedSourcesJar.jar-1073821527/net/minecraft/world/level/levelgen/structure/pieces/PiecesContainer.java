package net.minecraft.world.level.levelgen.structure.pieces;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.slf4j.Logger;

public record PiecesContainer(List<StructurePiece> pieces) {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation JIGSAW_RENAME = new ResourceLocation("jigsaw");
    private static final Map<ResourceLocation, ResourceLocation> RENAMES = ImmutableMap.<ResourceLocation, ResourceLocation>builder().put(new ResourceLocation("nvi"), JIGSAW_RENAME).put(new ResourceLocation("pcp"), JIGSAW_RENAME).put(new ResourceLocation("bastionremnant"), JIGSAW_RENAME).put(new ResourceLocation("runtime"), JIGSAW_RENAME).build();

    public PiecesContainer(List<StructurePiece> pieces) {
        this.pieces = List.copyOf(pieces);
    }

    public boolean isEmpty() {
        return this.pieces.isEmpty();
    }

    public boolean isInsidePiece(BlockPos pos) {
        for(StructurePiece structurePiece : this.pieces) {
            if (structurePiece.getBoundingBox().isInside(pos)) {
                return true;
            }
        }

        return false;
    }

    public Tag save(StructurePieceSerializationContext context) {
        ListTag listTag = new ListTag();

        for(StructurePiece structurePiece : this.pieces) {
            listTag.add(structurePiece.createTag(context));
        }

        return listTag;
    }

    public static PiecesContainer load(ListTag list, StructurePieceSerializationContext context) {
        List<StructurePiece> list2 = Lists.newArrayList();

        for(int i = 0; i < list.size(); ++i) {
            CompoundTag compoundTag = list.getCompound(i);
            String string = compoundTag.getString("id").toLowerCase(Locale.ROOT);
            ResourceLocation resourceLocation = new ResourceLocation(string);
            ResourceLocation resourceLocation2 = RENAMES.getOrDefault(resourceLocation, resourceLocation);
            StructurePieceType structurePieceType = Registry.STRUCTURE_PIECE.get(resourceLocation2);
            if (structurePieceType == null) {
                LOGGER.error("Unknown structure piece id: {}", (Object)resourceLocation2);
            } else {
                try {
                    StructurePiece structurePiece = structurePieceType.load(context, compoundTag);
                    list2.add(structurePiece);
                } catch (Exception var10) {
                    LOGGER.error("Exception loading structure piece with id {}", resourceLocation2, var10);
                }
            }
        }

        return new PiecesContainer(list2);
    }

    public BoundingBox calculateBoundingBox() {
        return StructurePiece.createBoundingBox(this.pieces.stream());
    }
}
