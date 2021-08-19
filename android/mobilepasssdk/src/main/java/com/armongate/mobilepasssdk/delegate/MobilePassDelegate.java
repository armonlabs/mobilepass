package com.armongate.mobilepasssdk.delegate;

import com.armongate.mobilepasssdk.model.LogItem;

public interface MobilePassDelegate {
    void onPassCancelled(int reason);
    void onPassCompleted(boolean succeed);
    void onNeedPermission(int type);
    void onQRCodeListStateChanged(int state);
    void onLogReceived(LogItem log);
}
