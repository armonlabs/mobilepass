package com.armongate.mobilepasssdk.enums;

public enum PassMethod {
    BLE("ble"),
    REMOTE("remote");

    private final String value;

    PassMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}