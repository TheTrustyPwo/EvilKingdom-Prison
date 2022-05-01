package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.MineshaftConfiguration;
import net.minecraft.world.level.levelgen.structure.MineShaftPieces;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class MineshaftFeature extends StructureFeature<MineshaftConfiguration> {
    public MineshaftFeature(Codec<MineshaftConfiguration> configCodec) {
        super(configCodec, PieceGeneratorSupplier.simple(MineshaftFeature::checkLocation, MineshaftFeature::generatePieces));
    }

    private static boolean checkLocation(PieceGeneratorSupplier.Context<MineshaftConfiguration> context) {
        WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(0L));
        worldgenRandom.setLargeFeatureSeed(context.seed(), context.chunkPos().x, context.chunkPos().z);
        double d = (double)(context.config()).probability;
        return worldgenRandom.nextDouble() >= d ? false : context.validBiome().test(context.chunkGenerator().getNoiseBiome(QuartPos.fromBlock(context.chunkPos().getMiddleBlockX()), QuartPos.fromBlock(50), QuartPos.fromBlock(context.chunkPos().getMiddleBlockZ())));
    }

    private static void generatePieces(StructurePiecesBuilder collector, PieceGenerator.Context<MineshaftConfiguration> context) {
        MineShaftPieces.MineShaftRoom mineShaftRoom = new MineShaftPieces.MineShaftRoom(0, context.random(), context.chunkPos().getBlockX(2), context.chunkPos().getBlockZ(2), (context.config()).type);
        collector.addPiece(mineShaftRoom);
        mineShaftRoom.addChildren(mineShaftRoom, collector, context.random());
        int i = context.chunkGenerator().getSeaLevel();
        if ((context.config()).type == MineshaftFeature.Type.MESA) {
            BlockPos blockPos = collector.getBoundingBox().getCenter();
            int j = context.chunkGenerator().getBaseHeight(blockPos.getX(), blockPos.getZ(), Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor());
            int k = j <= i ? i : Mth.randomBetweenInclusive(context.random(), i, j);
            int l = k - blockPos.getY();
            collector.offsetPiecesVertically(l);
        } else {
            collector.moveBelowSeaLevel(i, context.chunkGenerator().getMinY(), context.random(), 10);
        }

    }

    public static enum Type implements StringRepresentable {
        NORMAL("normal", Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_FENCE),
        MESA("mesa", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_FENCE);

        public static final Codec<MineshaftFeature.Type> CODEC = StringRepresentable.fromEnum(MineshaftFeature.Type::values, MineshaftFeature.Type::byName);
        private static final Map<String, MineshaftFeature.Type> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(MineshaftFeature.Type::getName, (type) -> {
            return type;
        }));
        private final String name;
        private final BlockState woodState;
        private final BlockState planksState;
        private final BlockState fenceState;

        private Type(String name, Block log, Block planks, Block fence) {
            this.name = name;
            this.woodState = log.defaultBlockState();
            this.planksState = planks.defaultBlockState();
            this.fenceState = fence.defaultBlockState();
        }

        public String getName() {
            return this.name;
        }

        private static MineshaftFeature.Type byName(String name) {
            return BY_NAME.get(name);
        }

        public static MineshaftFeature.Type byId(int index) {
            return index >= 0 && index < values().length ? values()[index] : NORMAL;
        }

        public BlockState getWoodState() {
            return this.woodState;
        }

        public BlockState getPlanksState() {
            return this.planksState;
        }

        public BlockState getFenceState() {
            return this.fenceState;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
