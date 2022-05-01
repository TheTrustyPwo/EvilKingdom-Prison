package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class TheEndGatewayBlockEntity extends TheEndPortalBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SPAWN_TIME = 200;
    private static final int COOLDOWN_TIME = 40;
    private static final int ATTENTION_INTERVAL = 2400;
    private static final int EVENT_COOLDOWN = 1;
    private static final int GATEWAY_HEIGHT_ABOVE_SURFACE = 10;
    public long age;
    private int teleportCooldown;
    @Nullable
    public BlockPos exitPortal;
    public boolean exactTeleport;

    public TheEndGatewayBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.END_GATEWAY, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("Age", this.age);
        if (this.exitPortal != null) {
            nbt.put("ExitPortal", NbtUtils.writeBlockPos(this.exitPortal));
        }

        if (this.exactTeleport) {
            nbt.putBoolean("ExactTeleport", true);
        }

    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.age = nbt.getLong("Age");
        if (nbt.contains("ExitPortal", 10)) {
            BlockPos blockPos = NbtUtils.readBlockPos(nbt.getCompound("ExitPortal"));
            if (Level.isInSpawnableBounds(blockPos)) {
                this.exitPortal = blockPos;
            }
        }

        this.exactTeleport = nbt.getBoolean("ExactTeleport");
    }

    public static void beamAnimationTick(Level world, BlockPos pos, BlockState state, TheEndGatewayBlockEntity blockEntity) {
        ++blockEntity.age;
        if (blockEntity.isCoolingDown()) {
            --blockEntity.teleportCooldown;
        }

    }

    public static void teleportTick(Level world, BlockPos pos, BlockState state, TheEndGatewayBlockEntity blockEntity) {
        boolean bl = blockEntity.isSpawning();
        boolean bl2 = blockEntity.isCoolingDown();
        ++blockEntity.age;
        if (bl2) {
            --blockEntity.teleportCooldown;
        } else {
            List<Entity> list = world.getEntitiesOfClass(Entity.class, new AABB(pos), TheEndGatewayBlockEntity::canEntityTeleport);
            if (!list.isEmpty()) {
                teleportEntity(world, pos, state, list.get(world.random.nextInt(list.size())), blockEntity);
            }

            if (blockEntity.age % 2400L == 0L) {
                triggerCooldown(world, pos, state, blockEntity);
            }
        }

        if (bl != blockEntity.isSpawning() || bl2 != blockEntity.isCoolingDown()) {
            setChanged(world, pos, state);
        }

    }

    public static boolean canEntityTeleport(Entity entity) {
        return EntitySelector.NO_SPECTATORS.test(entity) && !entity.getRootVehicle().isOnPortalCooldown();
    }

    public boolean isSpawning() {
        return this.age < 200L;
    }

    public boolean isCoolingDown() {
        return this.teleportCooldown > 0;
    }

    public float getSpawnPercent(float tickDelta) {
        return Mth.clamp(((float)this.age + tickDelta) / 200.0F, 0.0F, 1.0F);
    }

    public float getCooldownPercent(float tickDelta) {
        return 1.0F - Mth.clamp(((float)this.teleportCooldown - tickDelta) / 40.0F, 0.0F, 1.0F);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    private static void triggerCooldown(Level world, BlockPos pos, BlockState state, TheEndGatewayBlockEntity blockEntity) {
        if (!world.isClientSide) {
            blockEntity.teleportCooldown = 40;
            world.blockEvent(pos, state.getBlock(), 1, 0);
            setChanged(world, pos, state);
        }

    }

    @Override
    public boolean triggerEvent(int type, int data) {
        if (type == 1) {
            this.teleportCooldown = 40;
            return true;
        } else {
            return super.triggerEvent(type, data);
        }
    }

    public static void teleportEntity(Level world, BlockPos pos, BlockState state, Entity entity, TheEndGatewayBlockEntity blockEntity) {
        if (world instanceof ServerLevel && !blockEntity.isCoolingDown()) {
            ServerLevel serverLevel = (ServerLevel)world;
            blockEntity.teleportCooldown = 100;
            if (blockEntity.exitPortal == null && world.dimension() == Level.END) {
                BlockPos blockPos = findOrCreateValidTeleportPos(serverLevel, pos);
                blockPos = blockPos.above(10);
                LOGGER.debug("Creating portal at {}", (Object)blockPos);
                spawnGatewayPortal(serverLevel, blockPos, EndGatewayConfiguration.knownExit(pos, false));
                blockEntity.exitPortal = blockPos;
            }

            if (blockEntity.exitPortal != null) {
                BlockPos blockPos2 = blockEntity.exactTeleport ? blockEntity.exitPortal : findExitPosition(world, blockEntity.exitPortal);
                Entity entity3;
                if (entity instanceof ThrownEnderpearl) {
                    Entity entity2 = ((ThrownEnderpearl)entity).getOwner();
                    if (entity2 instanceof ServerPlayer) {
                        CriteriaTriggers.ENTER_BLOCK.trigger((ServerPlayer)entity2, state);
                    }

                    if (entity2 != null) {
                        entity3 = entity2;
                        entity.discard();
                    } else {
                        entity3 = entity;
                    }
                } else {
                    entity3 = entity.getRootVehicle();
                }

                entity3.setPortalCooldown();
                entity3.teleportToWithTicket((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY(), (double)blockPos2.getZ() + 0.5D);
            }

            triggerCooldown(world, pos, state, blockEntity);
        }
    }

    private static BlockPos findExitPosition(Level world, BlockPos pos) {
        BlockPos blockPos = findTallestBlock(world, pos.offset(0, 2, 0), 5, false);
        LOGGER.debug("Best exit position for portal at {} is {}", pos, blockPos);
        return blockPos.above();
    }

    private static BlockPos findOrCreateValidTeleportPos(ServerLevel world, BlockPos pos) {
        Vec3 vec3 = findExitPortalXZPosTentative(world, pos);
        LevelChunk levelChunk = getChunk(world, vec3);
        BlockPos blockPos = findValidSpawnInChunk(levelChunk);
        if (blockPos == null) {
            blockPos = new BlockPos(vec3.x + 0.5D, 75.0D, vec3.z + 0.5D);
            LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", (Object)blockPos);
            EndFeatures.END_ISLAND.value().place(world, world.getChunkSource().getGenerator(), new Random(blockPos.asLong()), blockPos);
        } else {
            LOGGER.debug("Found suitable block to teleport to: {}", (Object)blockPos);
        }

        return findTallestBlock(world, blockPos, 16, true);
    }

    private static Vec3 findExitPortalXZPosTentative(ServerLevel world, BlockPos pos) {
        Vec3 vec3 = (new Vec3((double)pos.getX(), 0.0D, (double)pos.getZ())).normalize();
        int i = 1024;
        Vec3 vec32 = vec3.scale(1024.0D);

        for(int j = 16; !isChunkEmpty(world, vec32) && j-- > 0; vec32 = vec32.add(vec3.scale(-16.0D))) {
            LOGGER.debug("Skipping backwards past nonempty chunk at {}", (Object)vec32);
        }

        for(int var6 = 16; isChunkEmpty(world, vec32) && var6-- > 0; vec32 = vec32.add(vec3.scale(16.0D))) {
            LOGGER.debug("Skipping forward past empty chunk at {}", (Object)vec32);
        }

        LOGGER.debug("Found chunk at {}", (Object)vec32);
        return vec32;
    }

    private static boolean isChunkEmpty(ServerLevel world, Vec3 pos) {
        return getChunk(world, pos).getHighestSectionPosition() <= world.getMinBuildHeight();
    }

    private static BlockPos findTallestBlock(BlockGetter world, BlockPos pos, int searchRadius, boolean force) {
        BlockPos blockPos = null;

        for(int i = -searchRadius; i <= searchRadius; ++i) {
            for(int j = -searchRadius; j <= searchRadius; ++j) {
                if (i != 0 || j != 0 || force) {
                    for(int k = world.getMaxBuildHeight() - 1; k > (blockPos == null ? world.getMinBuildHeight() : blockPos.getY()); --k) {
                        BlockPos blockPos2 = new BlockPos(pos.getX() + i, k, pos.getZ() + j);
                        BlockState blockState = world.getBlockState(blockPos2);
                        if (blockState.isCollisionShapeFullBlock(world, blockPos2) && (force || !blockState.is(Blocks.BEDROCK))) {
                            blockPos = blockPos2;
                            break;
                        }
                    }
                }
            }
        }

        return blockPos == null ? pos : blockPos;
    }

    private static LevelChunk getChunk(Level world, Vec3 pos) {
        return world.getChunk(Mth.floor(pos.x / 16.0D), Mth.floor(pos.z / 16.0D));
    }

    @Nullable
    private static BlockPos findValidSpawnInChunk(LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos blockPos = new BlockPos(chunkPos.getMinBlockX(), 30, chunkPos.getMinBlockZ());
        int i = chunk.getHighestSectionPosition() + 16 - 1;
        BlockPos blockPos2 = new BlockPos(chunkPos.getMaxBlockX(), i, chunkPos.getMaxBlockZ());
        BlockPos blockPos3 = null;
        double d = 0.0D;

        for(BlockPos blockPos4 : BlockPos.betweenClosed(blockPos, blockPos2)) {
            BlockState blockState = chunk.getBlockState(blockPos4);
            BlockPos blockPos5 = blockPos4.above();
            BlockPos blockPos6 = blockPos4.above(2);
            if (blockState.is(Blocks.END_STONE) && !chunk.getBlockState(blockPos5).isCollisionShapeFullBlock(chunk, blockPos5) && !chunk.getBlockState(blockPos6).isCollisionShapeFullBlock(chunk, blockPos6)) {
                double e = blockPos4.distToCenterSqr(0.0D, 0.0D, 0.0D);
                if (blockPos3 == null || e < d) {
                    blockPos3 = blockPos4;
                    d = e;
                }
            }
        }

        return blockPos3;
    }

    private static void spawnGatewayPortal(ServerLevel world, BlockPos pos, EndGatewayConfiguration config) {
        Feature.END_GATEWAY.place(config, world, world.getChunkSource().getGenerator(), new Random(), pos);
    }

    @Override
    public boolean shouldRenderFace(Direction direction) {
        return Block.shouldRenderFace(this.getBlockState(), this.level, this.getBlockPos(), direction, this.getBlockPos().relative(direction));
    }

    public int getParticleAmount() {
        int i = 0;

        for(Direction direction : Direction.values()) {
            i += this.shouldRenderFace(direction) ? 1 : 0;
        }

        return i;
    }

    public void setExitPosition(BlockPos pos, boolean exactTeleport) {
        this.exactTeleport = exactTeleport;
        this.exitPortal = pos;
    }
}
