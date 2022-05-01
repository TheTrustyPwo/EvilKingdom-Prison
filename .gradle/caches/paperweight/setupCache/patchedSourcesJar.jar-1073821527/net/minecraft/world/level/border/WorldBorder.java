package net.minecraft.world.level.border;

import com.google.common.collect.Lists;
import com.mojang.serialization.DynamicLike;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WorldBorder {
    public static final double MAX_SIZE = 5.9999968E7D;
    public static final double MAX_CENTER_COORDINATE = 2.9999984E7D;
    private final List<BorderChangeListener> listeners = Lists.newArrayList();
    private double damagePerBlock = 0.2D;
    private double damageSafeZone = 5.0D;
    private int warningTime = 15;
    private int warningBlocks = 5;
    private double centerX;
    private double centerZ;
    int absoluteMaxSize = 29999984;
    private WorldBorder.BorderExtent extent = new WorldBorder.StaticBorderExtent(5.9999968E7D);
    public static final WorldBorder.Settings DEFAULT_SETTINGS = new WorldBorder.Settings(0.0D, 0.0D, 0.2D, 5.0D, 5, 15, 5.9999968E7D, 0L, 0.0D);

    public boolean isWithinBounds(BlockPos pos) {
        return (double)(pos.getX() + 1) > this.getMinX() && (double)pos.getX() < this.getMaxX() && (double)(pos.getZ() + 1) > this.getMinZ() && (double)pos.getZ() < this.getMaxZ();
    }

    public boolean isWithinBounds(ChunkPos pos) {
        return (double)pos.getMaxBlockX() > this.getMinX() && (double)pos.getMinBlockX() < this.getMaxX() && (double)pos.getMaxBlockZ() > this.getMinZ() && (double)pos.getMinBlockZ() < this.getMaxZ();
    }

    public boolean isWithinBounds(double x, double z) {
        return x > this.getMinX() && x < this.getMaxX() && z > this.getMinZ() && z < this.getMaxZ();
    }

    public boolean isWithinBounds(double x, double z, double margin) {
        return x > this.getMinX() - margin && x < this.getMaxX() + margin && z > this.getMinZ() - margin && z < this.getMaxZ() + margin;
    }

    public boolean isWithinBounds(AABB box) {
        return box.maxX > this.getMinX() && box.minX < this.getMaxX() && box.maxZ > this.getMinZ() && box.minZ < this.getMaxZ();
    }

    public BlockPos clampToBounds(double x, double y, double z) {
        return new BlockPos(Mth.clamp(x, this.getMinX(), this.getMaxX()), y, Mth.clamp(z, this.getMinZ(), this.getMaxZ()));
    }

    public double getDistanceToBorder(Entity entity) {
        return this.getDistanceToBorder(entity.getX(), entity.getZ());
    }

    public VoxelShape getCollisionShape() {
        return this.extent.getCollisionShape();
    }

    public double getDistanceToBorder(double x, double z) {
        double d = z - this.getMinZ();
        double e = this.getMaxZ() - z;
        double f = x - this.getMinX();
        double g = this.getMaxX() - x;
        double h = Math.min(f, g);
        h = Math.min(h, d);
        return Math.min(h, e);
    }

    public boolean isInsideCloseToBorder(Entity entity, AABB box) {
        double d = Math.max(Mth.absMax(box.getXsize(), box.getZsize()), 1.0D);
        return this.getDistanceToBorder(entity) < d * 2.0D && this.isWithinBounds(entity.getX(), entity.getZ(), d);
    }

    public BorderStatus getStatus() {
        return this.extent.getStatus();
    }

    public double getMinX() {
        return this.extent.getMinX();
    }

    public double getMinZ() {
        return this.extent.getMinZ();
    }

    public double getMaxX() {
        return this.extent.getMaxX();
    }

    public double getMaxZ() {
        return this.extent.getMaxZ();
    }

    public double getCenterX() {
        return this.centerX;
    }

    public double getCenterZ() {
        return this.centerZ;
    }

    public void setCenter(double x, double z) {
        this.centerX = x;
        this.centerZ = z;
        this.extent.onCenterChange();

        for(BorderChangeListener borderChangeListener : this.getListeners()) {
            borderChangeListener.onBorderCenterSet(this, x, z);
        }

    }

    public double getSize() {
        return this.extent.getSize();
    }

    public long getLerpRemainingTime() {
        return this.extent.getLerpRemainingTime();
    }

    public double getLerpTarget() {
        return this.extent.getLerpTarget();
    }

    public void setSize(double size) {
        this.extent = new WorldBorder.StaticBorderExtent(size);

        for(BorderChangeListener borderChangeListener : this.getListeners()) {
            borderChangeListener.onBorderSizeSet(this, size);
        }

    }

    public void lerpSizeBetween(double fromSize, double toSize, long time) {
        this.extent = (WorldBorder.BorderExtent)(fromSize == toSize ? new WorldBorder.StaticBorderExtent(toSize) : new WorldBorder.MovingBorderExtent(fromSize, toSize, time));

        for(BorderChangeListener borderChangeListener : this.getListeners()) {
            borderChangeListener.onBorderSizeLerping(this, fromSize, toSize, time);
        }

    }

    protected List<BorderChangeListener> getListeners() {
        return Lists.newArrayList(this.listeners);
    }

    public void addListener(BorderChangeListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(BorderChangeListener listener) {
        this.listeners.remove(listener);
    }

    public void setAbsoluteMaxSize(int maxRadius) {
        this.absoluteMaxSize = maxRadius;
        this.extent.onAbsoluteMaxSizeChange();
    }

    public int getAbsoluteMaxSize() {
        return this.absoluteMaxSize;
    }

    public double getDamageSafeZone() {
        return this.damageSafeZone;
    }

    public void setDamageSafeZone(double safeZone) {
        this.damageSafeZone = safeZone;

        for(BorderChangeListener borderChangeListener : this.getListeners()) {
            borderChangeListener.onBorderSetDamageSafeZOne(this, safeZone);
        }

    }

    public double getDamagePerBlock() {
        return this.damagePerBlock;
    }

    public void setDamagePerBlock(double damagePerBlock) {
        this.damagePerBlock = damagePerBlock;

        for(BorderChangeListener borderChangeListener : this.getListeners()) {
            borderChangeListener.onBorderSetDamagePerBlock(this, damagePerBlock);
        }

    }

    public double getLerpSpeed() {
        return this.extent.getLerpSpeed();
    }

    public int getWarningTime() {
        return this.warningTime;
    }

    public void setWarningTime(int warningTime) {
        this.warningTime = warningTime;

        for(BorderChangeListener borderChangeListener : this.getListeners()) {
            borderChangeListener.onBorderSetWarningTime(this, warningTime);
        }

    }

    public int getWarningBlocks() {
        return this.warningBlocks;
    }

    public void setWarningBlocks(int warningBlocks) {
        this.warningBlocks = warningBlocks;

        for(BorderChangeListener borderChangeListener : this.getListeners()) {
            borderChangeListener.onBorderSetWarningBlocks(this, warningBlocks);
        }

    }

    public void tick() {
        this.extent = this.extent.update();
    }

    public WorldBorder.Settings createSettings() {
        return new WorldBorder.Settings(this);
    }

    public void applySettings(WorldBorder.Settings properties) {
        this.setCenter(properties.getCenterX(), properties.getCenterZ());
        this.setDamagePerBlock(properties.getDamagePerBlock());
        this.setDamageSafeZone(properties.getSafeZone());
        this.setWarningBlocks(properties.getWarningBlocks());
        this.setWarningTime(properties.getWarningTime());
        if (properties.getSizeLerpTime() > 0L) {
            this.lerpSizeBetween(properties.getSize(), properties.getSizeLerpTarget(), properties.getSizeLerpTime());
        } else {
            this.setSize(properties.getSize());
        }

    }

    interface BorderExtent {
        double getMinX();

        double getMaxX();

        double getMinZ();

        double getMaxZ();

        double getSize();

        double getLerpSpeed();

        long getLerpRemainingTime();

        double getLerpTarget();

        BorderStatus getStatus();

        void onAbsoluteMaxSizeChange();

        void onCenterChange();

        WorldBorder.BorderExtent update();

        VoxelShape getCollisionShape();
    }

    class MovingBorderExtent implements WorldBorder.BorderExtent {
        private final double from;
        private final double to;
        private final long lerpEnd;
        private final long lerpBegin;
        private final double lerpDuration;

        MovingBorderExtent(double oldSize, double newSize, long timeDuration) {
            this.from = oldSize;
            this.to = newSize;
            this.lerpDuration = (double)timeDuration;
            this.lerpBegin = Util.getMillis();
            this.lerpEnd = this.lerpBegin + timeDuration;
        }

        @Override
        public double getMinX() {
            return Mth.clamp(WorldBorder.this.getCenterX() - this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
        }

        @Override
        public double getMinZ() {
            return Mth.clamp(WorldBorder.this.getCenterZ() - this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
        }

        @Override
        public double getMaxX() {
            return Mth.clamp(WorldBorder.this.getCenterX() + this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
        }

        @Override
        public double getMaxZ() {
            return Mth.clamp(WorldBorder.this.getCenterZ() + this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
        }

        @Override
        public double getSize() {
            double d = (double)(Util.getMillis() - this.lerpBegin) / this.lerpDuration;
            return d < 1.0D ? Mth.lerp(d, this.from, this.to) : this.to;
        }

        @Override
        public double getLerpSpeed() {
            return Math.abs(this.from - this.to) / (double)(this.lerpEnd - this.lerpBegin);
        }

        @Override
        public long getLerpRemainingTime() {
            return this.lerpEnd - Util.getMillis();
        }

        @Override
        public double getLerpTarget() {
            return this.to;
        }

        @Override
        public BorderStatus getStatus() {
            return this.to < this.from ? BorderStatus.SHRINKING : BorderStatus.GROWING;
        }

        @Override
        public void onCenterChange() {
        }

        @Override
        public void onAbsoluteMaxSizeChange() {
        }

        @Override
        public WorldBorder.BorderExtent update() {
            return (WorldBorder.BorderExtent)(this.getLerpRemainingTime() <= 0L ? WorldBorder.this.new StaticBorderExtent(this.to) : this);
        }

        @Override
        public VoxelShape getCollisionShape() {
            return Shapes.join(Shapes.INFINITY, Shapes.box(Math.floor(this.getMinX()), Double.NEGATIVE_INFINITY, Math.floor(this.getMinZ()), Math.ceil(this.getMaxX()), Double.POSITIVE_INFINITY, Math.ceil(this.getMaxZ())), BooleanOp.ONLY_FIRST);
        }
    }

    public static class Settings {
        private final double centerX;
        private final double centerZ;
        private final double damagePerBlock;
        private final double safeZone;
        private final int warningBlocks;
        private final int warningTime;
        private final double size;
        private final long sizeLerpTime;
        private final double sizeLerpTarget;

        Settings(double centerX, double centerZ, double damagePerBlock, double safeZone, int warningBlocks, int warningTime, double size, long sizeLerpTime, double sizeLerpTarget) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.damagePerBlock = damagePerBlock;
            this.safeZone = safeZone;
            this.warningBlocks = warningBlocks;
            this.warningTime = warningTime;
            this.size = size;
            this.sizeLerpTime = sizeLerpTime;
            this.sizeLerpTarget = sizeLerpTarget;
        }

        Settings(WorldBorder worldBorder) {
            this.centerX = worldBorder.getCenterX();
            this.centerZ = worldBorder.getCenterZ();
            this.damagePerBlock = worldBorder.getDamagePerBlock();
            this.safeZone = worldBorder.getDamageSafeZone();
            this.warningBlocks = worldBorder.getWarningBlocks();
            this.warningTime = worldBorder.getWarningTime();
            this.size = worldBorder.getSize();
            this.sizeLerpTime = worldBorder.getLerpRemainingTime();
            this.sizeLerpTarget = worldBorder.getLerpTarget();
        }

        public double getCenterX() {
            return this.centerX;
        }

        public double getCenterZ() {
            return this.centerZ;
        }

        public double getDamagePerBlock() {
            return this.damagePerBlock;
        }

        public double getSafeZone() {
            return this.safeZone;
        }

        public int getWarningBlocks() {
            return this.warningBlocks;
        }

        public int getWarningTime() {
            return this.warningTime;
        }

        public double getSize() {
            return this.size;
        }

        public long getSizeLerpTime() {
            return this.sizeLerpTime;
        }

        public double getSizeLerpTarget() {
            return this.sizeLerpTarget;
        }

        public static WorldBorder.Settings read(DynamicLike<?> dynamic, WorldBorder.Settings properties) {
            double d = Mth.clamp(dynamic.get("BorderCenterX").asDouble(properties.centerX), -2.9999984E7D, 2.9999984E7D);
            double e = Mth.clamp(dynamic.get("BorderCenterZ").asDouble(properties.centerZ), -2.9999984E7D, 2.9999984E7D);
            double f = dynamic.get("BorderSize").asDouble(properties.size);
            long l = dynamic.get("BorderSizeLerpTime").asLong(properties.sizeLerpTime);
            double g = dynamic.get("BorderSizeLerpTarget").asDouble(properties.sizeLerpTarget);
            double h = dynamic.get("BorderSafeZone").asDouble(properties.safeZone);
            double i = dynamic.get("BorderDamagePerBlock").asDouble(properties.damagePerBlock);
            int j = dynamic.get("BorderWarningBlocks").asInt(properties.warningBlocks);
            int k = dynamic.get("BorderWarningTime").asInt(properties.warningTime);
            return new WorldBorder.Settings(d, e, i, h, j, k, f, l, g);
        }

        public void write(CompoundTag nbt) {
            nbt.putDouble("BorderCenterX", this.centerX);
            nbt.putDouble("BorderCenterZ", this.centerZ);
            nbt.putDouble("BorderSize", this.size);
            nbt.putLong("BorderSizeLerpTime", this.sizeLerpTime);
            nbt.putDouble("BorderSafeZone", this.safeZone);
            nbt.putDouble("BorderDamagePerBlock", this.damagePerBlock);
            nbt.putDouble("BorderSizeLerpTarget", this.sizeLerpTarget);
            nbt.putDouble("BorderWarningBlocks", (double)this.warningBlocks);
            nbt.putDouble("BorderWarningTime", (double)this.warningTime);
        }
    }

    class StaticBorderExtent implements WorldBorder.BorderExtent {
        private final double size;
        private double minX;
        private double minZ;
        private double maxX;
        private double maxZ;
        private VoxelShape shape;

        public StaticBorderExtent(double size) {
            this.size = size;
            this.updateBox();
        }

        @Override
        public double getMinX() {
            return this.minX;
        }

        @Override
        public double getMaxX() {
            return this.maxX;
        }

        @Override
        public double getMinZ() {
            return this.minZ;
        }

        @Override
        public double getMaxZ() {
            return this.maxZ;
        }

        @Override
        public double getSize() {
            return this.size;
        }

        @Override
        public BorderStatus getStatus() {
            return BorderStatus.STATIONARY;
        }

        @Override
        public double getLerpSpeed() {
            return 0.0D;
        }

        @Override
        public long getLerpRemainingTime() {
            return 0L;
        }

        @Override
        public double getLerpTarget() {
            return this.size;
        }

        private void updateBox() {
            this.minX = Mth.clamp(WorldBorder.this.getCenterX() - this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
            this.minZ = Mth.clamp(WorldBorder.this.getCenterZ() - this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
            this.maxX = Mth.clamp(WorldBorder.this.getCenterX() + this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
            this.maxZ = Mth.clamp(WorldBorder.this.getCenterZ() + this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
            this.shape = Shapes.join(Shapes.INFINITY, Shapes.box(Math.floor(this.getMinX()), Double.NEGATIVE_INFINITY, Math.floor(this.getMinZ()), Math.ceil(this.getMaxX()), Double.POSITIVE_INFINITY, Math.ceil(this.getMaxZ())), BooleanOp.ONLY_FIRST);
        }

        @Override
        public void onAbsoluteMaxSizeChange() {
            this.updateBox();
        }

        @Override
        public void onCenterChange() {
            this.updateBox();
        }

        @Override
        public WorldBorder.BorderExtent update() {
            return this;
        }

        @Override
        public VoxelShape getCollisionShape() {
            return this.shape;
        }
    }
}
