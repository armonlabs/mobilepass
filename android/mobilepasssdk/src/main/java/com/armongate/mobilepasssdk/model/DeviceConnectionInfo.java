package com.armongate.mobilepasssdk.model;

public class DeviceConnectionInfo {

    public String deviceId;
    public String publicKey;
    public String hardwareId;

    public DeviceConnectionInfo(String id, String publicKey, String hardwareId) {
        this.deviceId   = id;
        this.publicKey  = publicKey;
        this.hardwareId = hardwareId;
    }

}
