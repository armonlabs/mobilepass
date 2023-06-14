package com.armongate.mobilepasssdk.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.constant.LogCodes;
import com.armongate.mobilepasssdk.constant.NeedPermissionType;
import com.armongate.mobilepasssdk.constant.PassFlowStateCode;
import com.armongate.mobilepasssdk.constant.QRTriggerType;
import com.armongate.mobilepasssdk.delegate.PassFlowDelegate;
import com.armongate.mobilepasssdk.fragment.CheckFragment;
import com.armongate.mobilepasssdk.fragment.HuaweiMapFragment;
import com.armongate.mobilepasssdk.fragment.GoogleMapFragment;
import com.armongate.mobilepasssdk.fragment.PermissionFragment;
import com.armongate.mobilepasssdk.fragment.StatusFragment;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.manager.PassFlowManager;
import com.armongate.mobilepasssdk.manager.SettingsManager;
import com.armongate.mobilepasssdk.model.QRCodeContent;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PassFlowActivity extends AppCompatActivity implements PassFlowDelegate {

    public PassFlowActivity() {
        super(R.layout.activity_armon_pass_flow);
    }

    public static final String ACTION_BLUETOOTH    = "bluetooth";
    public static final String ACTION_REMOTEACCESS = "remoteAccess";
    public static final String ACTION_LOCATION     = "location";

    private List<String>    actionList          = new ArrayList<>();
    private String          actionCurrent       = "";
    private QRCodeContent   activeQRCodeContent = null;
    private boolean         connectionActive    = false;

    private int     activePermissionCode    = -1;
    private boolean activePermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set flow delegate listener for activities
        DelegateManager.getInstance().setCurrentPassFlowDelegate(this);

        // Set current language for UI
        setLocale(ConfigurationManager.getInstance().getLanguage());

        if (savedInstanceState == null) {
            if (SettingsManager.getInstance().checkCameraPermission(getApplicationContext(), this)) {
                if (ConfigurationManager.getInstance().usingHMS()) {
                    scanQRCodesForHMS();
                } else {
                    scanQRCodesForGMS();
                }
            } else {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_NEED_PERMISSION);
                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.armon_mp_fragment_container, CheckFragment.class, null)
                        .commit();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus && connectionActive) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (activePermissionCode > 0) {
            if (activePermissionCode == SettingsManager.REQUEST_CODE_CAMERA) {
                if (activePermissionGranted) {
                    PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_PERMISSION_GRANTED);

                    if (ConfigurationManager.getInstance().usingHMS()) {
                        scanQRCodesForHMS();
                    } else {
                        scanQRCodesForGMS();
                    }
                } else {
                    PassFlowManager.getInstance().addToStates(PassFlowStateCode.SCAN_QRCODE_PERMISSION_REJECTED);
                    showPermissionMessage(NeedPermissionType.NEED_PERMISSION_CAMERA);
                }
            } else if (activePermissionCode == SettingsManager.REQUEST_CODE_LOCATION) {
                if (activePermissionGranted) {
                    PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_LOCATION_PERMISSION_GRANTED);
                    processAction();
                } else {
                    PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_LOCATION_PERMISSION_REJECTED);
                    showPermissionMessage(NeedPermissionType.NEED_PERMISSION_LOCATION);
                }
            } else if (activePermissionCode == SettingsManager.REQUEST_CODE_BLE_SCAN) {
                if (activePermissionGranted) {
                    PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_PERMISSION_GRANTED);
                    processAction();
                } else {
                    PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_PERMISSION_REJECTED);
                    showPermissionMessage(NeedPermissionType.NEED_PERMISSION_BLUETOOTH);
                }
            }

            activePermissionCode = -1;
            activePermissionGranted = false;
        }
    }

    private void scanQRCodesForGMS() {
        Intent intent = new Intent(this, GoogleQRCodeReaderActivity.class);
        this.startActivity(intent);
    }

    private void scanQRCodesForHMS() {
        Intent intent = new Intent(this, HuaweiQRCodeReaderActivity.class);
        this.startActivity(intent);
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
        super.onBackPressed();
        DelegateManager.getInstance().onCancelled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean permissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        activePermissionCode    = requestCode;
        activePermissionGranted = permissionGranted;

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
    public void onNextActionRequired() {
        checkNextAction();
    }

    @Override
    public void onConnectionStateChanged(boolean isActive) {
        connectionActive = isActive;
    }

    @Override
    public void onFinishRequired() {
        finish();
    }

    private void checkNextAction() {
        LogManager.getInstance().debug("Checking next action now");

        if (this.isFinishing() || this.isDestroyed()) {
            LogManager.getInstance().warn("Checking next action has been cancelled due to activity state: " + (this.isFinishing() ? "finishing" : "destroyed"), LogCodes.UI_INVALID_STATE_TO_REPLACE_FRAGMENT);
            finish();
        } else {
            if (this.actionList.size() > 0) {
                actionCurrent = this.actionList.get(0);
                this.actionList.remove(0);

                processAction();
            } else {
                LogManager.getInstance().error("Checking next action has been cancelled due to empty action list", LogCodes.PASSFLOW_EMPTY_ACTION_LIST, this);
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_ACTION_LIST_EMPTY);
                finish();
            }
        }
    }

    private void replaceFragment(Class<? extends androidx.fragment.app.Fragment> newFragment, @Nullable Bundle args) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.armon_mp_fragment_container, newFragment, args)
                    .replace(R.id.armon_mp_fragment_container, newFragment, args)
                    .setReorderingAllowed(true)
                    .commit();
        } catch (Exception ex) {
            LogManager.getInstance().error("Replace fragment failed with error message: " + ex.getLocalizedMessage(), LogCodes.UI_INVALID_STATE_TO_REPLACE_FRAGMENT, this);
            finish();
        }
    }

    private void showPermissionMessage(int needPermissionType) {
        LogManager.getInstance().warn("Need permission to continue passing flow, permission type: " + needPermissionType, LogCodes.NEED_PERMISSION_DEFAULT + needPermissionType);

        Bundle bundle = new Bundle();
        bundle.putInt("type", needPermissionType);

        replaceFragment(PermissionFragment.class, bundle);
    }

    private void processQRCodeData(String code) {
        activeQRCodeContent = ConfigurationManager.getInstance().getQRCodeContent(code);

        if (activeQRCodeContent != null && activeQRCodeContent.valid) {
            LogManager.getInstance().info("QR code content for [" + code + "] is processing...");

            boolean needLocation = activeQRCodeContent.qrCode != null
                    && activeQRCodeContent.qrCode.v != null
                    && activeQRCodeContent.qrCode.v
                    && activeQRCodeContent.geoLocation != null
                    && activeQRCodeContent.geoLocation.la != null
                    && activeQRCodeContent.geoLocation.lo != null
                    && activeQRCodeContent.geoLocation.r != null;
            LogManager.getInstance().info("QR code need location validation: " + needLocation);

            actionList      = new ArrayList<>();
            actionCurrent   = "";

            if (activeQRCodeContent.qrCode == null || activeQRCodeContent.qrCode.t == null) {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_QRCODE_TRIGGER_TYPE, code);
                LogManager.getInstance().error("Process qr code has been cancelled due to empty trigger type", LogCodes.PASSFLOW_PROCESS_QRCODE_TRIGGERTYPE, this);
            } else {
                switch (activeQRCodeContent.qrCode.t) {
                    case QRTriggerType.Bluetooth:
                        LogManager.getInstance().info("Trigger Type: Bluetooth");
                        actionCurrent = ACTION_BLUETOOTH;

                        break;
                    case QRTriggerType.BluetoothThenRemote:
                        LogManager.getInstance().info("Trigger Type: Bluetooth Then Remote");
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

                        if (activeQRCodeContent.qrCode.t == QRTriggerType.RemoteThenBluetooth) {
                            LogManager.getInstance().info("Trigger Type: Remote Then Bluetooth");
                            actionList.add(ACTION_BLUETOOTH);
                        } else {
                            LogManager.getInstance().info("Trigger Type: Remote");
                        }
                        break;
                    default:
                        LogManager.getInstance().error("Process qr code has been cancelled due to empty action type", LogCodes.PASSFLOW_PROCESS_QRCODE_EMPTY_ACTION, this);
                }

                PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_STARTED);
                processAction();
            }
        } else {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_QRCODE_MISSING_CONTENT, code);
            LogManager.getInstance().error("Process QR Code message received with empty or invalid content", LogCodes.PASSFLOW_EMPTY_QRCODE_CONTENT, this);
        }

    }

    private void processAction() {
        if (actionCurrent.isEmpty()) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_ACTION_TYPE);
            LogManager.getInstance().error("Process qr code has been cancelled due to empty action type", LogCodes.PASSFLOW_PROCESS_QRCODE_EMPTY_ACTION, this);
            return;
        }

        LogManager.getInstance().debug("Current action: " + actionCurrent);
        LogManager.getInstance().debug("Action list: " + actionList.toString());

        boolean needLocationPermission = actionCurrent.equals(ACTION_BLUETOOTH) || actionCurrent.equals(ACTION_LOCATION) || actionList.contains(ACTION_BLUETOOTH) || actionList.contains(ACTION_LOCATION);

        if (needLocationPermission && !SettingsManager.getInstance().checkLocationEnabled(getApplicationContext())) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_LOCATION_NEED_ENABLED);
            showPermissionMessage(NeedPermissionType.NEED_ENABLE_LOCATION_SERVICES);
            return;
        } else {
            if (!needLocationPermission || SettingsManager.getInstance().checkLocationPermission(getApplicationContext(), this)) {
                if (actionCurrent.equals(ACTION_BLUETOOTH)) {
                    if (!SettingsManager.getInstance().checkBluetoothScanPermission(getApplicationContext(), this)) {
                        PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_NEED_PERMISSION);
                        replaceFragment(CheckFragment.class, null);
                        return;
                    }
                }
            } else {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_LOCATION_NEED_PERMISSION);
                replaceFragment(CheckFragment.class, null);
                return;
            }
        }

        if (actionCurrent.equals(ACTION_LOCATION)) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_LOCATION);
            Bundle bundle = new Bundle();

            if (activeQRCodeContent != null && activeQRCodeContent.geoLocation != null) {
                if (activeQRCodeContent.geoLocation.la != null) {
                    bundle.putDouble("latitude", activeQRCodeContent.geoLocation.la);
                }

                if (activeQRCodeContent.geoLocation.lo != null) {
                    bundle.putDouble("longitude", activeQRCodeContent.geoLocation.lo);
                }

                if (activeQRCodeContent.geoLocation.r != null) {
                    bundle.putInt("radius", activeQRCodeContent.geoLocation.r);
                }
            }

            if (ConfigurationManager.getInstance().usingHMS()) {
                replaceFragment(HuaweiMapFragment.class, bundle);
            } else {
                replaceFragment(GoogleMapFragment.class, bundle);
            }
        } else {
            Gson gson = new Gson();
            String deviceDetails = activeQRCodeContent != null ? gson.toJson(activeQRCodeContent.terminals) : "";
            String qrCodeInfo = activeQRCodeContent != null ? gson.toJson(activeQRCodeContent.qrCode) : "";
            String clubInfo = activeQRCodeContent != null ? gson.toJson(activeQRCodeContent.clubInfo) : "";

            Bundle bundle = new Bundle();
            bundle.putString("type", actionCurrent);
            bundle.putString("devices", deviceDetails);
            bundle.putString("qrCode", qrCodeInfo);
            bundle.putString("clubInfo", clubInfo);
            bundle.putString("nextAction", actionList.size() > 0 ? actionList.get(0) : "");

            replaceFragment(StatusFragment.class, bundle);
        }
    }
}

