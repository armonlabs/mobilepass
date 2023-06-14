package com.armongate.mobilepasssdk.delegate;

import com.armongate.mobilepasssdk.model.LogItem;
import com.armongate.mobilepasssdk.model.PassFlowResult;
import com.armongate.mobilepasssdk.model.PassResult;

public interface MobilePassDelegate {
    void onLogReceived(LogItem log);
    void onInvalidQRCode(String content);


    void onMemberIdChanged();
    void onSyncMemberIdCompleted();
    void onSyncMemberIdFailed(int statusCode);
    void onQRCodesDataLoaded(int count);
    void onQRCodesSyncStarted();
    void onQRCodesSyncFailed(int statusCode);
    void onQRCodesReady(boolean synced, int count);
    void onQRCodesEmpty();
    void onScanFlowCompleted(PassFlowResult result);

}
