package com.armongate.mobilepasssdk.manager;

import android.os.Handler;

import com.armongate.mobilepasssdk.activity.PassFlowActivity;
import com.armongate.mobilepasssdk.constant.CancelReason;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.delegate.PassFlowDelegate;

public class DelegateManager {

    private static DelegateManager      mInstance                   = null;
    private static PassFlowDelegate     mCurrentPassFlowDelegate    = null;
    private static MobilePassDelegate   mCurrentMobilePassDelegate  = null;

    private static boolean mFlowCompleted = false;
    private static boolean mDismissedManual = false;
    private static boolean mFinishedBefore = false;

    private DelegateManager() {

    }

    public static DelegateManager getInstance() {
        if (mInstance == null) {
            mInstance  = new DelegateManager();
        }

        return mInstance;
    }

    public void setCurrentPassFlowDelegate(PassFlowDelegate listener) {
        mCurrentPassFlowDelegate = listener;
    }

    public void setCurrentMobilePassDelegate(MobilePassDelegate listener) {
        mCurrentMobilePassDelegate = listener;
    }

    public void clearFlowFlags() {
        mFlowCompleted = false;
        mDismissedManual = false;
    }

    public void onCompleted(boolean succeed) {
        mFlowCompleted = true;

        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onPassCompleted(succeed);
        }

        startAutoCloseTimer();
    }

    public void onCancelled(boolean dismiss) {
        endFlow(dismiss, CancelReason.USER_CLOSED);
    }

    public void onQRCodeListStateChanged(int state) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onQRCodeListStateChanged(state);
        }
    }

    public void flowLocationValidated() {
        if (mCurrentPassFlowDelegate != null) {
            mCurrentPassFlowDelegate.onLocationValidated();
        }
    }

    public void flowQRCodeFound(String code) {
        if (mCurrentPassFlowDelegate != null) {
            mCurrentPassFlowDelegate.onQRCodeFound(code);
        }
    }

    public void flowNextActionRequired() {
        if (mCurrentPassFlowDelegate != null) {
            mCurrentPassFlowDelegate.onNextActionRequired();
        }
    }

    public void flowConnectionStateChanged(boolean isActive) {
        if (mCurrentPassFlowDelegate != null) {
            mCurrentPassFlowDelegate.onConnectionStateChanged(isActive);
        }
    }

    public void onErrorOccurred(Exception ex) {
        LogManager.getInstance().error("Exception on pass flow; " + ex.getLocalizedMessage());
        endFlow(true, CancelReason.ERROR);
    }

    public void onNeedPermissionCamera() {
        endFlow(true, CancelReason.NEED_PERMISSION_CAMERA);
    }

    public void onNeedPermissionLocation() {
        endFlow(true, CancelReason.NEED_PERMISSION_LOCATION);
    }

    public void onNeedPermissionBluetooth() {
        endFlow(true, CancelReason.NEED_PERMISSION_BLUETOOTH);
    }

    public void onNeedLocationSettingsChange() {
        endFlow(true, CancelReason.NEED_ENABLE_LOCATION_SERVICES);
    }

    public void onNeedEnableBluetooth() {
        endFlow(true, CancelReason.NEED_ENABLE_BLE);
    }

    public void onMockLocationDetected() {
        endFlow(true, CancelReason.USING_MOCK_LOCATION_DATA);
    }

    private void endFlow(boolean dismiss, int reason) {
        if (dismiss) {
            mDismissedManual = true;

            if (mCurrentPassFlowDelegate != null && !mFinishedBefore) {
                mFinishedBefore = true;
                mCurrentPassFlowDelegate.onFinishRequired();
            }

            if (mCurrentMobilePassDelegate != null && !mFlowCompleted) {
                mCurrentMobilePassDelegate.onPassCancelled(reason);
            }
        } else if (mCurrentMobilePassDelegate != null && !mFlowCompleted && !mDismissedManual) {
            mCurrentMobilePassDelegate.onPassCancelled(reason);
        }
    }

    private void startAutoCloseTimer() {
        if (ConfigurationManager.getInstance().autoCloseTimeout() != null) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    endFlow(true, CancelReason.AUTO_CLOSE);
                }
            }, ConfigurationManager.getInstance().autoCloseTimeout() * 1000);
        }
    }

}
