package com.armongate.mobilepasssdk.delegate;

public interface MobilePassDelegate {
    void onPassCancelled(int reason);
    void onPassCompleted(boolean succeed);
    void onNeedPermission(int type);
    void onQRCodeListStateChanged(int state);
}
