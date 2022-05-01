package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.datafixers.util.Unit;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ResetChunksCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("resetchunks").requires((source) -> {
            return source.hasPermission(2);
        }).executes((context) -> {
            return resetChunks(context.getSource(), 0, true);
        }).then(Commands.argument("range", IntegerArgumentType.integer(0, 5)).executes((context) -> {
            return resetChunks(context.getSource(), IntegerArgumentType.getInteger(context, "range"), true);
        }).then(Commands.argument("skipOldChunks", BoolArgumentType.bool()).executes((commandContext) -> {
            return resetChunks(commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "range"), BoolArgumentType.getBool(commandContext, "skipOldChunks"));
        }))));
    }

    private static int resetChunks(CommandSourceStack source, int radius, boolean skipOldChunks) {
        ServerLevel serverLevel = source.getLevel();
        ServerChunkCache serverChunkCache = serverLevel.getChunkSource();
        serverChunkCache.chunkMap.debugReloadGenerator();
        Vec3 vec3 = source.getPosition();
        ChunkPos chunkPos = new ChunkPos(new BlockPos(vec3));
        int i = chunkPos.z - radius;
        int j = chunkPos.z + radius;
        int k = chunkPos.x - radius;
        int l = chunkPos.x + radius;

        for(int m = i; m <= j; ++m) {
            for(int n = k; n <= l; ++n) {
                ChunkPos chunkPos2 = new ChunkPos(n, m);
                LevelChunk levelChunk = serverChunkCache.getChunk(n, m, false);
                if (levelChunk != null && (!skipOldChunks || !levelChunk.isOldNoiseGeneration())) {
                    for(BlockPos blockPos : BlockPos.betweenClosed(chunkPos2.getMinBlockX(), serverLevel.getMinBuildHeight(), chunkPos2.getMinBlockZ(), chunkPos2.getMaxBlockX(), serverLevel.getMaxBuildHeight() - 1, chunkPos2.getMaxBlockZ())) {
                        serverLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 16);
                    }
                }
            }
        }

        ProcessorMailbox<Runnable> processorMailbox = ProcessorMailbox.create(Util.backgroundExecutor(), "worldgen-resetchunks");
        long o = System.currentTimeMillis();
        int p = (radius * 2 + 1) * (radius * 2 + 1);

        for(ChunkStatus chunkStatus : ImmutableList.of(ChunkStatus.BIOMES, ChunkStatus.NOISE, ChunkStatus.SURFACE, ChunkStatus.CARVERS, ChunkStatus.LIQUID_CARVERS, ChunkStatus.FEATURES)) {
            long q = System.currentTimeMillis();
            CompletableFuture<Unit> completableFuture = CompletableFuture.supplyAsync(() -> {
                return Unit.INSTANCE;
            }, processorMailbox::tell);

            for(int r = chunkPos.z - radius; r <= chunkPos.z + radius; ++r) {
                for(int s = chunkPos.x - radius; s <= chunkPos.x + radius; ++s) {
                    ChunkPos chunkPos3 = new ChunkPos(s, r);
                    LevelChunk levelChunk2 = serverChunkCache.getChunk(s, r, false);
                    if (levelChunk2 != null && (!skipOldChunks || !levelChunk2.isOldNoiseGeneration())) {
                        List<ChunkAccess> list = Lists.newArrayList();
                        int t = Math.max(1, chunkStatus.getRange());

                        for(int u = chunkPos3.z - t; u <= chunkPos3.z + t; ++u) {
                            for(int v = chunkPos3.x - t; v <= chunkPos3.x + t; ++v) {
                                ChunkAccess chunkAccess = serverChunkCache.getChunk(v, u, chunkStatus.getParent(), true);
                                ChunkAccess chunkAccess2;
                                if (chunkAccess instanceof ImposterProtoChunk) {
                                    chunkAccess2 = new ImposterProtoChunk(((ImposterProtoChunk)chunkAccess).getWrapped(), true);
                                } else if (chunkAccess instanceof LevelChunk) {
                                    chunkAccess2 = new ImposterProtoChunk((LevelChunk)chunkAccess, true);
                                } else {
                                    chunkAccess2 = chunkAccess;
                                }

                                list.add(chunkAccess2);
                            }
                        }

                        completableFuture = completableFuture.thenComposeAsync((unit) -> {
                            return chunkStatus.generate(processorMailbox::tell, serverLevel, serverChunkCache.getGenerator(), serverLevel.getStructureManager(), serverChunkCache.getLightEngine(), (chunk) -> {
                                throw new UnsupportedOperationException("Not creating full chunks here");
                            }, list, true).thenApply((either) -> {
                                if (chunkStatus == ChunkStatus.NOISE) {
                                    either.left().ifPresent((chunk) -> {
                                        Heightmap.primeHeightmaps(chunk, ChunkStatus.POST_FEATURES);
                                    });
                                }

                                return Unit.INSTANCE;
                            });
                        }, processorMailbox::tell);
                    }
                }
            }

            source.getServer().managedBlock(completableFuture::isDone);
            LOGGER.debug(chunkStatus.getName() + " took " + (System.currentTimeMillis() - q) + " ms");
        }

        long w = System.currentTimeMillis();

        for(int x = chunkPos.z - radius; x <= chunkPos.z + radius; ++x) {
            for(int y = chunkPos.x - radius; y <= chunkPos.x + radius; ++y) {
                ChunkPos chunkPos4 = new ChunkPos(y, x);
                LevelChunk levelChunk3 = serverChunkCache.getChunk(y, x, false);
                if (levelChunk3 != null && (!skipOldChunks || !levelChunk3.isOldNoiseGeneration())) {
                    for(BlockPos blockPos2 : BlockPos.betweenClosed(chunkPos4.getMinBlockX(), serverLevel.getMinBuildHeight(), chunkPos4.getMinBlockZ(), chunkPos4.getMaxBlockX(), serverLevel.getMaxBuildHeight() - 1, chunkPos4.getMaxBlockZ())) {
                        serverChunkCache.blockChanged(blockPos2);
                    }
                }
            }
        }

        LOGGER.debug("blockChanged took " + (System.currentTimeMillis() - w) + " ms");
        long z = System.currentTimeMillis() - o;
        source.sendSuccess(new TextComponent(String.format("%d chunks have been reset. This took %d ms for %d chunks, or %02f ms per chunk", p, z, p, (float)z / (float)p)), true);
        return 1;
    }
}
