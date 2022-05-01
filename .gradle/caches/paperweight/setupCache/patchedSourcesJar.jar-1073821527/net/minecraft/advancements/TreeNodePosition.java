package net.minecraft.advancements;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;

public class TreeNodePosition {
    private final Advancement advancement;
    @Nullable
    private final TreeNodePosition parent;
    @Nullable
    private final TreeNodePosition previousSibling;
    private final int childIndex;
    private final List<TreeNodePosition> children = Lists.newArrayList();
    private TreeNodePosition ancestor;
    @Nullable
    private TreeNodePosition thread;
    private int x;
    private float y;
    private float mod;
    private float change;
    private float shift;

    public TreeNodePosition(Advancement advancement, @Nullable TreeNodePosition parent, @Nullable TreeNodePosition previousSibling, int childrenSize, int depth) {
        if (advancement.getDisplay() == null) {
            throw new IllegalArgumentException("Can't position an invisible advancement!");
        } else {
            this.advancement = advancement;
            this.parent = parent;
            this.previousSibling = previousSibling;
            this.childIndex = childrenSize;
            this.ancestor = this;
            this.x = depth;
            this.y = -1.0F;
            TreeNodePosition treeNodePosition = null;

            for(Advancement advancement2 : advancement.getChildren()) {
                treeNodePosition = this.addChild(advancement2, treeNodePosition);
            }

        }
    }

    @Nullable
    private TreeNodePosition addChild(Advancement advancement, @Nullable TreeNodePosition lastChild) {
        if (advancement.getDisplay() != null) {
            lastChild = new TreeNodePosition(advancement, this, lastChild, this.children.size() + 1, this.x + 1);
            this.children.add(lastChild);
        } else {
            for(Advancement advancement2 : advancement.getChildren()) {
                lastChild = this.addChild(advancement2, lastChild);
            }
        }

        return lastChild;
    }

    private void firstWalk() {
        if (this.children.isEmpty()) {
            if (this.previousSibling != null) {
                this.y = this.previousSibling.y + 1.0F;
            } else {
                this.y = 0.0F;
            }

        } else {
            TreeNodePosition treeNodePosition = null;

            for(TreeNodePosition treeNodePosition2 : this.children) {
                treeNodePosition2.firstWalk();
                treeNodePosition = treeNodePosition2.apportion(treeNodePosition == null ? treeNodePosition2 : treeNodePosition);
            }

            this.executeShifts();
            float f = ((this.children.get(0)).y + (this.children.get(this.children.size() - 1)).y) / 2.0F;
            if (this.previousSibling != null) {
                this.y = this.previousSibling.y + 1.0F;
                this.mod = this.y - f;
            } else {
                this.y = f;
            }

        }
    }

    private float secondWalk(float deltaRow, int depth, float minRow) {
        this.y += deltaRow;
        this.x = depth;
        if (this.y < minRow) {
            minRow = this.y;
        }

        for(TreeNodePosition treeNodePosition : this.children) {
            minRow = treeNodePosition.secondWalk(deltaRow + this.mod, depth + 1, minRow);
        }

        return minRow;
    }

    private void thirdWalk(float deltaRow) {
        this.y += deltaRow;

        for(TreeNodePosition treeNodePosition : this.children) {
            treeNodePosition.thirdWalk(deltaRow);
        }

    }

    private void executeShifts() {
        float f = 0.0F;
        float g = 0.0F;

        for(int i = this.children.size() - 1; i >= 0; --i) {
            TreeNodePosition treeNodePosition = this.children.get(i);
            treeNodePosition.y += f;
            treeNodePosition.mod += f;
            g += treeNodePosition.change;
            f += treeNodePosition.shift + g;
        }

    }

    @Nullable
    private TreeNodePosition previousOrThread() {
        if (this.thread != null) {
            return this.thread;
        } else {
            return !this.children.isEmpty() ? this.children.get(0) : null;
        }
    }

    @Nullable
    private TreeNodePosition nextOrThread() {
        if (this.thread != null) {
            return this.thread;
        } else {
            return !this.children.isEmpty() ? this.children.get(this.children.size() - 1) : null;
        }
    }

    private TreeNodePosition apportion(TreeNodePosition last) {
        if (this.previousSibling == null) {
            return last;
        } else {
            TreeNodePosition treeNodePosition = this;
            TreeNodePosition treeNodePosition2 = this;
            TreeNodePosition treeNodePosition3 = this.previousSibling;
            TreeNodePosition treeNodePosition4 = this.parent.children.get(0);
            float f = this.mod;
            float g = this.mod;
            float h = treeNodePosition3.mod;

            float i;
            for(i = treeNodePosition4.mod; treeNodePosition3.nextOrThread() != null && treeNodePosition.previousOrThread() != null; g += treeNodePosition2.mod) {
                treeNodePosition3 = treeNodePosition3.nextOrThread();
                treeNodePosition = treeNodePosition.previousOrThread();
                treeNodePosition4 = treeNodePosition4.previousOrThread();
                treeNodePosition2 = treeNodePosition2.nextOrThread();
                treeNodePosition2.ancestor = this;
                float j = treeNodePosition3.y + h - (treeNodePosition.y + f) + 1.0F;
                if (j > 0.0F) {
                    treeNodePosition3.getAncestor(this, last).moveSubtree(this, j);
                    f += j;
                    g += j;
                }

                h += treeNodePosition3.mod;
                f += treeNodePosition.mod;
                i += treeNodePosition4.mod;
            }

            if (treeNodePosition3.nextOrThread() != null && treeNodePosition2.nextOrThread() == null) {
                treeNodePosition2.thread = treeNodePosition3.nextOrThread();
                treeNodePosition2.mod += h - g;
            } else {
                if (treeNodePosition.previousOrThread() != null && treeNodePosition4.previousOrThread() == null) {
                    treeNodePosition4.thread = treeNodePosition.previousOrThread();
                    treeNodePosition4.mod += f - i;
                }

                last = this;
            }

            return last;
        }
    }

    private void moveSubtree(TreeNodePosition positioner, float extraRowDistance) {
        float f = (float)(positioner.childIndex - this.childIndex);
        if (f != 0.0F) {
            positioner.change -= extraRowDistance / f;
            this.change += extraRowDistance / f;
        }

        positioner.shift += extraRowDistance;
        positioner.y += extraRowDistance;
        positioner.mod += extraRowDistance;
    }

    private TreeNodePosition getAncestor(TreeNodePosition treeNodePosition, TreeNodePosition treeNodePosition2) {
        return this.ancestor != null && treeNodePosition.parent.children.contains(this.ancestor) ? this.ancestor : treeNodePosition2;
    }

    private void finalizePosition() {
        if (this.advancement.getDisplay() != null) {
            this.advancement.getDisplay().setLocation((float)this.x, this.y);
        }

        if (!this.children.isEmpty()) {
            for(TreeNodePosition treeNodePosition : this.children) {
                treeNodePosition.finalizePosition();
            }
        }

    }

    public static void run(Advancement root) {
        if (root.getDisplay() == null) {
            throw new IllegalArgumentException("Can't position children of an invisible root!");
        } else {
            TreeNodePosition treeNodePosition = new TreeNodePosition(root, (TreeNodePosition)null, (TreeNodePosition)null, 1, 0);
            treeNodePosition.firstWalk();
            float f = treeNodePosition.secondWalk(0.0F, 0, treeNodePosition.y);
            if (f < 0.0F) {
                treeNodePosition.thirdWalk(-f);
            }

            treeNodePosition.finalizePosition();
        }
    }
}
