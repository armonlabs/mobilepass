package com.armongate.mobilepasssdk.model;

public class StorageDataUserDetails {
    public String userId;
    public String publicKey;
    public String privateKey;

    public StorageDataUserDetails(String userId, String publicKey, String privateKey) {
        this.userId     = userId;
        this.publicKey  = publicKey;
        this.privateKey = privateKey;
    }
}