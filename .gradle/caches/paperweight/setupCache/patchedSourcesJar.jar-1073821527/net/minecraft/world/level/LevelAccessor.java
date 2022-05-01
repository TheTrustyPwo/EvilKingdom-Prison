package net.minecraft.world.level;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

public interface LevelAccessor extends CommonLevelAccessor, LevelTimeAccess {
    @Override
    default long dayTime() {
        return this.getLevelData().getDayTime();
    }

    long nextSubTickCount();

    LevelTickAccess<Block> getBlockTicks();

    private <T> ScheduledTick<T> createTick(BlockPos pos, T type, int delay, TickPriority priority) {
        return new ScheduledTick<>(type, pos, this.getLevelData().getGameTime() + (long)delay, priority, this.nextSubTickCount());
    }

    private <T> ScheduledTick<T> createTick(BlockPos pos, T type, int delay) {
        return new ScheduledTick<>(type, pos, this.getLevelData().getGameTime() + (long)delay, this.nextSubTickCount());
    }

    default void scheduleTick(BlockPos pos, Block block, int delay, TickPriority priority) {
        this.getBlockTicks().schedule(this.createTick(pos, block, delay, priority));
    }

    default void scheduleTick(BlockPos pos, Block block, int delay) {
        this.getBlockTicks().schedule(this.createTick(pos, block, delay));
    }

    LevelTickAccess<Fluid> getFluidTicks();

    default void scheduleTick(BlockPos pos, Fluid fluid, int delay, TickPriority priority) {
        this.getFluidTicks().schedule(this.createTick(pos, fluid, delay, priority));
    }

    default void scheduleTick(BlockPos pos, Fluid fluid, int delay) {
        this.getFluidTicks().schedule(this.createTick(pos, fluid, delay));
    }

    LevelData getLevelData();

    DifficultyInstance getCurrentDifficultyAt(BlockPos pos);

    @Nullable
    MinecraftServer getServer();

    default Difficulty getDifficulty() {
        return this.getLevelData().getDifficulty();
    }

    ChunkSource getChunkSource();

    @Override
    default boolean hasChunk(int chunkX, int chunkZ) {
        return this.getChunkSource().hasChunk(chunkX, chunkZ);
    }

    Random getRandom();

    default void blockUpdated(BlockPos pos, Block block) {
    }

    void playSound(@Nullable Player player, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch);

    void addParticle(ParticleOptions parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ);

    void levelEvent(@Nullable Player player, int eventId, BlockPos pos, int data);

    default void levelEvent(int eventId, BlockPos pos, int data) {
        this.levelEvent((Player)null, eventId, pos, data);
    }

    void gameEvent(@Nullable Entity entity, GameEvent event, BlockPos pos);

    default void gameEvent(GameEvent event, BlockPos pos) {
        this.gameEvent((Entity)null, event, pos);
    }

    default void gameEvent(GameEvent event, Entity emitter) {
        this.gameEvent((Entity)null, event, emitter.blockPosition());
    }

    default void gameEvent(@Nullable Entity entity, GameEvent event, Entity emitter) {
        this.gameEvent(entity, event, emitter.blockPosition());
    }
}
