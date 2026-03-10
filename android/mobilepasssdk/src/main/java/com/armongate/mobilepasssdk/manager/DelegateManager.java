package com.armongate.mobilepasssdk.manager;

import com.armongate.mobilepasssdk.constant.NeedPermissionType;
import com.armongate.mobilepasssdk.constant.QRCodeListState;
import com.armongate.mobilepasssdk.enums.AnalyticsResult;
import com.armongate.mobilepasssdk.enums.PassFlowStateUpdate;
import com.armongate.mobilepasssdk.enums.PassMethod;
import com.armongate.mobilepasssdk.constant.PassFlowResultCode;
import com.armongate.mobilepasssdk.constant.PassFlowStateCode;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.enums.QRCodesSyncState;
import com.armongate.mobilepasssdk.model.AnalyticsStep;
import com.armongate.mobilepasssdk.model.LocationRequirement;
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

    private static DelegateManager    mInstance                  = null;
    private static MobilePassDelegate mCurrentMobilePassDelegate = null;

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

    public void setCurrentMobilePassDelegate(MobilePassDelegate listener) {
        mCurrentMobilePassDelegate = listener;
    }

    public void onMemberIdChanged() {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onMemberIdChanged();
        }
    }

    public void onMemberIdSyncCompleted(boolean success, Integer statusCode) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onSyncMemberIdCompleted(success, success ? null : statusCode);
        }
    }

    public void onQRCodesSyncFailed(Integer statusCode) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onQRCodesSyncStateChanged(
                QRCodesSyncState.syncFailed(statusCode != null ? statusCode : -1)
            );
        }
    }

    public void onCompleted(int resultCode, Boolean isRemoteAccess) {
        this.onCompleted(resultCode, isRemoteAccess, null, null, null, null);
    }

    public void onCompleted(int resultCode, Boolean isRemoteAccess, Integer direction, String clubId, String clubName) {
        this.onCompleted(resultCode, isRemoteAccess, direction, clubId, clubName, null);
    }

    public void onCompleted(int resultCode, Boolean isRemoteAccess, Integer direction, String clubId, String clubName, String message) {
        if (mCurrentMobilePassDelegate != null) {
            PassFlowResult result = new PassFlowResult(
                            resultCode,
                            direction,
                            clubId,
                            clubName,
                    PassFlowManager.getInstance().getStates(),
                    message);

            mCurrentMobilePassDelegate.onPassFlowStateChanged(
                PassFlowStateUpdate.completed(result)
            );
        }

        // Map result code to analytics result
        AnalyticsResult analyticsResult;
        if (resultCode == PassFlowResultCode.CANCEL) {
            analyticsResult = AnalyticsResult.CANCEL;
        } else if (resultCode == PassFlowResultCode.SUCCESS) {
            analyticsResult = AnalyticsResult.SUCCESS;
        } else {
            analyticsResult = AnalyticsResult.FAIL;
        }
        
        shareAnalytics(
            analyticsResult,
            isRemoteAccess,
            direction,
            clubId
        );
    }

    public void onQRCodeListStateChanged(int state, int count) {
        mQRCodeListState = state;

        if (mCurrentMobilePassDelegate != null) {
            if (state == QRCodeListState.SYNCING) {
                mCurrentMobilePassDelegate.onQRCodesSyncStateChanged(
                    QRCodesSyncState.syncStarted()
                );
            } else {
                if (count == 0) {
                    mCurrentMobilePassDelegate.onQRCodesSyncStateChanged(
                        QRCodesSyncState.dataEmpty()
                    );
                } else {
                    mCurrentMobilePassDelegate.onQRCodesSyncStateChanged(
                        QRCodesSyncState.syncCompleted(state == QRCodeListState.USING_SYNCED_DATA, count)
                    );
                }
            }
        }
    }

    public void notifyLocationRequired(LocationRequirement requirement) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onLocationVerificationRequired(requirement);
        }
    }

    public void notifyStateChanged(int state, String data) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onPassFlowStateChanged(
                PassFlowStateUpdate.stateChanged(state, data)
            );
        }
    }

    public void needPermission(int type) {
        LogManager.getInstance().warn("Need permission to continue passing flow, permission type: " + type, null);

        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onPermissionRequired(type);
        }
    }

    public void onLogItemCreated(LogItem log) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onLogReceived(log);
        }
    }

    // Note: onMockLocationDetected removed - location verification is now app's responsibility

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
