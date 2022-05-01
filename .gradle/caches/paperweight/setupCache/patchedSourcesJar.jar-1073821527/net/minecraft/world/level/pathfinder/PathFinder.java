package net.minecraft.world.level.pathfinder;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;

public class PathFinder {
    private static final float FUDGING = 1.5F;
    private final Node[] neighbors = new Node[32];
    private final int maxVisitedNodes;
    public final NodeEvaluator nodeEvaluator;
    private static final boolean DEBUG = false;
    private final BinaryHeap openSet = new BinaryHeap();

    public PathFinder(NodeEvaluator pathNodeMaker, int range) {
        this.nodeEvaluator = pathNodeMaker;
        this.maxVisitedNodes = range;
    }

    @Nullable
    public Path findPath(PathNavigationRegion world, Mob mob, Set<BlockPos> positions, float followRange, int distance, float rangeMultiplier) {
        this.openSet.clear();
        this.nodeEvaluator.prepare(world, mob);
        Node node = this.nodeEvaluator.getStart();
        Map<Target, BlockPos> map = positions.stream().collect(Collectors.toMap((pos) -> {
            return this.nodeEvaluator.getGoal((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        }, Function.identity()));
        Path path = this.findPath(world.getProfiler(), node, map, followRange, distance, rangeMultiplier);
        this.nodeEvaluator.done();
        return path;
    }

    @Nullable
    private Path findPath(ProfilerFiller profiler, Node startNode, Map<Target, BlockPos> positions, float followRange, int distance, float rangeMultiplier) {
        profiler.push("find_path");
        profiler.markForCharting(MetricCategory.PATH_FINDING);
        Set<Target> set = positions.keySet();
        startNode.g = 0.0F;
        startNode.h = this.getBestH(startNode, set);
        startNode.f = startNode.h;
        this.openSet.clear();
        this.openSet.insert(startNode);
        Set<Node> set2 = ImmutableSet.of();
        int i = 0;
        Set<Target> set3 = Sets.newHashSetWithExpectedSize(set.size());
        int j = (int)((float)this.maxVisitedNodes * rangeMultiplier);

        while(!this.openSet.isEmpty()) {
            ++i;
            if (i >= j) {
                break;
            }

            Node node = this.openSet.pop();
            node.closed = true;

            for(Target target : set) {
                if (node.distanceManhattan(target) <= (float)distance) {
                    target.setReached();
                    set3.add(target);
                }
            }

            if (!set3.isEmpty()) {
                break;
            }

            if (!(node.distanceTo(startNode) >= followRange)) {
                int k = this.nodeEvaluator.getNeighbors(this.neighbors, node);

                for(int l = 0; l < k; ++l) {
                    Node node2 = this.neighbors[l];
                    float f = node.distanceTo(node2);
                    node2.walkedDistance = node.walkedDistance + f;
                    float g = node.g + f + node2.costMalus;
                    if (node2.walkedDistance < followRange && (!node2.inOpenSet() || g < node2.g)) {
                        node2.cameFrom = node;
                        node2.g = g;
                        node2.h = this.getBestH(node2, set) * 1.5F;
                        if (node2.inOpenSet()) {
                            this.openSet.changeCost(node2, node2.g + node2.h);
                        } else {
                            node2.f = node2.g + node2.h;
                            this.openSet.insert(node2);
                        }
                    }
                }
            }
        }

        Optional<Path> optional = !set3.isEmpty() ? set3.stream().map((target) -> {
            return this.reconstructPath(target.getBestNode(), positions.get(target), true);
        }).min(Comparator.comparingInt(Path::getNodeCount)) : set.stream().map((target) -> {
            return this.reconstructPath(target.getBestNode(), positions.get(target), false);
        }).min(Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount));
        profiler.pop();
        return !optional.isPresent() ? null : optional.get();
    }

    private float getBestH(Node node, Set<Target> targets) {
        float f = Float.MAX_VALUE;

        for(Target target : targets) {
            float g = node.distanceTo(target);
            target.updateBest(g, node);
            f = Math.min(g, f);
        }

        return f;
    }

    private Path reconstructPath(Node endNode, BlockPos target, boolean reachesTarget) {
        List<Node> list = Lists.newArrayList();
        Node node = endNode;
        list.add(0, endNode);

        while(node.cameFrom != null) {
            node = node.cameFrom;
            list.add(0, node);
        }

        return new Path(list, target, reachesTarget);
    }
}
