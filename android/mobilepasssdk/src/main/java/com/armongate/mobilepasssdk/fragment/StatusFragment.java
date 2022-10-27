package com.armongate.mobilepasssdk.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.activity.PassFlowActivity;
import com.armongate.mobilepasssdk.constant.LogCodes;
import com.armongate.mobilepasssdk.constant.NeedPermissionType;
import com.armongate.mobilepasssdk.constant.PassFailCode;
import com.armongate.mobilepasssdk.delegate.BluetoothManagerDelegate;
import com.armongate.mobilepasssdk.manager.BluetoothManager;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;
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
            runBluetooth();
        } else if (mActionType.equals(PassFlowActivity.ACTION_REMOTEACCESS)) {
            runRemoteAccess();
        } else {
            LogManager.getInstance().error("Starting pass action has been cancelled due to invalid type", LogCodes.PASSFLOW_ACTION_INVALID_TYPE);
            updateStatus(R.string.text_status_message_error_invalid_action, "", R.drawable.error);
        }
    }

    private void runRemoteAccess() {
        if (mQRCode == null) {
            LogManager.getInstance().warn("Run remote access action has been terminated due to invalid qr code content", LogCodes.PASSFLOW_ACTION_EMPTY_QRCODE_CONTENT);
            onRemoteAccessFailed(null, R.string.text_status_message_error_invalid_qrcode_content, "", PassFailCode.REMOTE_ACCESS_INVALID_QR_CONTENT);
            return;
        }

        if (mQRCode.i == null || mQRCode.i.isEmpty()) {
            LogManager.getInstance().warn("Run remote access action has been terminated due to invalid qr code id", LogCodes.PASSFLOW_ACTION_EMPTY_QRCODE_ID);
            onRemoteAccessFailed(null, R.string.text_status_message_error_invalid_qrcode_id, "", PassFailCode.REMOTE_ACCESS_INVALID_QR_CODE_ID);
            return;
        }

        DelegateManager.getInstance().flowConnectionStateChanged(true);
        BluetoothManager.getInstance().stopScan(true);

        RequestAccess request = new RequestAccess();
        request.q = mQRCode.i;

        new AccessPointService().remoteOpen(request, new BaseService.ServiceResultListener() {
            @Override
            public void onCompleted(Object result) {
                updateStatus( R.string.text_status_message_succeed,"", R.drawable.success);
                onPassCompleted();

                DelegateManager.getInstance().flowConnectionStateChanged(false);
            }

            @Override
            public void onError(int statusCode, String message) {
                onRemoteAccessFailed(statusCode, -1, message, null);
                DelegateManager.getInstance().flowConnectionStateChanged(false);
            }
        });
    }

    private void runBluetooth() {
        if (mQRCode == null) {
            LogManager.getInstance().warn("Run bluetooth action has been terminated due to invalid qr code content", LogCodes.PASSFLOW_ACTION_EMPTY_QRCODE_CONTENT);
            onBluetoothConnectionFailed(R.string.text_status_message_error_invalid_qrcode_content, PassFailCode.BLUETOOTH_INVALID_QR_CONTENT);
            return;
        }

        if (mQRCode.d == null) {
            LogManager.getInstance().warn("Run bluetooth action has been terminated due to invalid direction", LogCodes.PASSFLOW_ACTION_EMPTY_DIRECTION);
            onBluetoothConnectionFailed(R.string.text_status_message_error_invalid_direction, PassFailCode.BLUETOOTH_INVALID_DIRECTION);
            return;
        }

        if (mQRCode.h == null || mQRCode.h.isEmpty()) {
            LogManager.getInstance().warn("Run bluetooth action has been terminated due to invalid hardware id", LogCodes.PASSFLOW_ACTION_EMPTY_HARDWAREID);
            onBluetoothConnectionFailed(R.string.text_status_message_error_invalid_hardware_id, PassFailCode.BLUETOOTH_INVALID_HARDWARE_ID);
            return;
        }

        if (mQRCode.r == null) {
            LogManager.getInstance().warn("Run bluetooth action has been terminated due to invalid relay number", LogCodes.PASSFLOW_ACTION_EMPTY_RELAYNUMBER);
            onBluetoothConnectionFailed(R.string.text_status_message_error_invalid_relay_number, PassFailCode.BLUETOOTH_INVALID_RELAY_NUMBER);
            return;
        }

        BluetoothManager.getInstance().delegate = this;

        if (BluetoothManager.getInstance().getCurrentState().enabled) {
            DelegateManager.getInstance().flowConnectionStateChanged(true);

            updateStatus( R.string.text_status_message_scanning, "", R.drawable.waiting);

            BLEScanConfiguration config = new BLEScanConfiguration(mDevices, ConfigurationManager.getInstance().getMemberId(), mQRCode.h, mQRCode.d, mQRCode.r);

            BluetoothManager.getInstance().startScan(config);
            startConnectionTimer();
        } else {
            if (ConfigurationManager.getInstance().waitForBLEEnabled() || mNextAction == null) {
                LogManager.getInstance().warn("Need permission to continue passing flow, permission type: " + NeedPermissionType.NEED_ENABLE_BLE, LogCodes.NEED_ENABLE_BLE);
                updateStatus( R.string.text_status_message_need_ble_enabled,"", R.drawable.warning);
            } else {
                LogManager.getInstance().info("Bluetooth disabled now and SDK configuration says no need to wait for it. Ignore Bluetooth scanning and continue to next action.");
                onBluetoothConnectionFailed(-1, PassFailCode.BLUETOOTH_DISABLED);
            }
        }
    }

    private void startConnectionTimer() {
        mTimerHandler =  new Handler();
        Runnable mTimerRunnable = new Runnable() {
            public void run() {
                LogManager.getInstance().info("Bluetooth connection timer elapsed");
                DelegateManager.getInstance().flowConnectionStateChanged(false);

                onBluetoothConnectionFailed(-1, PassFailCode.BLUETOOTH_CONNECTION_TIMEOUT);
            }
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

    private void onBluetoothConnectionFailed(int messageId, Integer failCode) {
        onBluetoothFlowCompleted();

        if (mNextAction != null && mNextAction.length() > 0) {
            if (mNextAction.equals(PassFlowActivity.ACTION_LOCATION)) {
                LogManager.getInstance().info("Bluetooth connection failed and now validate user location to continue remote access");
                DelegateManager.getInstance().flowNextActionRequired();
            } else if (mNextAction.equals(PassFlowActivity.ACTION_REMOTEACCESS)) {
                LogManager.getInstance().info("Bluetooth connection failed and now continue for remote access request");
                mActionType = PassFlowActivity.ACTION_REMOTEACCESS;

                updateStatus(R.string.text_status_message_waiting,"", R.drawable.waiting);
                runRemoteAccess();
            } else {
                LogManager.getInstance().warn("Bluetooth connection failed and next action has invalid value", LogCodes.PASSFLOW_ACTION_INVALID_NEXT_ACTION);
                updateStatus(R.string.text_status_message_failed,"", R.drawable.error);
                onPassCompleted(false, PassFailCode.BLUETOOTH_INVALID_NEXT_ACTION);
            }
        } else {
            updateStatus(messageId != -1 ? messageId :  R.string.text_status_message_failed, "", R.drawable.error);
            onPassCompleted(false, failCode != null ? failCode : PassFailCode.BLUETOOTH_CONNECTION_FAILED);
        }
    }

    private void onRemoteAccessFailed(Integer errorCode, int messageId, String message, Integer failCode) {
        if ((errorCode == null || errorCode != 401) && mNextAction != null && mNextAction.equals(PassFlowActivity.ACTION_BLUETOOTH)) {
            LogManager.getInstance().info("Remote access request failed and now continue to Bluetooth scanning");
            updateStatus( R.string.text_status_message_scanning, "",  R.drawable.waiting);
            runBluetooth();
        } else if (errorCode != null) {
            int code = errorCode == 0 ? PassFailCode.REMOTE_ACCESS_COULD_NOT_BE_SENT : PassFailCode.REMOTE_ACCESS_REQUEST_ERROR;

            if (message == null || message.isEmpty()) {
                int failMessage = R.string.text_status_message_failed;

                if (errorCode == 401) {
                    failMessage = R.string.text_status_message_unauthorized;
                    code = PassFailCode.REMOTE_ACCESS_UNAUTHORIZED;
                } else if (errorCode == 404) {
                    failMessage = R.string.text_status_message_not_connected;
                    code = PassFailCode.REMOTE_ACCESS_NOT_CONNECTED;
                } else if (errorCode == 408) {
                    failMessage = R.string.test_status_message_remoteaccess_timeout;
                    code = PassFailCode.REMOTE_ACCESS_TIMEOUT;
                } else if (errorCode == 0) {
                    failMessage = R.string.text_status_message_no_connection;
                }

                updateStatus(failMessage, "", R.drawable.error);
            } else {
                updateStatus(-1, message, R.drawable.error);
            }

            onPassCompleted(false, code);
        } else {
            if (message == null || message.isEmpty()) {
                updateStatus(messageId != -1 ? messageId : R.string.text_status_message_failed, "", R.drawable.error);
            } else {
                updateStatus(-1, message, R.drawable.error);
            }

            onPassCompleted(false, failCode != null ? failCode : PassFailCode.REMOTE_ACCESS_FAILED);
        }
    }

    private void onPassCompleted() {
        this.onPassCompleted(true, null);
    }

    private void onPassCompleted(boolean success, Integer failCode) {
        if (mQRCode != null && mClubInfo != null) {
            DelegateManager.getInstance().onCompleted(success, mQRCode.d, mClubInfo.i, mClubInfo.n, failCode);
        } else {
            DelegateManager.getInstance().onCompleted(success, null, null, null, failCode);
        }
    }

    private void updateStatus(final int messageId, final String messageText, final @Nullable Integer icon) {
        if (mActivity != null) {
            mWaitingUpdate = null;
            mActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
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
                }
            });
        } else {
            mWaitingUpdate = new WaitingStatusUpdate(messageId, messageText, icon);
        }
    }

    @Override
    public void onConnectionStateChanged(DeviceConnectionStatus state) {
        if (state.state != DeviceConnectionStatus.ConnectionState.CONNECTING) {
            this.endConnectionTimer();
        }

        if (state.state == DeviceConnectionStatus.ConnectionState.CONNECTED) {
            updateStatus( R.string.text_status_message_succeed, "", R.drawable.success);
            onPassCompleted();

            onBluetoothConnectionSucceed();
        } else if (state.state == DeviceConnectionStatus.ConnectionState.FAILED
                || state.state == DeviceConnectionStatus.ConnectionState.NOT_FOUND
                || (mLastConnectionState == DeviceConnectionStatus.ConnectionState.CONNECTING && state.state == DeviceConnectionStatus.ConnectionState.DISCONNECTED)) {

            onBluetoothConnectionFailed(-1, PassFailCode.BLUETOOTH_CONNECTION_FAILED);
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
