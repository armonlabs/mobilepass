package com.armongate.mobilepasssdk.manager;

import android.os.Handler;
import android.os.Looper;

import com.armongate.mobilepasssdk.constant.CancelReason;
import com.armongate.mobilepasssdk.constant.PassFailCode;
import com.armongate.mobilepasssdk.constant.QRCodeListState;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.delegate.PassFlowDelegate;
import com.armongate.mobilepasssdk.delegate.QRCodeListStateDelegate;
import com.armongate.mobilepasssdk.model.LogItem;
import com.armongate.mobilepasssdk.model.PassResult;

public class DelegateManager {

    private static DelegateManager          mInstance                       = null;
    private static PassFlowDelegate         mCurrentPassFlowDelegate        = null;
    private static MobilePassDelegate       mCurrentMobilePassDelegate      = null;
    private static QRCodeListStateDelegate  mCurrentQRCodeListStateDelegate = null;

    private static boolean mFlowCompleted = false;
    private static boolean mDismissedManual = false;
    private static boolean mFinishedBefore = false;
    private static Handler mAutoCloseHandler = null;

    private int mQRCodeListState = QRCodeListState.INITIALIZING;

    private DelegateManager() {

    }

    public static DelegateManager getInstance() {
        if (mInstance == null) {
            mInstance  = new DelegateManager();
        }

        return mInstance;
    }

    public int getQRCodeListState() {
        return mQRCodeListState;
    }

    public boolean isQRCodeListRefreshable() {
        return mQRCodeListState != QRCodeListState.INITIALIZING && mQRCodeListState != QRCodeListState.SYNCING;
    }

    public void setCurrentPassFlowDelegate(PassFlowDelegate listener) {
        mCurrentPassFlowDelegate = listener;
    }

    public void setCurrentMobilePassDelegate(MobilePassDelegate listener) {
        mCurrentMobilePassDelegate = listener;
    }

    public void setCurrentQRCodeListStateDelegate(QRCodeListStateDelegate listener) {
        mCurrentQRCodeListStateDelegate = listener;
    }

    public void clearFlowFlags() {
        mFlowCompleted = false;
        mDismissedManual = false;
        mFinishedBefore = false;
    }

    public void onCompleted(boolean success, Integer direction, String clubId, String clubName, Integer failCode) {
        mFlowCompleted = true;

        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onPassCompleted(new PassResult(success, direction, clubId, clubName, failCode));
        }

        startAutoCloseTimer();
    }

    public void onCancelled(boolean dismiss) {
        endFlow(dismiss, CancelReason.USER_CLOSED);
    }

    public void onQRCodeListStateChanged(int state) {
        mQRCodeListState = state;

        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onQRCodeListStateChanged(state);
        }

        if (mCurrentQRCodeListStateDelegate != null) {
            mCurrentQRCodeListStateDelegate.onStateChanged(state);
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

    public void flowCloseWithInvalidQRCode(String code) {
        endFlow(true, CancelReason.INVALID_QR_CODE);

        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onInvalidQRCode(code);
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
        LogManager.getInstance().error("Exception on pass flow; " + ex.getLocalizedMessage(), null);
        endFlow(true, CancelReason.ERROR);
    }

    public void onLogItemCreated(LogItem log) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onLogReceived(log);
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

        endAutoCloseTime();
    }

    private void startAutoCloseTimer() {
        if (ConfigurationManager.getInstance().autoCloseTimeout() != null) {
            LogManager.getInstance().info("Start auto close timer for " + ConfigurationManager.getInstance().autoCloseTimeout() + " second(s)");
            mAutoCloseHandler = new Handler(Looper.getMainLooper());
            mAutoCloseHandler.postDelayed(new Runnable() {
                public void run() {
                    LogManager.getInstance().debug("Auto close timer has been triggered");
                    endFlow(true, -1);
                }
            }, ConfigurationManager.getInstance().autoCloseTimeout() * 1000);
        }
    }

    private void endAutoCloseTime() {
        if (mAutoCloseHandler != null) {
            LogManager.getInstance().info("End auto close timer");
            mAutoCloseHandler.removeCallbacksAndMessages(null);
        }
    }

}
