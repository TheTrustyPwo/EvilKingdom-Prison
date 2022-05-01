package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public interface BlockPredicate extends BiPredicate<WorldGenLevel, BlockPos> {
    Codec<BlockPredicate> CODEC = Registry.BLOCK_PREDICATE_TYPES.byNameCodec().dispatch(BlockPredicate::type, BlockPredicateType::codec);
    BlockPredicate ONLY_IN_AIR_PREDICATE = matchesBlock(Blocks.AIR, BlockPos.ZERO);
    BlockPredicate ONLY_IN_AIR_OR_WATER_PREDICATE = matchesBlocks(List.of(Blocks.AIR, Blocks.WATER), BlockPos.ZERO);

    BlockPredicateType<?> type();

    static BlockPredicate allOf(List<BlockPredicate> predicates) {
        return new AllOfPredicate(predicates);
    }

    static BlockPredicate allOf(BlockPredicate... predicates) {
        return allOf(List.of(predicates));
    }

    static BlockPredicate allOf(BlockPredicate first, BlockPredicate second) {
        return allOf(List.of(first, second));
    }

    static BlockPredicate anyOf(List<BlockPredicate> predicates) {
        return new AnyOfPredicate(predicates);
    }

    static BlockPredicate anyOf(BlockPredicate... predicates) {
        return anyOf(List.of(predicates));
    }

    static BlockPredicate anyOf(BlockPredicate first, BlockPredicate second) {
        return anyOf(List.of(first, second));
    }

    static BlockPredicate matchesBlocks(List<Block> blocks, Vec3i offset) {
        return new MatchingBlocksPredicate(offset, HolderSet.direct(Block::builtInRegistryHolder, blocks));
    }

    static BlockPredicate matchesBlocks(List<Block> blocks) {
        return matchesBlocks(blocks, Vec3i.ZERO);
    }

    static BlockPredicate matchesBlock(Block block, Vec3i offset) {
        return matchesBlocks(List.of(block), offset);
    }

    static BlockPredicate matchesTag(TagKey<Block> tag, Vec3i offset) {
        return new MatchingBlockTagPredicate(offset, tag);
    }

    static BlockPredicate matchesTag(TagKey<Block> offset) {
        return matchesTag(offset, Vec3i.ZERO);
    }

    static BlockPredicate matchesFluids(List<Fluid> fluids, Vec3i offset) {
        return new MatchingFluidsPredicate(offset, HolderSet.direct(Fluid::builtInRegistryHolder, fluids));
    }

    static BlockPredicate matchesFluid(Fluid fluid, Vec3i offset) {
        return matchesFluids(List.of(fluid), offset);
    }

    static BlockPredicate not(BlockPredicate predicate) {
        return new NotPredicate(predicate);
    }

    static BlockPredicate replaceable(Vec3i offset) {
        return new ReplaceablePredicate(offset);
    }

    static BlockPredicate replaceable() {
        return replaceable(Vec3i.ZERO);
    }

    static BlockPredicate wouldSurvive(BlockState state, Vec3i offset) {
        return new WouldSurvivePredicate(offset, state);
    }

    static BlockPredicate hasSturdyFace(Vec3i offset, Direction face) {
        return new HasSturdyFacePredicate(offset, face);
    }

    static BlockPredicate hasSturdyFace(Direction face) {
        return hasSturdyFace(Vec3i.ZERO, face);
    }

    static BlockPredicate solid(Vec3i offset) {
        return new SolidPredicate(offset);
    }

    static BlockPredicate solid() {
        return solid(Vec3i.ZERO);
    }

    static BlockPredicate insideWorld(Vec3i offset) {
        return new InsideWorldBoundsPredicate(offset);
    }

    static BlockPredicate alwaysTrue() {
        return TrueBlockPredicate.INSTANCE;
    }
}
