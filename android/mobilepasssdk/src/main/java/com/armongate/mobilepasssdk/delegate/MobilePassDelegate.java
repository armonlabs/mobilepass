package com.armongate.mobilepasssdk.delegate;

import com.armongate.mobilepasssdk.model.LogItem;
import com.armongate.mobilepasssdk.model.PassResult;

public interface MobilePassDelegate {
    void onPassCancelled(int reason);
    void onPassCompleted(PassResult result);
    void onQRCodeListStateChanged(int state);
    void onLogReceived(LogItem log);
    void onInvalidQRCode(String content);
}
