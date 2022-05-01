package net.minecraft.util;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {
    private static final String SYMMETRIC_ALGORITHM = "AES";
    private static final int SYMMETRIC_BITS = 128;
    private static final String ASYMMETRIC_ALGORITHM = "RSA";
    private static final int ASYMMETRIC_BITS = 1024;
    private static final String BYTE_ENCODING = "ISO_8859_1";
    private static final String HASH_ALGORITHM = "SHA-1";

    public static SecretKey generateSecretKey() throws CryptException {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey();
        } catch (Exception var1) {
            throw new CryptException(var1);
        }
    }

    public static KeyPair generateKeyPair() throws CryptException {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception var1) {
            throw new CryptException(var1);
        }
    }

    public static byte[] digestData(String baseServerId, PublicKey publicKey, SecretKey secretKey) throws CryptException {
        try {
            return digestData(baseServerId.getBytes("ISO_8859_1"), secretKey.getEncoded(), publicKey.getEncoded());
        } catch (Exception var4) {
            throw new CryptException(var4);
        }
    }

    private static byte[] digestData(byte[]... bytes) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");

        for(byte[] bs : bytes) {
            messageDigest.update(bs);
        }

        return messageDigest.digest();
    }

    public static PublicKey byteToPublicKey(byte[] bytes) throws CryptException {
        try {
            EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(bytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(encodedKeySpec);
        } catch (Exception var3) {
            throw new CryptException(var3);
        }
    }

    public static SecretKey decryptByteToSecretKey(PrivateKey privateKey, byte[] encryptedSecretKey) throws CryptException {
        byte[] bs = decryptUsingKey(privateKey, encryptedSecretKey);

        try {
            return new SecretKeySpec(bs, "AES");
        } catch (Exception var4) {
            throw new CryptException(var4);
        }
    }

    public static byte[] encryptUsingKey(Key key, byte[] data) throws CryptException {
        return cipherData(1, key, data);
    }

    public static byte[] decryptUsingKey(Key key, byte[] data) throws CryptException {
        return cipherData(2, key, data);
    }

    private static byte[] cipherData(int opMode, Key key, byte[] data) throws CryptException {
        try {
            return setupCipher(opMode, key.getAlgorithm(), key).doFinal(data);
        } catch (Exception var4) {
            throw new CryptException(var4);
        }
    }

    private static Cipher setupCipher(int opMode, String algorithm, Key key) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(opMode, key);
        return cipher;
    }

    public static Cipher getCipher(int opMode, Key key) throws CryptException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            cipher.init(opMode, key, new IvParameterSpec(key.getEncoded()));
            return cipher;
        } catch (Exception var3) {
            throw new CryptException(var3);
        }
    }
}
