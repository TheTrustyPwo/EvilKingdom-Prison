package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class ProcessorRule {
    public static final Codec<ProcessorRule> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(RuleTest.CODEC.fieldOf("input_predicate").forGetter((processorRule) -> {
            return processorRule.inputPredicate;
        }), RuleTest.CODEC.fieldOf("location_predicate").forGetter((processorRule) -> {
            return processorRule.locPredicate;
        }), PosRuleTest.CODEC.optionalFieldOf("position_predicate", PosAlwaysTrueTest.INSTANCE).forGetter((processorRule) -> {
            return processorRule.posPredicate;
        }), BlockState.CODEC.fieldOf("output_state").forGetter((processorRule) -> {
            return processorRule.outputState;
        }), CompoundTag.CODEC.optionalFieldOf("output_nbt").forGetter((processorRule) -> {
            return Optional.ofNullable(processorRule.outputTag);
        })).apply(instance, ProcessorRule::new);
    });
    private final RuleTest inputPredicate;
    private final RuleTest locPredicate;
    private final PosRuleTest posPredicate;
    private final BlockState outputState;
    @Nullable
    private final CompoundTag outputTag;

    public ProcessorRule(RuleTest inputPredicate, RuleTest locationPredicate, BlockState state) {
        this(inputPredicate, locationPredicate, PosAlwaysTrueTest.INSTANCE, state, Optional.empty());
    }

    public ProcessorRule(RuleTest inputPredicate, RuleTest locationPredicate, PosRuleTest positionPredicate, BlockState state) {
        this(inputPredicate, locationPredicate, positionPredicate, state, Optional.empty());
    }

    public ProcessorRule(RuleTest inputPredicate, RuleTest locationPredicate, PosRuleTest positionPredicate, BlockState outputState, Optional<CompoundTag> nbt) {
        this.inputPredicate = inputPredicate;
        this.locPredicate = locationPredicate;
        this.posPredicate = positionPredicate;
        this.outputState = outputState;
        this.outputTag = nbt.orElse((CompoundTag)null);
    }

    public boolean test(BlockState input, BlockState location, BlockPos blockPos, BlockPos blockPos2, BlockPos pivot, Random random) {
        return this.inputPredicate.test(input, random) && this.locPredicate.test(location, random) && this.posPredicate.test(blockPos, blockPos2, pivot, random);
    }

    public BlockState getOutputState() {
        return this.outputState;
    }

    @Nullable
    public CompoundTag getOutputTag() {
        return this.outputTag;
    }
}
