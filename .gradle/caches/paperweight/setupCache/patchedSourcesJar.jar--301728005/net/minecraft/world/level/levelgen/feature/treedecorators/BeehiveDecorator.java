package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

public class BeehiveDecorator extends TreeDecorator {
    public static final Codec<BeehiveDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(BeehiveDecorator::new, (decorator) -> {
        return decorator.probability;
    }).codec();
    private static final Direction WORLDGEN_FACING = Direction.SOUTH;
    private static final Direction[] SPAWN_DIRECTIONS = Direction.Plane.HORIZONTAL.stream().filter((direction) -> {
        return direction != WORLDGEN_FACING.getOpposite();
    }).toArray((i) -> {
        return new Direction[i];
    });
    private final float probability;

    public BeehiveDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.BEEHIVE;
    }

    @Override
    public void place(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, Random random, List<BlockPos> logPositions, List<BlockPos> leavesPositions) {
        if (!(random.nextFloat() >= this.probability)) {
            int i = !leavesPositions.isEmpty() ? Math.max(leavesPositions.get(0).getY() - 1, logPositions.get(0).getY() + 1) : Math.min(logPositions.get(0).getY() + 1 + random.nextInt(3), logPositions.get(logPositions.size() - 1).getY());
            List<BlockPos> list = logPositions.stream().filter((pos) -> {
                return pos.getY() == i;
            }).flatMap((pos) -> {
                return Stream.of(SPAWN_DIRECTIONS).map(pos::relative);
            }).collect(Collectors.toList());
            if (!list.isEmpty()) {
                Collections.shuffle(list);
                Optional<BlockPos> optional = list.stream().filter((pos) -> {
                    return Feature.isAir(world, pos) && Feature.isAir(world, pos.relative(WORLDGEN_FACING));
                }).findFirst();
                if (!optional.isEmpty()) {
                    replacer.accept(optional.get(), Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, WORLDGEN_FACING));
                    world.getBlockEntity(optional.get(), BlockEntityType.BEEHIVE).ifPresent((blockEntity) -> {
                        int i = 2 + random.nextInt(2);

                        for(int j = 0; j < i; ++j) {
                            CompoundTag compoundTag = new CompoundTag();
                            compoundTag.putString("id", Registry.ENTITY_TYPE.getKey(EntityType.BEE).toString());
                            blockEntity.storeBee(compoundTag, random.nextInt(599), false);
                        }

                    });
                }
            }
        }
    }
}
