package net.minecraft.world.entity.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class MinecartSpawner extends AbstractMinecart {
    private final BaseSpawner spawner = new BaseSpawner() {
        @Override
        public void broadcastEvent(Level world, BlockPos pos, int i) {
            world.broadcastEntityEvent(MinecartSpawner.this, (byte)i);
        }
    };
    private final Runnable ticker;

    public MinecartSpawner(EntityType<? extends MinecartSpawner> type, Level world) {
        super(type, world);
        this.ticker = this.createTicker(world);
    }

    public MinecartSpawner(Level world, double x, double y, double z) {
        super(EntityType.SPAWNER_MINECART, world, x, y, z);
        this.ticker = this.createTicker(world);
    }

    private Runnable createTicker(Level world) {
        return world instanceof ServerLevel ? () -> {
            this.spawner.serverTick((ServerLevel)world, this.blockPosition());
        } : () -> {
            this.spawner.clientTick(world, this.blockPosition());
        };
    }

    @Override
    public AbstractMinecart.Type getMinecartType() {
        return AbstractMinecart.Type.SPAWNER;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.SPAWNER.defaultBlockState();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.spawner.load(this.level, this.blockPosition(), nbt);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        this.spawner.save(nbt);
    }

    @Override
    public void handleEntityEvent(byte status) {
        this.spawner.onEventTriggered(this.level, status);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticker.run();
    }

    public BaseSpawner getSpawner() {
        return this.spawner;
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }
}
