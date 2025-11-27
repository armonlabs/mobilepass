package com.armongate.mobilepasssdk.enums;

/**
 * Types of QR code processing errors
 */
public enum QRCodeErrorType {
    INVALID_FORMAT(1),  // Malformed QR data (scanning issue)
    NOT_FOUND(2),       // Valid format but not in authorized list
    EXPIRED(3),         // Valid but expired (reserved for future)
    UNAUTHORIZED(4);    // Valid but user not authorized (reserved for future)

    private final int value;

    QRCodeErrorType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

