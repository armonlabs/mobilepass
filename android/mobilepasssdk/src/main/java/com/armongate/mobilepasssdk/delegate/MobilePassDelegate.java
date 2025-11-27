package com.armongate.mobilepasssdk.delegate;

import androidx.annotation.Nullable;

import com.armongate.mobilepasssdk.constant.NeedPermissionType;
import com.armongate.mobilepasssdk.enums.PassFlowStateUpdate;
import com.armongate.mobilepasssdk.enums.QRCodesSyncState;
import com.armongate.mobilepasssdk.model.LocationRequirement;
import com.armongate.mobilepasssdk.model.LogItem;

/**
 * Delegate interface for SDK callbacks
 * Headless architecture - app provides all UI
 */
public interface MobilePassDelegate {
    /**
     * Log messages from SDK
     */
    void onLogReceived(LogItem log);

    /**
     * Member ID changed (token updated)
     */
    void onMemberIdChanged();

    /**
     * Member ID sync completed
     * @param success true if sync succeeded
     * @param statusCode HTTP status code if failed (null if success)
     */
    void onSyncMemberIdCompleted(boolean success, @Nullable Integer statusCode);

    /**
     * QR codes sync state changed
     * Consolidates: syncStarted, syncCompleted, syncFailed, dataEmpty
     */
    void onQRCodesSyncStateChanged(QRCodesSyncState state);

    /**
     * Pass flow state changed
     * Consolidates: intermediate states and final completion
     */
    void onPassFlowStateChanged(PassFlowStateUpdate state);

    /**
     * Location verification required for remote access
     * App should verify location and call confirmLocationVerified()
     */
    void onLocationVerificationRequired(LocationRequirement requirement);

    /**
     * Permission required
     * App should handle permission request UI
     */
    void onPermissionRequired(int type);
}
