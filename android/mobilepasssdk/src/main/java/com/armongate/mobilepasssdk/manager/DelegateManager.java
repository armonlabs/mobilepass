package com.armongate.mobilepasssdk.manager;

import com.armongate.mobilepasssdk.constant.CancelReason;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.delegate.PassFlowDelegate;

public class DelegateManager {

    private static DelegateManager      mInstance                   = null;
    private static PassFlowDelegate     mCurrentPassFlowDelegate    = null;
    private static MobilePassDelegate   mCurrentMobilePassDelegate  = null;

    private static boolean mFlowCompleted = false;

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
    }

    public void mainPassCompleted(boolean succeed) {
        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onPassCompleted(succeed);
        }
    }

    public void mainPassCancelled(int reason) {
        if (reason == CancelReason.USER_CLOSED && mFlowCompleted) {
            return;
        }

        if (mCurrentMobilePassDelegate != null) {
            mCurrentMobilePassDelegate.onPassCancelled(reason);
        }
    }

    public void mainQRCodeListStateChanged(int state) {
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

    public void flowPassCompleted(boolean succeed) {
        if (mCurrentPassFlowDelegate != null) {
            mCurrentPassFlowDelegate.onPassCompleted(succeed);
        }

        mFlowCompleted = true;
        mainPassCompleted(succeed);
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

    public void flowNeedPermissionCamera() {
        if (mCurrentPassFlowDelegate != null) {
            mCurrentPassFlowDelegate.needPermissionCamera();
        }

        mainPassCancelled(CancelReason.NEED_PERMISSION_CAMERA);
    }

    public void flowNeedPermissionLocation() {
        if (mCurrentPassFlowDelegate != null) {
            mCurrentPassFlowDelegate.needPermissionLocation();
        }

        mainPassCancelled(CancelReason.NEED_PERMISSION_LOCATION);
    }

    public void flowNeedLocationSettingsChange() {
        if (mCurrentPassFlowDelegate != null) {
            mCurrentPassFlowDelegate.needEnableLocationServices();
        }

        mainPassCancelled(CancelReason.NEED_ENABLE_LOCATION_SERVICES);
    }

    public void flowNeedEnableBluetooth() {
        if (mCurrentPassFlowDelegate != null) {
            mCurrentPassFlowDelegate.needEnableBluetooth();
        }

        mainPassCancelled(CancelReason.NEED_ENABLE_BLE);
    }

    public void flowError(Exception ex) {
        if (mCurrentPassFlowDelegate != null) {
            mCurrentPassFlowDelegate.onError(ex);
        }

        mainPassCancelled(CancelReason.ERROR);
    }
}
