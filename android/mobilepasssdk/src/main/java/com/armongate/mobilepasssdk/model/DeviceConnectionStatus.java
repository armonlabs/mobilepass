package com.armongate.mobilepasssdk.model;

public class DeviceConnectionStatus {

    public enum ConnectionState {
        CONNECTING,
        CONNECTED,
        FAILED,
        DISCONNECTED,
        NOT_FOUND
    }

    public String           id;
    public ConnectionState  state;
    public Integer          failReason;

    public DeviceConnectionStatus(String id, ConnectionState state, Integer failReason) {
        this.id         = id;
        this.state      = state;
        this.failReason = failReason;
    }

}
