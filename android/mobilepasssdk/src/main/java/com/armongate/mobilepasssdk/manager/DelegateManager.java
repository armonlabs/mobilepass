package com.armongate.mobilepasssdk.manager;

import android.os.Handler;
import android.os.Looper;

import com.armongate.mobilepasssdk.enums.AnalyticsResult;
import com.armongate.mobilepasssdk.enums.PassMethod;
import com.armongate.mobilepasssdk.constant.PassFlowResultCode;
import com.armongate.mobilepasssdk.constant.PassFlowStateCode;
import com.armongate.mobilepasssdk.constant.QRCodeListState;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.delegate.PassFlowDelegate;
import com.armongate.mobilepasssdk.delegate.QRCodeListStateDelegate;
import com.armongate.mobilepasssdk.model.AnalyticsStep;
import com.armongate.mobilepasssdk.model.LogItem;
import com.armongate.mobilepasssdk.model.PassFlowResult;
import com.armongate.mobilepasssdk.model.PassFlowState;
import com.armongate.mobilepasssdk.model.request.RequestAnalyticsData;
import com.armongate.mobilepasssdk.service.AnalyticsService;
import com.armongate.mobilepasssdk.service.BaseService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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

    public void onMemberIdChanged() {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onMemberIdChanged();
        }
    }

    public void onMemberIdSyncCompleted(boolean success, Integer statusCode) {
        if (mCurrentMobilePassDelegate != null) {
            if (success) {
                mCurrentMobilePassDelegate.onSyncMemberIdCompleted();
            } else {
                mCurrentMobilePassDelegate.onSyncMemberIdFailed(statusCode != null ? statusCode : -1);
            }
        }
    }

    public void onQRCodesDataLoaded(int count) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onQRCodesDataLoaded(count);
        }
    }

    public void onQRCodesSyncFailed(Integer statusCode) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onQRCodesSyncFailed(statusCode);
        }
    }

    public void onCompleted(boolean success, Boolean isRemoteAccess) {
        this.onCompleted(success, isRemoteAccess, null, null, null);
    }

    public void onCompleted(boolean success, Boolean isRemoteAccess, Integer direction, String clubId, String clubName) {
        mFlowCompleted = true;

        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onScanFlowCompleted(
                    new PassFlowResult(
                            success ? PassFlowResultCode.SUCCESS : PassFlowResultCode.FAIL,
                            direction,
                            clubId,
                            clubName,
                            PassFlowManager.getInstance().getStates()));
        }

        startAutoCloseTimer();
        shareAnalytics(
            success ? AnalyticsResult.SUCCESS : AnalyticsResult.FAIL,
            isRemoteAccess,
            direction,
            clubId
        );
    }

    public void onCancelled(boolean dismiss) {
        endFlow(dismiss, PassFlowStateCode.CANCELLED_BY_USER, null);
    }

    public void onQRCodeListStateChanged(int state, int count) {
        mQRCodeListState = state;

        if (mCurrentMobilePassDelegate != null) {
            if (state == QRCodeListState.SYNCING) {
                mCurrentMobilePassDelegate.onQRCodesSyncStarted();
            } else {
                if (count == 0) {
                    mCurrentMobilePassDelegate.onQRCodesEmpty();
                } else {
                    mCurrentMobilePassDelegate.onQRCodesReady(state == QRCodeListState.USING_SYNCED_DATA, count);
                }
            }
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
        endFlow(true, PassFlowStateCode.CANCELLED_WITH_INVALID_QRCODE, code);

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

        endFlow(true, PassFlowStateCode.CANCELLED_WITH_ERROR, ex.getLocalizedMessage());
    }

    public void onLogItemCreated(LogItem log) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onLogReceived(log);
        }
    }

    public void goToSettings() {
        endFlow(true, PassFlowStateCode.CANCELLED_TO_GO_SETTINGS, null);
    }

    public void onMockLocationDetected() {
        endFlow(true, PassFlowStateCode.CANCELLED_WITH_MOCK_LOCATION, null);
    }

    private void endFlow(boolean dismiss, int reason, String data) {
        if (dismiss) {
            mDismissedManual = true;

            if (mCurrentPassFlowDelegate != null && !mFinishedBefore) {
                mFinishedBefore = true;
                mCurrentPassFlowDelegate.onFinishRequired();
            }

            if (!mFlowCompleted && reason >= 0) {
                this.cancelFlow(reason, data);
            }
        } else if (!mFlowCompleted && !mDismissedManual && reason >= 0) {
            this.cancelFlow(reason, data);
        }

        endAutoCloseTime();
    }

    private void cancelFlow(int reason, String data) {
        PassFlowManager.getInstance().addToStates(reason, data);

        if(mCurrentMobilePassDelegate != null ) {
            mCurrentMobilePassDelegate.onScanFlowCompleted(
                    new PassFlowResult(
                            PassFlowResultCode.CANCEL,
                            null, null, null,
                            PassFlowManager.getInstance().getStates()));
        }

        shareAnalytics(AnalyticsResult.CANCEL, null, null, null);
    }

    private void startAutoCloseTimer() {
        if (ConfigurationManager.getInstance().autoCloseTimeout() != null) {
            LogManager.getInstance().info("Start auto close timer for " + ConfigurationManager.getInstance().autoCloseTimeout() + " second(s)");
            mAutoCloseHandler = new Handler(Looper.getMainLooper());
            mAutoCloseHandler.postDelayed(() -> {
                LogManager.getInstance().debug("Auto close timer has been triggered");
                endFlow(true, -1, null);
            }, ConfigurationManager.getInstance().autoCloseTimeout() * 1000);
        }
    }

    private void endAutoCloseTime() {
        if (mAutoCloseHandler != null) {
            LogManager.getInstance().info("End auto close timer");
            mAutoCloseHandler.removeCallbacksAndMessages(null);
        }
    }

    private void shareAnalytics(AnalyticsResult result, Boolean isRemoteAccess, Integer direction, String clubId) {
        List<PassFlowState> states = PassFlowManager.getInstance().getLogStates();
        Date startTime = states.isEmpty() ? new Date() : states.get(0).datetime;
        long duration = new Date().getTime() - startTime.getTime();
        
        List<AnalyticsStep> analyticsSteps = new ArrayList<>();
        for (PassFlowState state : states) {
            analyticsSteps.add(new AnalyticsStep(
                state.getState(),
                state.getData(),
                state.getDatetime() != null ? state.getDatetime() : startTime
            ));
        }

        PassMethod method = null;
        if (isRemoteAccess != null) {
            method = isRemoteAccess ? PassMethod.REMOTE : PassMethod.BLE;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String formattedDate = sdf.format(new Date());

        RequestAnalyticsData request = new RequestAnalyticsData(
            formattedDate,
            duration,
            result,
            method,
            clubId != null ? clubId : PassFlowManager.getInstance().getLastClubId(),
            PassFlowManager.getInstance().getLastQRCodeId(),
            direction,
            analyticsSteps
        );

        new AnalyticsService().sendAnalytics(ConfigurationManager.getInstance().getCurrentContext(), request, new BaseService.ServiceResultListener() {
            @Override
            public void onCompleted(Object result) {}

            @Override
            public void onError(int statusCode, String message) {}
        });
    }
}
