package com.armongate.mobilepasssdk.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.activity.PassFlowActivity;
import com.armongate.mobilepasssdk.constant.BLEFailCode;
import com.armongate.mobilepasssdk.constant.LogCodes;
import com.armongate.mobilepasssdk.constant.NeedPermissionType;
import com.armongate.mobilepasssdk.constant.PassFlowStateCode;
import com.armongate.mobilepasssdk.delegate.BluetoothManagerDelegate;
import com.armongate.mobilepasssdk.enums.Language;
import com.armongate.mobilepasssdk.manager.BluetoothManager;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.manager.PassFlowManager;
import com.armongate.mobilepasssdk.model.BLEScanConfiguration;
import com.armongate.mobilepasssdk.model.DeviceCapability;
import com.armongate.mobilepasssdk.model.DeviceConnectionStatus;
import com.armongate.mobilepasssdk.model.WaitingStatusUpdate;
import com.armongate.mobilepasssdk.model.request.RequestAccess;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListClubInfo;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListQRCode;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListTerminal;
import com.armongate.mobilepasssdk.service.AccessPointService;
import com.armongate.mobilepasssdk.service.BaseService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StatusFragment extends Fragment implements BluetoothManagerDelegate {
    private String  mActionType;
    private String  mNextAction;
    private View    mCurrentView;
    private ResponseAccessPointListQRCode           mQRCode;
    private ResponseAccessPointListClubInfo         mClubInfo;
    private List<ResponseAccessPointListTerminal>   mDevices;

    private Handler mTimerHandler;

    private FragmentActivity    mActivity;
    private WaitingStatusUpdate mWaitingUpdate = null;

    private DeviceConnectionStatus.ConnectionState mLastConnectionState;
    private boolean mLastBluetoothState = BluetoothManager.getInstance().getCurrentState().enabled;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Gson gson = new Gson();

        String devicesJson = getArguments() != null && getArguments().containsKey("devices") ? getArguments().getString("devices") : "";
        Type typeDeviceList = new TypeToken<List<ResponseAccessPointListTerminal>>(){}.getType();

        String qrCodesJson = getArguments() != null && getArguments().containsKey("qrCode") ? getArguments().getString("qrCode") : "";
        Type typeQRCodeList = new TypeToken<ResponseAccessPointListQRCode>(){}.getType();

        String clubInfoJson = getArguments() != null && getArguments().containsKey("clubInfo") ? getArguments().getString("clubInfo") : "";
        Type typeClubInfo = new TypeToken<ResponseAccessPointListClubInfo>(){}.getType();

        if (devicesJson != null && !devicesJson.isEmpty()) {
            mDevices = gson.fromJson(devicesJson, typeDeviceList);
        } else {
            mDevices = new ArrayList<>();
        }

        if (qrCodesJson != null && !qrCodesJson.isEmpty()) {
            mQRCode = gson.fromJson(qrCodesJson, typeQRCodeList);
        } else {
            mQRCode = null;
        }

        if (clubInfoJson != null && !clubInfoJson.isEmpty()) {
            mClubInfo = gson.fromJson(clubInfoJson, typeClubInfo);
        } else {
            mClubInfo = null;
        }

        mActionType = getArguments() != null && getArguments().containsKey("type") ? getArguments().getString("type") : "";
        mNextAction = getArguments() != null && getArguments().containsKey("nextAction") ? getArguments().getString("nextAction") : "";

        // Inflate the layout for this fragment
        mCurrentView = inflater.inflate(R.layout.fragment_armon_status, container, false);

        updateStatus(mActionType.equals(PassFlowActivity.ACTION_BLUETOOTH) ? R.string.text_status_message_scanning : R.string.text_status_message_waiting,
                "",
                R.drawable.waiting);


        Button button = mCurrentView.findViewById(R.id.armon_mp_btnBLESuggestionSettings);
        button.setOnClickListener(v -> {
            DelegateManager.getInstance().goToSettings();

            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);

            try {
                Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                intent.setData(uri);
            } catch (Exception ex) {
                LogManager.getInstance().error("Exception occurred while adding intent data for package name, error: " + ex.getLocalizedMessage(), LogCodes.NEED_PERMISSION_DEFAULT + NeedPermissionType.NEED_PERMISSION_BLUETOOTH);
            }

            startActivity(intent);
        });

        startAction();

        return mCurrentView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof FragmentActivity){
            mActivity = (FragmentActivity)context;

            if (mWaitingUpdate != null) {
                updateStatus(mWaitingUpdate.messageId, mWaitingUpdate.messageText, mWaitingUpdate.icon);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;

        endConnectionTimer();

        BluetoothManager.getInstance().delegate = null;
        BluetoothManager.getInstance().stopScan(false);
    }

    private void startAction() {
        if (mActionType.equals(PassFlowActivity.ACTION_BLUETOOTH)) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_BLUETOOTH);
            runBluetooth();
        } else if (mActionType.equals(PassFlowActivity.ACTION_REMOTEACCESS)) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_REMOTE_ACCESS);
            runRemoteAccess();
        } else {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_ACTION_TYPE);
            LogManager.getInstance().error("Starting pass action has been cancelled due to invalid type", LogCodes.PASSFLOW_ACTION_INVALID_TYPE);

            updateStatus(R.string.text_status_message_error_invalid_action, "", R.drawable.error);
        }
    }

    private void runRemoteAccess() {
        PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_STARTED);

        if (mQRCode == null) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_REMOTE_ACCESS_QRCODE_DATA);
            LogManager.getInstance().warn("Run remote access action has been terminated due to invalid qr code content", LogCodes.PASSFLOW_ACTION_EMPTY_QRCODE_CONTENT);

            onRemoteAccessFailed(null, R.string.text_status_message_error_invalid_qrcode_content, "");
            return;
        }

        if (mQRCode.i == null || mQRCode.i.isEmpty()) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_REMOTE_ACCESS_QRCODE_ID);
            LogManager.getInstance().warn("Run remote access action has been terminated due to invalid qr code id", LogCodes.PASSFLOW_ACTION_EMPTY_QRCODE_ID);

            onRemoteAccessFailed(null, R.string.text_status_message_error_invalid_qrcode_id, "");
            return;
        }

        DelegateManager.getInstance().flowConnectionStateChanged(true);
        BluetoothManager.getInstance().stopScan(true);

        RequestAccess request = new RequestAccess();
        request.q = mQRCode.i;

        PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST);

        new AccessPointService().remoteOpen(request, new BaseService.ServiceResultListener() {
            @Override
            public void onCompleted(Object result) {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST_SUCCEED);

                updateStatus( R.string.text_status_message_succeed,"", R.drawable.success);
                onPassCompleted(true);

                DelegateManager.getInstance().flowConnectionStateChanged(false);
            }

            @Override
            public void onError(int statusCode, String message) {
                onRemoteAccessFailed(statusCode, -1, message);
                DelegateManager.getInstance().flowConnectionStateChanged(false);
            }
        });
    }

    private void runBluetooth() {
        PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_STARTED);

        if (mQRCode == null || mQRCode.i == null || mQRCode.i.isEmpty()) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_BLUETOOTH_QRCODE_DATA);
            LogManager.getInstance().warn("Run bluetooth action has been terminated due to invalid qr code content", LogCodes.PASSFLOW_ACTION_EMPTY_QRCODE_CONTENT);

            onBluetoothConnectionFailed(R.string.text_status_message_error_invalid_qrcode_content, "", -1);
            return;
        }

        if (mQRCode.d == null) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_BLUETOOTH_DIRECTION);
            LogManager.getInstance().warn("Run bluetooth action has been terminated due to invalid direction", LogCodes.PASSFLOW_ACTION_EMPTY_DIRECTION);

            onBluetoothConnectionFailed(R.string.text_status_message_error_invalid_direction, "", -1);
            return;
        }

        if (mQRCode.h == null || mQRCode.h.isEmpty()) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_BLUETOOTH_HARDWARE_ID);
            LogManager.getInstance().warn("Run bluetooth action has been terminated due to invalid hardware id", LogCodes.PASSFLOW_ACTION_EMPTY_HARDWAREID);

            onBluetoothConnectionFailed(R.string.text_status_message_error_invalid_hardware_id, "", -1);
            return;
        }

        if (mQRCode.r == null) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_BLUETOOTH_RELAY_NUMBER);
            LogManager.getInstance().warn("Run bluetooth action has been terminated due to invalid relay number", LogCodes.PASSFLOW_ACTION_EMPTY_RELAYNUMBER);

            onBluetoothConnectionFailed(R.string.text_status_message_error_invalid_relay_number, "", -1);
            return;
        }

        BluetoothManager.getInstance().delegate = this;

        if (BluetoothManager.getInstance().getCurrentState().enabled) {
            DelegateManager.getInstance().flowConnectionStateChanged(true);

            updateStatus( R.string.text_status_message_scanning, "", R.drawable.waiting);

            BLEScanConfiguration config = new BLEScanConfiguration(mDevices, ConfigurationManager.getInstance().getMemberId(), ConfigurationManager.getInstance().getBarcodeId(), mQRCode.i, mQRCode.h, mQRCode.d, mQRCode.r, Objects.equals(ConfigurationManager.getInstance().getLanguage(), "en") ? Language.EN : Language.TR);

            PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_START_SCAN);

            BluetoothManager.getInstance().startScan(config);
            startConnectionTimer();
        } else {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_BLUETOOTH_NEED_ENABLED);

            if (ConfigurationManager.getInstance().waitForBLEEnabled() || mNextAction == null) {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_OFF_WAITING);
                LogManager.getInstance().warn("Need permission to continue passing flow, permission type: " + NeedPermissionType.NEED_ENABLE_BLE, LogCodes.NEED_ENABLE_BLE);

                updateStatus( R.string.text_status_message_need_ble_enabled,"", R.drawable.warning);
            } else {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_OFF_NO_WAIT);
                LogManager.getInstance().info("Bluetooth disabled now and SDK configuration says no need to wait for it. Ignore Bluetooth scanning and continue to next action.");

                onBluetoothConnectionFailed(-1, "", -1);
            }
        }
    }

    private void startConnectionTimer() {
        mTimerHandler =  new Handler();
        Runnable mTimerRunnable = () -> {
            LogManager.getInstance().info("Bluetooth connection timer elapsed");
            DelegateManager.getInstance().flowConnectionStateChanged(false);

            PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_TIMEOUT);
            onBluetoothConnectionFailed(-1, "", -1);
        };

        mTimerHandler.postDelayed(mTimerRunnable, ConfigurationManager.getInstance().getBLEConnectionTimeout() * 1000);
    }

    private void endConnectionTimer() {
        if (mTimerHandler != null) {
            mTimerHandler.removeCallbacksAndMessages(null);
        }
    }

    private void onBluetoothFlowCompleted() {
        DelegateManager.getInstance().flowConnectionStateChanged(false);

        BluetoothManager.getInstance().delegate = null;
        BluetoothManager.getInstance().stopScan(true);
    }

    private void onBluetoothConnectionSucceed() {
        onBluetoothFlowCompleted();
    }

    private void onBluetoothConnectionFailed(int messageId, String messageText, int failCode) {
        onBluetoothFlowCompleted();

        if ((failCode == BLEFailCode.BenefitsInvalidCard || failCode == BLEFailCode.BenefitsLimitReached || failCode == BLEFailCode.PerfectGymNoAccess)
            && messageText != null && !messageText.isEmpty()) {
            updateStatus(-1, messageText, R.drawable.error);
            onPassCompleted(false);
        } else {
            if (mNextAction != null && mNextAction.length() > 0) {
                if (mNextAction.equals(PassFlowActivity.ACTION_LOCATION)) {
                    LogManager.getInstance().info("Bluetooth connection failed and now validate user location to continue remote access");
                    DelegateManager.getInstance().flowNextActionRequired();
                } else if (mNextAction.equals(PassFlowActivity.ACTION_REMOTEACCESS)) {
                    LogManager.getInstance().info("Bluetooth connection failed and now continue for remote access request");

                    mActionType = PassFlowActivity.ACTION_REMOTEACCESS;
                    PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_REMOTE_ACCESS);

                    updateStatus(R.string.text_status_message_waiting, "", R.drawable.waiting);
                    runRemoteAccess();
                } else {
                    PassFlowManager.getInstance().addToStates(PassFlowStateCode.INVALID_ACTION_TYPE);

                    LogManager.getInstance().warn("Bluetooth connection failed and next action has invalid value", LogCodes.PASSFLOW_ACTION_INVALID_NEXT_ACTION);
                    updateStatus(R.string.text_status_message_failed, "", R.drawable.error);
                    onPassCompleted(false);
                }
            } else {
                updateStatus(messageId != -1 ? messageId : R.string.text_status_message_failed, "", R.drawable.error);
                onPassCompleted(false);
            }
        }
    }

    private void onRemoteAccessFailed(Integer errorCode, int messageId, String message) {
        if (errorCode != null) {
            if (errorCode == 401) {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_UNAUTHORIZED, message);
            } else if (errorCode == 404) {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_DEVICE_NOT_CONNECTED, message);
            } else if (errorCode == 408) {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_DEVICE_TIMEOUT, message);
            } else if (errorCode == 0) {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_NO_NETWORK, message);
            } else {
                PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_REQUEST_FAILED, message);
            }
        }

        if ((errorCode == null || errorCode != 401) && mNextAction != null && mNextAction.equals(PassFlowActivity.ACTION_BLUETOOTH)) {
            LogManager.getInstance().info("Remote access request failed and now continue to Bluetooth scanning");
            
            mActionType = PassFlowActivity.ACTION_BLUETOOTH;
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.PROCESS_ACTION_BLUETOOTH);

            updateStatus( R.string.text_status_message_scanning, "",  R.drawable.waiting);
            runBluetooth();
        } else if (errorCode != null) {
            if (message == null || message.isEmpty()) {
                int failMessage = R.string.text_status_message_failed;

                if (errorCode == 401) {
                    failMessage = R.string.text_status_message_unauthorized;
                } else if (errorCode == 404) {
                    failMessage = R.string.text_status_message_not_connected;
                } else if (errorCode == 408) {
                    failMessage = R.string.test_status_message_remoteaccess_timeout;
                } else if (errorCode == 0) {
                    failMessage = R.string.text_status_message_no_connection;
                }

                updateStatus(failMessage, "", R.drawable.error, errorCode != 401 && (!BluetoothManager.getInstance().getCurrentState().enabled || BluetoothManager.getInstance().getCurrentState().needAuthorize));
            } else {
                updateStatus(-1, message, R.drawable.error, errorCode != 401 && (!BluetoothManager.getInstance().getCurrentState().enabled || BluetoothManager.getInstance().getCurrentState().needAuthorize));
            }

            onPassCompleted(false);
        } else {
            if (message == null || message.isEmpty()) {
                updateStatus(messageId != -1 ? messageId : R.string.text_status_message_failed, "", R.drawable.error, !BluetoothManager.getInstance().getCurrentState().enabled || BluetoothManager.getInstance().getCurrentState().needAuthorize);
            } else {
                updateStatus(-1, message, R.drawable.error, !BluetoothManager.getInstance().getCurrentState().enabled || BluetoothManager.getInstance().getCurrentState().needAuthorize);
            }

            onPassCompleted(false);
        }
    }

    private void onPassCompleted(boolean success) {
        if (mActionType.equals(PassFlowActivity.ACTION_BLUETOOTH)) {
            PassFlowManager.getInstance().addToStates(success ? PassFlowStateCode.RUN_ACTION_BLUETOOTH_PASS_SUCCEED : PassFlowStateCode.RUN_ACTION_BLUETOOTH_PASS_FAILED);
        } else {
            PassFlowManager.getInstance().addToStates(success ? PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_PASS_SUCCEED : PassFlowStateCode.RUN_ACTION_REMOTE_ACCESS_PASS_FAILED);
        }

        if (mQRCode != null && mClubInfo != null) {
            DelegateManager.getInstance().onCompleted(success, !mActionType.equals(PassFlowActivity.ACTION_BLUETOOTH), mQRCode.d, mClubInfo.i, mClubInfo.n);
        } else {
            DelegateManager.getInstance().onCompleted(success, !mActionType.equals(PassFlowActivity.ACTION_BLUETOOTH));
        }
    }

    private void updateStatus(final int messageId, final String messageText, final @Nullable Integer icon) {
        this.updateStatus(messageId, messageText, icon, false);
    }

    private void updateStatus(final int messageId, final String messageText, final @Nullable Integer icon, final boolean suggest) {
        if (mActivity != null) {
            mWaitingUpdate = null;
            mActivity.runOnUiThread(() -> {
                ImageView statusIcon = mCurrentView.findViewById(R.id.armon_mp_imgStatusIcon);

                if (icon != null) {
                    statusIcon.setImageResource(icon);
                }

                TextView statusMessage = mCurrentView.findViewById(R.id.armon_mp_txtStatusMessage);
                if (messageId != -1) {
                    statusMessage.setText(messageId);
                } else {
                    statusMessage.setText(messageText);
                }

                TextView bleSuggestion = mCurrentView.findViewById(R.id.armon_mp_txtBLESuggestionMessage);
                TextView bleSuggestionAction = mCurrentView.findViewById(R.id.armon_mp_txtBLESuggestionActionMessage);

                Button bleSettings = mCurrentView.findViewById(R.id.armon_mp_btnBLESuggestionSettings);

                if (BluetoothManager.getInstance().getCurrentState().needAuthorize) {
                    bleSuggestionAction.setText(R.string.text_status_message_suggestion_authorize);
                } else {
                    bleSuggestionAction.setText(R.string.text_status_message_suggestion_enable);
                }

                bleSuggestion.setVisibility(suggest ? View.VISIBLE : View.GONE);
                bleSuggestionAction.setVisibility(suggest ? View.VISIBLE : View.GONE);
                bleSettings.setVisibility(suggest && BluetoothManager.getInstance().getCurrentState().needAuthorize ? View.VISIBLE : View.GONE);
            });
        } else {
            mWaitingUpdate = new WaitingStatusUpdate(messageId, messageText, icon);
        }
    }

    @Override
    public void onConnectionStateChanged(DeviceConnectionStatus state) {
        if (state.state != DeviceConnectionStatus.ConnectionState.CONNECTING) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_CONNECTING);
            this.endConnectionTimer();
        }

        if (state.state == DeviceConnectionStatus.ConnectionState.CONNECTED) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_CONNECTED);

            updateStatus(R.string.text_status_message_succeed, "", R.drawable.success);
            onPassCompleted(true);

            onBluetoothConnectionSucceed();
        } else if (state.state == DeviceConnectionStatus.ConnectionState.FAILED
                || state.state == DeviceConnectionStatus.ConnectionState.NOT_FOUND
                || (mLastConnectionState == DeviceConnectionStatus.ConnectionState.CONNECTING && state.state == DeviceConnectionStatus.ConnectionState.DISCONNECTED)) {
            PassFlowManager.getInstance().addToStates(PassFlowStateCode.RUN_ACTION_BLUETOOTH_CONNECTION_FAILED);
            onBluetoothConnectionFailed(-1, state.failMessage, state.failReason);
        }

        mLastConnectionState = state.state;
    }

    @Override
    public void onBLEStateChanged(DeviceCapability state) {
        // Run Bluetooth if current action is matched and Bluetooth enabled newly
        if (mActionType != null && mActionType.equals(PassFlowActivity.ACTION_BLUETOOTH) && !mLastBluetoothState && state.enabled) {
            mLastBluetoothState = true;

            if (isVisible()) {
                runBluetooth();
            }
        } else {
            mLastBluetoothState = state.enabled;
        }
    }
}
