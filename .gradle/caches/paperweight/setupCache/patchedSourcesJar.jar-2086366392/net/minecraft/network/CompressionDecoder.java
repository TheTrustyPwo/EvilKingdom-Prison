package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.zip.Inflater;

public class CompressionDecoder extends ByteToMessageDecoder {
    public static final int MAXIMUM_COMPRESSED_LENGTH = 2097152;
    public static final int MAXIMUM_UNCOMPRESSED_LENGTH = 8388608;
    private final Inflater inflater;
    private final com.velocitypowered.natives.compression.VelocityCompressor compressor; // Paper
    private int threshold;
    private boolean validateDecompressed;

    // Paper start
    public CompressionDecoder(int compressionThreshold, boolean rejectsBadPackets) {
        this(null, compressionThreshold, rejectsBadPackets);
    }
    public CompressionDecoder(com.velocitypowered.natives.compression.VelocityCompressor compressor, int compressionThreshold, boolean rejectsBadPackets) {
        this.threshold = compressionThreshold;
        this.validateDecompressed = rejectsBadPackets;
        this.inflater = compressor == null ? new Inflater() : null;
        this.compressor = compressor;
        // Paper end
    }

    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() != 0) {
            FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(byteBuf);
            int i = friendlyByteBuf.readVarInt();
            if (i == 0) {
                list.add(friendlyByteBuf.readBytes(friendlyByteBuf.readableBytes()));
            } else {
                if (this.validateDecompressed) {
                    if (i < this.threshold) {
                        throw new DecoderException("Badly compressed packet - size of " + i + " is below server threshold of " + this.threshold);
                    }

                    if (i > 8388608) {
                        throw new DecoderException("Badly compressed packet - size of " + i + " is larger than protocol maximum of 8388608");
                    }
                }

                // Paper start
                if (this.inflater != null) {
                byte[] bs = new byte[friendlyByteBuf.readableBytes()];
                friendlyByteBuf.readBytes(bs);
                this.inflater.setInput(bs);
                byte[] cs = new byte[i];
                this.inflater.inflate(cs);
                list.add(Unpooled.wrappedBuffer(cs));
                this.inflater.reset();
                    return;
                }

                int claimedUncompressedSize = i; // OBFHELPER
                ByteBuf compatibleIn = com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible(channelHandlerContext.alloc(), this.compressor, byteBuf);
                ByteBuf uncompressed = com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer(channelHandlerContext.alloc(), this.compressor, claimedUncompressedSize);
                try {
                    this.compressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
                    list.add(uncompressed);
                    byteBuf.clear();
                } catch (Exception e) {
                    uncompressed.release();
                    throw e;
                } finally {
                    compatibleIn.release();
                }
                // Paper end
            }
        }
    }

    // Paper start
    @Override
    public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
        if (this.compressor != null) {
            this.compressor.close();
        }
    }
    // Paper end

    public void setThreshold(int compressionThreshold, boolean rejectsBadPackets) {
        this.threshold = compressionThreshold;
        this.validateDecompressed = rejectsBadPackets;
    }
}
