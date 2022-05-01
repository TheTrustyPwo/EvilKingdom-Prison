package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Path {
    public final List<Node> nodes;
    private Node[] openSet = new Node[0];
    private Node[] closedSet = new Node[0];
    @Nullable
    private Set<Target> targetNodes;
    private int nextNodeIndex;
    private final BlockPos target;
    private final float distToTarget;
    private final boolean reached;

    public Path(List<Node> nodes, BlockPos target, boolean reachesTarget) {
        this.nodes = nodes;
        this.target = target;
        this.distToTarget = nodes.isEmpty() ? Float.MAX_VALUE : this.nodes.get(this.nodes.size() - 1).distanceManhattan(this.target);
        this.reached = reachesTarget;
    }

    public void advance() {
        ++this.nextNodeIndex;
    }

    public boolean notStarted() {
        return this.nextNodeIndex <= 0;
    }

    public boolean isDone() {
        return this.nextNodeIndex >= this.nodes.size();
    }

    @Nullable
    public Node getEndNode() {
        return !this.nodes.isEmpty() ? this.nodes.get(this.nodes.size() - 1) : null;
    }

    public Node getNode(int index) {
        return this.nodes.get(index);
    }

    public void truncateNodes(int length) {
        if (this.nodes.size() > length) {
            this.nodes.subList(length, this.nodes.size()).clear();
        }

    }

    public void replaceNode(int index, Node node) {
        this.nodes.set(index, node);
    }

    public int getNodeCount() {
        return this.nodes.size();
    }

    public int getNextNodeIndex() {
        return this.nextNodeIndex;
    }

    public void setNextNodeIndex(int nodeIndex) {
        this.nextNodeIndex = nodeIndex;
    }

    public Vec3 getEntityPosAtNode(Entity entity, int index) {
        Node node = this.nodes.get(index);
        double d = (double)node.x + (double)((int)(entity.getBbWidth() + 1.0F)) * 0.5D;
        double e = (double)node.y;
        double f = (double)node.z + (double)((int)(entity.getBbWidth() + 1.0F)) * 0.5D;
        return new Vec3(d, e, f);
    }

    public BlockPos getNodePos(int index) {
        return this.nodes.get(index).asBlockPos();
    }

    public Vec3 getNextEntityPos(Entity entity) {
        return this.getEntityPosAtNode(entity, this.nextNodeIndex);
    }

    public BlockPos getNextNodePos() {
        return this.nodes.get(this.nextNodeIndex).asBlockPos();
    }

    public Node getNextNode() {
        return this.nodes.get(this.nextNodeIndex);
    }

    @Nullable
    public Node getPreviousNode() {
        return this.nextNodeIndex > 0 ? this.nodes.get(this.nextNodeIndex - 1) : null;
    }

    public boolean sameAs(@Nullable Path o) {
        if (o == null) {
            return false;
        } else if (o.nodes.size() != this.nodes.size()) {
            return false;
        } else {
            for(int i = 0; i < this.nodes.size(); ++i) {
                Node node = this.nodes.get(i);
                Node node2 = o.nodes.get(i);
                if (node.x != node2.x || node.y != node2.y || node.z != node2.z) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean canReach() {
        return this.reached;
    }

    @VisibleForDebug
    void setDebug(Node[] debugNodes, Node[] debugSecondNodes, Set<Target> debugTargetNodes) {
        this.openSet = debugNodes;
        this.closedSet = debugSecondNodes;
        this.targetNodes = debugTargetNodes;
    }

    @VisibleForDebug
    public Node[] getOpenSet() {
        return this.openSet;
    }

    @VisibleForDebug
    public Node[] getClosedSet() {
        return this.closedSet;
    }

    public void writeToStream(FriendlyByteBuf buffer) {
        if (this.targetNodes != null && !this.targetNodes.isEmpty()) {
            buffer.writeBoolean(this.reached);
            buffer.writeInt(this.nextNodeIndex);
            buffer.writeInt(this.targetNodes.size());
            this.targetNodes.forEach((target) -> {
                target.writeToStream(buffer);
            });
            buffer.writeInt(this.target.getX());
            buffer.writeInt(this.target.getY());
            buffer.writeInt(this.target.getZ());
            buffer.writeInt(this.nodes.size());

            for(Node node : this.nodes) {
                node.writeToStream(buffer);
            }

            buffer.writeInt(this.openSet.length);

            for(Node node2 : this.openSet) {
                node2.writeToStream(buffer);
            }

            buffer.writeInt(this.closedSet.length);

            for(Node node3 : this.closedSet) {
                node3.writeToStream(buffer);
            }

        }
    }

    public static Path createFromStream(FriendlyByteBuf buffer) {
        boolean bl = buffer.readBoolean();
        int i = buffer.readInt();
        int j = buffer.readInt();
        Set<Target> set = Sets.newHashSet();

        for(int k = 0; k < j; ++k) {
            set.add(Target.createFromStream(buffer));
        }

        BlockPos blockPos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
        List<Node> list = Lists.newArrayList();
        int l = buffer.readInt();

        for(int m = 0; m < l; ++m) {
            list.add(Node.createFromStream(buffer));
        }

        Node[] nodes = new Node[buffer.readInt()];

        for(int n = 0; n < nodes.length; ++n) {
            nodes[n] = Node.createFromStream(buffer);
        }

        Node[] nodes2 = new Node[buffer.readInt()];

        for(int o = 0; o < nodes2.length; ++o) {
            nodes2[o] = Node.createFromStream(buffer);
        }

        Path path = new Path(list, blockPos, bl);
        path.openSet = nodes;
        path.closedSet = nodes2;
        path.targetNodes = set;
        path.nextNodeIndex = i;
        return path;
    }

    @Override
    public String toString() {
        return "Path(length=" + this.nodes.size() + ")";
    }

    public BlockPos getTarget() {
        return this.target;
    }

    public float getDistToTarget() {
        return this.distToTarget;
    }
}
