package com.armongate.mobilepasssdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.armongate.mobilepasssdk.activity.PassFlowActivity;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;
import com.armongate.mobilepasssdk.manager.BluetoothManager;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.model.Configuration;
import com.armongate.mobilepasssdk.service.BaseService;
import com.google.gson.Gson;

import java.util.List;

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

        LogManager.getInstance().info("SDK Version: 1.2.0");
        LogManager.getInstance().info("Configuration: " + config.getLog());

        BaseService.getInstance().setContext(context);
        BluetoothManager.getInstance().setContext(context);
        ConfigurationManager.getInstance().setConfig(context, config);
    }

    /**
     * Set listener to handle SDK callbacks
     */
    public void setDelegate(@Nullable MobilePassDelegate listener) {
        DelegateManager.getInstance().setCurrentMobilePassDelegate(listener);
    }

    /**
     * To set or update OAuth token of user and language code
     *
     * @param token OAuth token value of current user's session to validate
     * @param language Language code to localize texts [tr | en]
     */
    public void updateToken(String token, String language) {
        ConfigurationManager.getInstance().setToken(token, language);
    }

    /**
     * Starts qr code reading session and related flow
     */
    public void triggerQRCodeRead() {
        DelegateManager.getInstance().clearFlowFlags();
        BluetoothManager.getInstance().setReady();

        showActivity();
    }

    public List<String> getLogs() {
        return LogManager.getInstance().getLogs();
    }

    private void showActivity() {
        Class<PassFlowActivity> cls = PassFlowActivity.class;

        Intent intent = new Intent(mActiveContext, cls);
        final ComponentName component = new ComponentName(mActiveContext, cls);
        intent.setComponent(component);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActiveContext.startActivity(intent);
    }

}
