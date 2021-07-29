package com.armongate.mobilepasssdk.manager;

import android.os.Handler;

import com.armongate.mobilepasssdk.activity.PassFlowActivity;
import com.armongate.mobilepasssdk.constant.CancelReason;
import com.armongate.mobilepasssdk.constant.NeedPermissionType;
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

    public void onNeedPermission(int type) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onNeedPermission(type);
        }
    }

    public void goToSettings() {
        endFlow(true, -1);
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

            if (mCurrentMobilePassDelegate != null && !mFlowCompleted && reason >= 0) {
                mCurrentMobilePassDelegate.onPassCancelled(reason);
            }
        } else if (mCurrentMobilePassDelegate != null && !mFlowCompleted && !mDismissedManual && reason >= 0) {
            mCurrentMobilePassDelegate.onPassCancelled(reason);
        }
    }

    private void startAutoCloseTimer() {
        if (ConfigurationManager.getInstance().autoCloseTimeout() != null) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    endFlow(true, -1);
                }
            }, ConfigurationManager.getInstance().autoCloseTimeout() * 1000);
        }
    }

}
