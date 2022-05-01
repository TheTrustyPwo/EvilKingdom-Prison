package net.minecraft.world.level.block.state.pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;

public class BlockPattern {
    private final Predicate<BlockInWorld>[][][] pattern;
    private final int depth;
    private final int height;
    private final int width;

    public BlockPattern(Predicate<BlockInWorld>[][][] pattern) {
        this.pattern = pattern;
        this.depth = pattern.length;
        if (this.depth > 0) {
            this.height = pattern[0].length;
            if (this.height > 0) {
                this.width = pattern[0][0].length;
            } else {
                this.width = 0;
            }
        } else {
            this.height = 0;
            this.width = 0;
        }

    }

    public int getDepth() {
        return this.depth;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    @VisibleForTesting
    public Predicate<BlockInWorld>[][][] getPattern() {
        return this.pattern;
    }

    @Nullable
    @VisibleForTesting
    public BlockPattern.BlockPatternMatch matches(LevelReader world, BlockPos frontTopLeft, Direction forwards, Direction up) {
        LoadingCache<BlockPos, BlockInWorld> loadingCache = createLevelCache(world, false);
        return this.matches(frontTopLeft, forwards, up, loadingCache);
    }

    @Nullable
    private BlockPattern.BlockPatternMatch matches(BlockPos frontTopLeft, Direction forwards, Direction up, LoadingCache<BlockPos, BlockInWorld> cache) {
        for(int i = 0; i < this.width; ++i) {
            for(int j = 0; j < this.height; ++j) {
                for(int k = 0; k < this.depth; ++k) {
                    if (!this.pattern[k][j][i].test(cache.getUnchecked(translateAndRotate(frontTopLeft, forwards, up, i, j, k)))) {
                        return null;
                    }
                }
            }
        }

        return new BlockPattern.BlockPatternMatch(frontTopLeft, forwards, up, cache, this.width, this.height, this.depth);
    }

    @Nullable
    public BlockPattern.BlockPatternMatch find(LevelReader world, BlockPos pos) {
        LoadingCache<BlockPos, BlockInWorld> loadingCache = createLevelCache(world, false);
        int i = Math.max(Math.max(this.width, this.height), this.depth);

        for(BlockPos blockPos : BlockPos.betweenClosed(pos, pos.offset(i - 1, i - 1, i - 1))) {
            for(Direction direction : Direction.values()) {
                for(Direction direction2 : Direction.values()) {
                    if (direction2 != direction && direction2 != direction.getOpposite()) {
                        BlockPattern.BlockPatternMatch blockPatternMatch = this.matches(blockPos, direction, direction2, loadingCache);
                        if (blockPatternMatch != null) {
                            return blockPatternMatch;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static LoadingCache<BlockPos, BlockInWorld> createLevelCache(LevelReader world, boolean forceLoad) {
        return CacheBuilder.newBuilder().build(new BlockPattern.BlockCacheLoader(world, forceLoad));
    }

    protected static BlockPos translateAndRotate(BlockPos pos, Direction forwards, Direction up, int offsetLeft, int offsetDown, int offsetForwards) {
        if (forwards != up && forwards != up.getOpposite()) {
            Vec3i vec3i = new Vec3i(forwards.getStepX(), forwards.getStepY(), forwards.getStepZ());
            Vec3i vec3i2 = new Vec3i(up.getStepX(), up.getStepY(), up.getStepZ());
            Vec3i vec3i3 = vec3i.cross(vec3i2);
            return pos.offset(vec3i2.getX() * -offsetDown + vec3i3.getX() * offsetLeft + vec3i.getX() * offsetForwards, vec3i2.getY() * -offsetDown + vec3i3.getY() * offsetLeft + vec3i.getY() * offsetForwards, vec3i2.getZ() * -offsetDown + vec3i3.getZ() * offsetLeft + vec3i.getZ() * offsetForwards);
        } else {
            throw new IllegalArgumentException("Invalid forwards & up combination");
        }
    }

    static class BlockCacheLoader extends CacheLoader<BlockPos, BlockInWorld> {
        private final LevelReader level;
        private final boolean loadChunks;

        public BlockCacheLoader(LevelReader world, boolean forceLoad) {
            this.level = world;
            this.loadChunks = forceLoad;
        }

        @Override
        public BlockInWorld load(BlockPos blockPos) {
            return new BlockInWorld(this.level, blockPos, this.loadChunks);
        }
    }

    public static class BlockPatternMatch {
        private final BlockPos frontTopLeft;
        private final Direction forwards;
        private final Direction up;
        private final LoadingCache<BlockPos, BlockInWorld> cache;
        private final int width;
        private final int height;
        private final int depth;

        public BlockPatternMatch(BlockPos frontTopLeft, Direction forwards, Direction up, LoadingCache<BlockPos, BlockInWorld> cache, int width, int height, int depth) {
            this.frontTopLeft = frontTopLeft;
            this.forwards = forwards;
            this.up = up;
            this.cache = cache;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        public BlockPos getFrontTopLeft() {
            return this.frontTopLeft;
        }

        public Direction getForwards() {
            return this.forwards;
        }

        public Direction getUp() {
            return this.up;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public int getDepth() {
            return this.depth;
        }

        public BlockInWorld getBlock(int offsetLeft, int offsetDown, int offsetForwards) {
            return this.cache.getUnchecked(BlockPattern.translateAndRotate(this.frontTopLeft, this.getForwards(), this.getUp(), offsetLeft, offsetDown, offsetForwards));
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("up", this.up).add("forwards", this.forwards).add("frontTopLeft", this.frontTopLeft).toString();
        }
    }
}
