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
    /**
     * Displayable text sent by the device.
     *
     * Present on rejections; on success only once the firmware sends one, which
     * is why it is not named after failure. May be truncated when the packet does
     * not fit the negotiated MTU - the result code never is.
     */
    public String           message;
    /**
     * Result code sent by the device, e.g. A-1001, B-2, C-1234
     *
     * Null for firmware that does not support result codes, which is expected to
     * be the common case for a while. failReason stays the only signal there.
     */
    public String           resultCode;

    public DeviceConnectionStatus(String id, ConnectionState state, Integer failReason, String message) {
        this(id, state, failReason, message, null);
    }

    public DeviceConnectionStatus(String id, ConnectionState state, Integer failReason, String message, String resultCode) {
        this.id             = id;
        this.state          = state;
        this.failReason     = failReason;
        this.message        = message;
        this.resultCode     = resultCode;
    }

}
