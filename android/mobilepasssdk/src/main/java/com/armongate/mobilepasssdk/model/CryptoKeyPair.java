package com.armongate.mobilepasssdk.model;

import com.armongate.mobilepasssdk.manager.CryptoManager;

public class CryptoKeyPair {

    public String publicKey;
    public String privateKey;

    public String getPublicKeyWithoutHead() {
        return CryptoManager.getInstance().getPublicKeyBase64WithNoHead(publicKey);
    }

    public CryptoKeyPair(String publicKey, String privateKey) {
        this.publicKey  = publicKey;
        this.privateKey = privateKey;
    }

}

