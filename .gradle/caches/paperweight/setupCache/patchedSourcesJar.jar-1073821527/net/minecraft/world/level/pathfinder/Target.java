package net.minecraft.world.level.pathfinder;

import net.minecraft.network.FriendlyByteBuf;

public class Target extends Node {
    private float bestHeuristic = Float.MAX_VALUE;
    private Node bestNode;
    private boolean reached;

    public Target(Node node) {
        super(node.x, node.y, node.z);
    }

    public Target(int x, int y, int z) {
        super(x, y, z);
    }

    public void updateBest(float distance, Node node) {
        if (distance < this.bestHeuristic) {
            this.bestHeuristic = distance;
            this.bestNode = node;
        }

    }

    public Node getBestNode() {
        return this.bestNode;
    }

    public void setReached() {
        this.reached = true;
    }

    public boolean isReached() {
        return this.reached;
    }

    public static Target createFromStream(FriendlyByteBuf buffer) {
        Target target = new Target(buffer.readInt(), buffer.readInt(), buffer.readInt());
        target.walkedDistance = buffer.readFloat();
        target.costMalus = buffer.readFloat();
        target.closed = buffer.readBoolean();
        target.type = BlockPathTypes.values()[buffer.readInt()];
        target.f = buffer.readFloat();
        return target;
    }
}
