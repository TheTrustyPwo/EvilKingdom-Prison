package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public abstract class ContainerOpenersCounter {
    private static final int CHECK_TICK_DELAY = 5;
    private int openCount;

    protected abstract void onOpen(Level world, BlockPos pos, BlockState state);

    protected abstract void onClose(Level world, BlockPos pos, BlockState state);

    protected abstract void openerCountChanged(Level world, BlockPos pos, BlockState state, int oldViewerCount, int newViewerCount);

    protected abstract boolean isOwnContainer(Player player);

    public void incrementOpeners(Player player, Level world, BlockPos pos, BlockState state) {
        int i = this.openCount++;
        if (i == 0) {
            this.onOpen(world, pos, state);
            world.gameEvent(player, GameEvent.CONTAINER_OPEN, pos);
            scheduleRecheck(world, pos, state);
        }

        this.openerCountChanged(world, pos, state, i, this.openCount);
    }

    public void decrementOpeners(Player player, Level world, BlockPos pos, BlockState state) {
        int i = this.openCount--;
        if (this.openCount == 0) {
            this.onClose(world, pos, state);
            world.gameEvent(player, GameEvent.CONTAINER_CLOSE, pos);
        }

        this.openerCountChanged(world, pos, state, i, this.openCount);
    }

    private int getOpenCount(Level world, BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        float f = 5.0F;
        AABB aABB = new AABB((double)((float)i - 5.0F), (double)((float)j - 5.0F), (double)((float)k - 5.0F), (double)((float)(i + 1) + 5.0F), (double)((float)(j + 1) + 5.0F), (double)((float)(k + 1) + 5.0F));
        return world.getEntities(EntityTypeTest.forClass(Player.class), aABB, this::isOwnContainer).size();
    }

    public void recheckOpeners(Level world, BlockPos pos, BlockState state) {
        int i = this.getOpenCount(world, pos);
        int j = this.openCount;
        if (j != i) {
            boolean bl = i != 0;
            boolean bl2 = j != 0;
            if (bl && !bl2) {
                this.onOpen(world, pos, state);
                world.gameEvent((Entity)null, GameEvent.CONTAINER_OPEN, pos);
            } else if (!bl) {
                this.onClose(world, pos, state);
                world.gameEvent((Entity)null, GameEvent.CONTAINER_CLOSE, pos);
            }

            this.openCount = i;
        }

        this.openerCountChanged(world, pos, state, j, i);
        if (i > 0) {
            scheduleRecheck(world, pos, state);
        }

    }

    public int getOpenerCount() {
        return this.openCount;
    }

    private static void scheduleRecheck(Level world, BlockPos pos, BlockState state) {
        world.scheduleTick(pos, state.getBlock(), 5);
    }
}
