package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

public class CipherBase {
    private final Cipher cipher;
    private byte[] heapIn = new byte[0];
    private byte[] heapOut = new byte[0];

    protected CipherBase(Cipher cipher) {
        this.cipher = cipher;
    }

    private byte[] bufToByte(ByteBuf buf) {
        int i = buf.readableBytes();
        if (this.heapIn.length < i) {
            this.heapIn = new byte[i];
        }

        buf.readBytes(this.heapIn, 0, i);
        return this.heapIn;
    }

    protected ByteBuf decipher(ChannelHandlerContext context, ByteBuf buf) throws ShortBufferException {
        int i = buf.readableBytes();
        byte[] bs = this.bufToByte(buf);
        ByteBuf byteBuf = context.alloc().heapBuffer(this.cipher.getOutputSize(i));
        byteBuf.writerIndex(this.cipher.update(bs, 0, i, byteBuf.array(), byteBuf.arrayOffset()));
        return byteBuf;
    }

    protected void encipher(ByteBuf buf, ByteBuf result) throws ShortBufferException {
        int i = buf.readableBytes();
        byte[] bs = this.bufToByte(buf);
        int j = this.cipher.getOutputSize(i);
        if (this.heapOut.length < j) {
            this.heapOut = new byte[j];
        }

        result.writeBytes(this.heapOut, 0, this.cipher.update(bs, 0, i, this.heapOut));
    }
}
