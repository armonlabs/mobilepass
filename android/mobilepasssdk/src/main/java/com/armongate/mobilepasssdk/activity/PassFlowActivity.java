package com.armongate.mobilepasssdk.activity;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.constant.CancelReason;
import com.armongate.mobilepasssdk.constant.QRTriggerType;
import com.armongate.mobilepasssdk.delegate.PassFlowDelegate;
import com.armongate.mobilepasssdk.fragment.MapFragment;
import com.armongate.mobilepasssdk.fragment.QRCodeReaderFragment;
import com.armongate.mobilepasssdk.fragment.StatusFragment;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.model.QRCodeContent;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointItemQRCodeItemTrigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PassFlowActivity extends AppCompatActivity implements PassFlowDelegate {

    public PassFlowActivity() {
        super(R.layout.activity_pass_flow);
    }

    public static final String ACTION_BLUETOOTH    = "bluetooth";
    public static final String ACTION_REMOTEACCESS = "remoteAccess";
    public static final String ACTION_LOCATION     = "location";

    private List<String>    actionList          = new ArrayList<>();
    private String          actionCurrent       = "";
    private QRCodeContent   activeQRCodeContent = null;
    private boolean         connectionActive    = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set flow delegate listener for activities
        DelegateManager.getInstance().setCurrentPassFlowDelegate(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment, QRCodeReaderFragment.class, null)
                    .commit();
        }

        setLocale(ConfigurationManager.getInstance().getLanguage());
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = this.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    @Override
    public void onBackPressed() {
        if (!connectionActive) {
            super.onBackPressed();
            DelegateManager.getInstance().mainPassCancelled(CancelReason.USER_CLOSED);
        }
    }

    @Override
    public void onQRCodeFound(String code) {
        processQRCodeData(code);
    }

    @Override
    public void onLocationValidated() {
        checkNextAction();
    }

    @Override
    public void onPassCompleted(boolean succeed) { }

    @Override
    public void onNextActionRequired() {
        checkNextAction();
    }

    @Override
    public void onConnectionStateChanged(boolean isActive) {
        connectionActive = isActive;

        // TODO Call this from Status fragment while waiting remote access response and ble connection
    }

    @Override
    public void needPermissionCamera() {
        finish();
    }

    @Override
    public void needPermissionLocation() {
        finish();
    }

    @Override
    public void needEnableBluetooth() {
        finish();
    }

    @Override
    public void needEnableLocationServices() {
        finish();
    }

    @Override
    public void onError(Exception exception) {
        finish();
    }

    private void checkNextAction() {
        if (this.actionList.size() > 0) {
            actionCurrent = this.actionList.get(0);
            this.actionList.remove(0);

            processAction();
        } else {
            finish();
        }
    }

    private void replaceFragment(Class<? extends androidx.fragment.app.Fragment> newFragment, @Nullable Bundle args) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment, newFragment, args)
                .setReorderingAllowed(true)
                .commit();
    }

    private void processQRCodeData(String code) {
        activeQRCodeContent = ConfigurationManager.getInstance().getQRCodeContent(code);

        if (activeQRCodeContent != null) {
            ResponseAccessPointItemQRCodeItemTrigger trigger = activeQRCodeContent.action.config != null ? activeQRCodeContent.action.config.trigger : null;

            if (trigger != null) {
                boolean needLocation = trigger.validateGeoLocation && activeQRCodeContent.accessPoint.geoLocation != null;

                LogManager.getInstance().debug("Need location: " + needLocation);

                actionList = new ArrayList<>();

                switch (trigger.type) {
                    case QRTriggerType.Bluetooth:
                        LogManager.getInstance().debug("Trigger Type: Bluetooth");
                        actionCurrent = ACTION_BLUETOOTH;
                        break;
                    case QRTriggerType.BluetoothThenRemote:
                        LogManager.getInstance().debug("Trigger Type: Bluetooth Then Remote");
                        actionCurrent = ACTION_BLUETOOTH;

                        if (needLocation) {
                            actionList.add(ACTION_LOCATION);
                        }

                        actionList.add(ACTION_REMOTEACCESS);
                        break;
                    case QRTriggerType.Remote:
                    case QRTriggerType.RemoteThenBluetooth:
                        actionCurrent = needLocation ? ACTION_LOCATION : ACTION_REMOTEACCESS;

                        if (needLocation) {
                            actionList.add(ACTION_REMOTEACCESS);
                        }

                        if (trigger.type == QRTriggerType.RemoteThenBluetooth) {
                            LogManager.getInstance().debug("Trigger Type: Remote Then Bluetooth");
                            actionList.add(ACTION_BLUETOOTH);
                        } else {
                            LogManager.getInstance().debug("Trigger Type: Remote");
                        }
                        break;
                    default:
                        LogManager.getInstance().warn("Unknown QR code trigger type! > " + trigger.type);
                }

                processAction();
            }
        }

    }

    private void processAction() {
        if (actionCurrent.isEmpty()) {
            return;
        }

        LogManager.getInstance().debug("Current action: " + actionCurrent);
        LogManager.getInstance().debug("Action list: " + actionList.toString());

        if (actionCurrent.equals(ACTION_LOCATION)) {
            Bundle bundle = new Bundle();
            bundle.putDouble("latitude", activeQRCodeContent.accessPoint.geoLocation.latitude);
            bundle.putDouble("longitude", activeQRCodeContent.accessPoint.geoLocation.longitude);
            bundle.putInt("radius", activeQRCodeContent.accessPoint.geoLocation.radius);

            replaceFragment(MapFragment.class, bundle);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("type", actionCurrent);
            bundle.putString("deviceId", activeQRCodeContent.accessPoint.deviceInfo.id);
            bundle.putString("accessPointId", activeQRCodeContent.accessPoint.id);
            bundle.putInt("direction", activeQRCodeContent.action.config.direction);
            bundle.putString("hardwareId", activeQRCodeContent.action.config.hardwareId);
            bundle.putInt("deviceNumber", activeQRCodeContent.action.config.deviceNumber);
            bundle.putInt("relayNumber", activeQRCodeContent.action.config.relayNumber);
            bundle.putString("publicKey", activeQRCodeContent.accessPoint.deviceInfo.publicKey);
            bundle.putString("nextAction", actionList.size() > 0 ? actionList.get(0) : "");

            replaceFragment(StatusFragment.class, bundle);
        }
    }
}
