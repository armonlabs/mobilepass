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
import com.armongate.mobilepasssdk.manager.PassFlowManager;
import com.armongate.mobilepasssdk.model.Configuration;
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

        LogManager.getInstance().info("SDK Version: 1.7.6");
        LogManager.getInstance().info("Service Provider: " + ConfigurationManager.getInstance().getServiceProvider());
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
        PassFlowManager.getInstance().clearStates();

        DelegateManager.getInstance().clearFlowFlags();
        BluetoothManager.getInstance().setReady();

        showActivity();
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
