package com.armongate.mobilepasssdk.manager;

import android.util.Base64;

import com.armongate.mobilepasssdk.model.CryptoKeyPair;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECParameterSpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

public class CryptoManager {

    // Singleton

    private static CryptoManager instance = null;
    private CryptoManager() {
        this.iv = new SecureRandom().generateSeed(16);

        try {
            P256_HEAD = createHeadForNamedCurve("NIST P-256", 256);
        } catch (Exception ex) {
            LogManager.getInstance().error("Creating P256_HEAD failed: " + ex.getLocalizedMessage());
        }
    }

    public static CryptoManager getInstance() {
        if (instance == null) {
            instance = new CryptoManager();
        }

        return instance;
    }

    // Private Fields

    private byte[] iv;
    private static byte[] P256_HEAD = new byte[0];

    static {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    // Private Functions

    private static byte[] createHeadForNamedCurve(String name, int size) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator generator  = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec   = new ECGenParameterSpec(name);

        generator.initialize(ecSpec);

        KeyPair keyPair = generator.generateKeyPair();

        byte[] encoded = keyPair.getPublic().getEncoded();
        return Arrays.copyOf(encoded, encoded.length - 2 * (size / Byte.SIZE) - 1);
    }

    private PublicKey createPublicKeyWithP256Head(String publicKeyBase64WithNoHead, KeyFactory keyFactory) throws InvalidKeySpecException {
        byte[] publicKeyWithNoHead  = Base64.decode(publicKeyBase64WithNoHead, Base64.NO_WRAP);
        byte[] encodedKey           = new byte[P256_HEAD.length + publicKeyWithNoHead.length];

        System.arraycopy(P256_HEAD, 0, encodedKey, 0, P256_HEAD.length);
        System.arraycopy(publicKeyWithNoHead, 0, encodedKey, P256_HEAD.length, publicKeyWithNoHead.length);

        X509EncodedKeySpec ecpks = new X509EncodedKeySpec(encodedKey);
        return keyFactory.generatePublic(ecpks);
    }

    private SecretKey generateSharedSecret(PrivateKey privateKey, PublicKey publicKey) throws InvalidKeyException, NoSuchAlgorithmException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");

        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);

        return keyAgreement.generateSecret("AES");
    }

    private SecretKey getSecretKey(String privateKey, String publicKey) {
        // public key should be 'base64 with no head'

        try {
            byte[] privateKeyBytes = Base64.decode(privateKey, Base64.NO_WRAP);

            KeyFactory factory = KeyFactory.getInstance("ECDSA", "SC");

            PrivateKey factoryPrivateKey   = factory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            PublicKey factoryPublicKey    = createPublicKeyWithP256Head(publicKey, factory);

            return generateSharedSecret(factoryPrivateKey, factoryPublicKey);
        } catch (Exception ex) {
            LogManager.getInstance().error("Generate secret key failed with error: " + ex.getLocalizedMessage());
            return null;
        }
    }

    private static byte[] getEncryptedBytes(byte[] plain, CipherParameters ivAndKey) throws Exception {
        return getAESEncryptionData(plain, true, ivAndKey);
    }

    private static byte[] getDecryptedBytes(byte[] cipher, CipherParameters ivAndKey) throws Exception {
        return getAESEncryptionData(cipher, false, ivAndKey);
    }

    private static byte[] getAESEncryptionData(byte[] data, boolean forEncryption, CipherParameters ivAndKey) throws Exception {
        PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(
                        new AESEngine()
                )
        );

        aes.init(forEncryption,  ivAndKey);
        return getCipherData(aes, data);
    }

    private static byte[] getCipherData(PaddedBufferedBlockCipher cipher, byte[] data) throws Exception {
        byte[] outputBuffer = new byte[cipher.getOutputSize(data.length)];

        int lengthFirst     = cipher.processBytes(data,  0, data.length, outputBuffer, 0);
        int lengthSecond    = cipher.doFinal(outputBuffer, lengthFirst);

        byte[] result = new byte[lengthFirst + lengthSecond];

        System.arraycopy(outputBuffer, 0, result, 0, result.length);

        return result;
    }

    // Public Functions

    public CryptoKeyPair generateKeyPair() {
        try {
            ECParameterSpec     ecSpec      = ECNamedCurveTable.getParameterSpec("secp256r1");
            KeyPairGenerator    generator   = KeyPairGenerator.getInstance("ECDSA", "SC");

            generator.initialize(ecSpec, new SecureRandom());
            KeyPair keyPair = generator.generateKeyPair();

            String publicKeyBase64  = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.NO_WRAP);
            String privateKeyBase64 = Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.NO_WRAP);

            return new CryptoKeyPair(publicKeyBase64, privateKeyBase64);
        } catch (Exception ex) {
            LogManager.getInstance().error("Generate key pair failed with error: " + ex.getLocalizedMessage());
            return null;
        }
    }

    public String getPublicKeyBase64WithNoHead(String publicKey) {
        byte[] publicKeyBytes   = Base64.decode(publicKey, Base64.NO_WRAP);
        byte[] result           = Arrays.copyOfRange(publicKeyBytes, P256_HEAD.length, publicKeyBytes.length);

        return Base64.encodeToString(result, Base64.NO_WRAP);
    }

    public byte[] encryptBytesWithIV(String privateKey, String publicKey, byte[] data, byte[] iv) {
        // public key should be 'base64 with no head'

        try {
            SecretKey secretKey = getSecretKey(privateKey, publicKey);

            byte[] password = MessageDigest.getInstance("SHA-256").digest(secretKey.getEncoded());

            CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(password), iv);

            return getEncryptedBytes(data, ivAndKey);
        } catch (Exception ex) {
            LogManager.getInstance().error("Encryption of data with IV failed with error: " + ex.getLocalizedMessage());
            return new byte[0];
        }
    }

}
