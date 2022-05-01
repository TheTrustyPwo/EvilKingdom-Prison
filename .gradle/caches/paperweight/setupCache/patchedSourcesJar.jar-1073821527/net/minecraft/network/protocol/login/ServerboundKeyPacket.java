package net.minecraft.network.protocol.login;

import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;

public class ServerboundKeyPacket implements Packet<ServerLoginPacketListener> {
    private final byte[] keybytes;
    private final byte[] nonce;

    public ServerboundKeyPacket(SecretKey secretKey, PublicKey publicKey, byte[] nonce) throws CryptException {
        this.keybytes = Crypt.encryptUsingKey(publicKey, secretKey.getEncoded());
        this.nonce = Crypt.encryptUsingKey(publicKey, nonce);
    }

    public ServerboundKeyPacket(FriendlyByteBuf buf) {
        this.keybytes = buf.readByteArray();
        this.nonce = buf.readByteArray();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByteArray(this.keybytes);
        buf.writeByteArray(this.nonce);
    }

    @Override
    public void handle(ServerLoginPacketListener listener) {
        listener.handleKey(this);
    }

    public SecretKey getSecretKey(PrivateKey privateKey) throws CryptException {
        return Crypt.decryptByteToSecretKey(privateKey, this.keybytes);
    }

    public byte[] getNonce(PrivateKey privateKey) throws CryptException {
        return Crypt.decryptUsingKey(privateKey, this.nonce);
    }
}
