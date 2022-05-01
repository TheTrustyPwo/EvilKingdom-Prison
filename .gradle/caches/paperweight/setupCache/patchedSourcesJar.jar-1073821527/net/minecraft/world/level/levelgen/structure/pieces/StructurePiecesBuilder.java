package net.minecraft.world.level.levelgen.structure.pieces;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;

public class StructurePiecesBuilder implements StructurePieceAccessor {
    private final List<StructurePiece> pieces = Lists.newArrayList();

    @Override
    public void addPiece(StructurePiece piece) {
        this.pieces.add(piece);
    }

    @Nullable
    @Override
    public StructurePiece findCollisionPiece(BoundingBox box) {
        return StructurePiece.findCollisionPiece(this.pieces, box);
    }

    /** @deprecated */
    @Deprecated
    public void offsetPiecesVertically(int y) {
        for(StructurePiece structurePiece : this.pieces) {
            structurePiece.move(0, y, 0);
        }

    }

    /** @deprecated */
    @Deprecated
    public void moveBelowSeaLevel(int topY, int bottomY, Random random, int topPenalty) {
        int i = topY - topPenalty;
        BoundingBox boundingBox = this.getBoundingBox();
        int j = boundingBox.getYSpan() + bottomY + 1;
        if (j < i) {
            j += random.nextInt(i - j);
        }

        int k = j - boundingBox.maxY();
        this.offsetPiecesVertically(k);
    }

    /** @deprecated */
    public void moveInsideHeights(Random random, int baseY, int topY) {
        BoundingBox boundingBox = this.getBoundingBox();
        int i = topY - baseY + 1 - boundingBox.getYSpan();
        int j;
        if (i > 1) {
            j = baseY + random.nextInt(i);
        } else {
            j = baseY;
        }

        int l = j - boundingBox.minY();
        this.offsetPiecesVertically(l);
    }

    public PiecesContainer build() {
        return new PiecesContainer(this.pieces);
    }

    public void clear() {
        this.pieces.clear();
    }

    public boolean isEmpty() {
        return this.pieces.isEmpty();
    }

    public BoundingBox getBoundingBox() {
        return StructurePiece.createBoundingBox(this.pieces.stream());
    }
}
