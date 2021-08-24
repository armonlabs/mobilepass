package com.armongate.mobilepasssdk.model;

public class DeviceConnectionInfo {

    public String deviceId;
    public String publicKey;

    public DeviceConnectionInfo(String id, String publicKey) {
        this.deviceId   = id;
        this.publicKey  = publicKey;
    }

}
