package com.armongate.mobilepasssdk.model;

public class StorageDataDevice {
    public String userId;
    public String deviceId;
    public String devicePublicKey;

    public StorageDataDevice(String userId, String deviceId, String devicePublicKey) {
        this.userId             = userId;
        this.deviceId           = deviceId;
        this.devicePublicKey    = devicePublicKey;
    }
}