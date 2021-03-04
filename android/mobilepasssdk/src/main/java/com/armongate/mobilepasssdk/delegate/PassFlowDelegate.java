package com.armongate.mobilepasssdk.delegate;

public interface PassFlowDelegate {
    void onQRCodeFound(String code);
    void onLocationValidated();
    void onPassCompleted(boolean succeed);
    void onNextActionRequired();
    void onConnectionStateChanged(boolean isActive);
    void needPermissionCamera();
    void needPermissionLocation();
    void needEnableBluetooth();
    void needEnableLocationServices();
    void onError(Exception exception);
}
