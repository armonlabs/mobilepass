package com.armongate.mobilepasssdk;

import android.content.Context;

import androidx.annotation.Nullable;

import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.manager.BluetoothManager;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.manager.PassFlowManager;
import com.armongate.mobilepasssdk.model.Configuration;
import com.armongate.mobilepasssdk.model.QRCodeProcessResult;
import com.armongate.mobilepasssdk.service.BaseService;

public class MobilePass {

    private final Context mActiveContext;

    /**
     * Initializes new Mobile Pass instance
     *
     * @param context Android context to use for SDK flow
     * @param config Configuration of flow
     */
    public MobilePass(Context context, Configuration config) {
        mActiveContext = context;

        if (config.listener != null) {
            DelegateManager.getInstance().setCurrentMobilePassDelegate(config.listener);
        }

        // Clear listener instance to prevent exception when convert configuration to json
        config.listener = null;

        ConfigurationManager.getInstance().setConfig(context, config);

        LogManager.getInstance().info("SDK Version: 2.0.1");
        LogManager.getInstance().info("Configuration: " + config.getLog());

        BaseService.getInstance().setContext(context);
        ConfigurationManager.getInstance().setReady();
        BluetoothManager.getInstance().setContext(context);
    }

    /**
     * Set listener to handle SDK callbacks
     */
    public void setDelegate(@Nullable MobilePassDelegate listener) {
        DelegateManager.getInstance().setCurrentMobilePassDelegate(listener);
    }

    /**
     * Sync QR code list from server
     */
    public void sync() {
        ConfigurationManager.getInstance().refreshList();
    }

    /**
     * Process scanned QR code data from external scanner
     *
     * @param data QR code string data scanned by the app
     * @return QRCodeProcessResult with validation status and error type
     */
    public QRCodeProcessResult processQRCode(String data) {
        PassFlowManager.getInstance().clearStates();
        BluetoothManager.getInstance().setReady();

        return PassFlowManager.getInstance().processQRCode(data);
    }

    /**
     * Confirm that location has been verified by the app
     *
     * Call this after your app:
     * 1. Receives onLocationVerificationRequired callback with lat/lon/radius
     * 2. Gets user's location and calculates distance
     * 3. Shows UI and user confirms or is within radius
     *
     * The app is responsible for all location verification logic and UI
     * SDK will automatically continue the pass flow after this call
     */
    public void confirmLocationVerified() {
        PassFlowManager.getInstance().confirmLocationVerified();
    }

    /**
     * Cancel current flow
     */
    /**
     * Cancel current flow - stops any active Bluetooth operations and resets state
     */
    public void cancelFlow() {
        PassFlowManager.getInstance().cancelFlow();
    }

}
