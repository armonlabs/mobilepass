package com.armongate.mobilepasssdk.delegate;

public interface PassFlowDelegate {
    void onQRCodeFound(String code);
    void onLocationValidated();
    void onNextActionRequired();
    void onConnectionStateChanged(boolean isActive);
    void onFinishRequired();
}
