package com.armongate.mobilepasssdk.model;

public class DeviceSignalInfo {

    public String   identifier;
    public int      rssi;

    public DeviceSignalInfo(String identifier, int rssi) {
        this.identifier = identifier;
        this.rssi       = rssi;
    }

}
