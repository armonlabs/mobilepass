package com.armongate.mobilepasssdk.model;

import androidx.annotation.Nullable;

import com.armongate.mobilepasssdk.enums.QRCodeErrorType;

/**
 * Result of QR code processing
 */
public class QRCodeProcessResult {
    private final boolean isValid;
    private final QRCodeErrorType errorType;

    public QRCodeProcessResult(boolean isValid, @Nullable QRCodeErrorType errorType) {
        this.isValid = isValid;
        this.errorType = errorType;
    }

    public boolean isValid() {
        return isValid;
    }

    @Nullable
    public QRCodeErrorType getErrorType() {
        return errorType;
    }
}

