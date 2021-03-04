package com.armongate.mobilepasssdk.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.armongate.mobilepasssdk.R;
import com.armongate.mobilepasssdk.activity.PassFlowActivity;
import com.armongate.mobilepasssdk.delegate.BluetoothManagerDelegate;
import com.armongate.mobilepasssdk.manager.BluetoothManager;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.manager.DelegateManager;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.model.BLEScanConfiguration;
import com.armongate.mobilepasssdk.model.DeviceConnectionStatus;
import com.armongate.mobilepasssdk.service.AccessPointService;
import com.armongate.mobilepasssdk.service.BaseService;

import java.util.Collections;

public class StatusFragment extends Fragment implements BluetoothManagerDelegate {
    private String  mActionType;
    private String  mDeviceId;
    private String  mAccessPointId;
    private String  mHardwareId;
    private int     mDeviceNumber;
    private int     mRelayNumber;
    private String  mDevicePublicKey;
    private int     mDirection;
    private String  mNextAction;
    private View    mCurrentView;

    private Handler mTimerHandler;


    private DeviceConnectionStatus.ConnectionState mLastConnectionState;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActionType         = getArguments() != null ? getArguments().getString("type") : "";
        mDeviceId           = getArguments() != null ? getArguments().getString("deviceId") : "";
        mAccessPointId      = getArguments() != null ? getArguments().getString("accessPointId") : "";
        mHardwareId         = getArguments() != null ? getArguments().getString("hardwareId") : "";
        mDeviceNumber       = getArguments() != null ? getArguments().getInt("deviceNumber") : 0;
        mRelayNumber        = getArguments() != null ? getArguments().getInt("relayNumber") : 0;
        mDirection          = getArguments() != null ? getArguments().getInt("direction") : 0;
        mDevicePublicKey    = getArguments() != null ? getArguments().getString("publicKey") : "";
        mNextAction         = getArguments() != null ? getArguments().getString("nextAction") : "";

        // Inflate the layout for this fragment
        mCurrentView = inflater.inflate(R.layout.fragment_status, container, false);

        /*
        ImageView gifViewer = mCurrentView.findViewById(R.id.imageGif);
        Glide.with(this).load(mActionType.equals(PassFlowActivity.ACTION_BLUETOOTH) ? R.drawable.scanning : R.drawable.loading).fitCenter().into(gifViewer);
         */

        BluetoothManager.getInstance().delegate = this;

        updateStatus(R.drawable.background_waiting,
                mActionType.equals(PassFlowActivity.ACTION_BLUETOOTH) ? R.string.text_status_message_scanning : R.string.text_status_message_waiting,
                true,
                null);

        startAction();

        return mCurrentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void startAction() {
        if (mActionType.equals(PassFlowActivity.ACTION_BLUETOOTH)) {
            runBluetooth();
        } else {
            runRemoteAccess();
        }
    }

    private void runRemoteAccess() {
        BluetoothManager.getInstance().stopScan(true);

        new AccessPointService().remoteOpen(mAccessPointId, mDirection, new BaseService.ServiceResultListener() {
            @Override
            public void onCompleted(Object result) {
                updateStatus(R.drawable.background_success, R.string.text_status_message_succeed, false, R.drawable.status_succeed);
                DelegateManager.getInstance().flowPassCompleted(true);
            }

            @Override
            public void onError(int statusCode) {
                if (statusCode != 401 && mNextAction != null && mNextAction.equals(PassFlowActivity.ACTION_BLUETOOTH)) {
                    updateStatus(R.drawable.background_waiting, R.string.text_status_message_scanning, true, null);
                    runBluetooth();
                } else {
                    int failMessage = R.string.text_status_message_failed;

                    if (statusCode == 401) {
                        failMessage = R.string.text_status_message_unauthorized;
                    } else if (statusCode == 404) {
                        failMessage = R.string.text_status_message_not_connected;
                    }

                    updateStatus(R.drawable.background_failed, failMessage, false, R.drawable.status_failed);
                    DelegateManager.getInstance().flowPassCompleted(false);
                }
            }
        });
    }

    private void runBluetooth() {
        BLEScanConfiguration config = new BLEScanConfiguration(Collections.singletonList(mDeviceId), mDevicePublicKey, ConfigurationManager.getInstance().getMemberId(), mHardwareId, mDeviceNumber, mRelayNumber);

        BluetoothManager.getInstance().startScan(config);
        startBluetoothTimer();
    }

    private void startBluetoothTimer() {
        mTimerHandler =  new Handler();
        Runnable mTimerRunnable = new Runnable() {
            public void run() {
                onBluetoothConnectionFailed(true);
            }
        };

        mTimerHandler.postDelayed(mTimerRunnable, 10000);
    }

    private void endBluetoothTimer() {
        if (mTimerHandler != null) {
            mTimerHandler.removeCallbacksAndMessages(null);
        }
    }

    private void onBluetoothConnectionFailed(boolean timeout) {
        if (timeout) {
            LogManager.getInstance().debug("Timeout occurred for BLE scanning");
            BluetoothManager.getInstance().stopScan(true);
        }

        if (mNextAction != null && mNextAction.length() > 0) {
            if (mNextAction.equals(PassFlowActivity.ACTION_LOCATION)) {
                DelegateManager.getInstance().flowNextActionRequired();
            } else if (mNextAction.equals(PassFlowActivity.ACTION_REMOTEACCESS)) {
                updateStatus(R.drawable.background_waiting, R.string.text_status_message_waiting, true, null);
                runRemoteAccess();
            }
        } else {
            updateStatus(R.drawable.background_failed, R.string.text_status_message_failed, false, R.drawable.status_failed);
            DelegateManager.getInstance().flowPassCompleted(false);
        }
    }

    private void updateStatus(final int background, final int message, final boolean showSpinner, final @Nullable Integer icon) {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ConstraintLayout statusBackground = mCurrentView.findViewById(R.id.status_background);
                statusBackground.setBackgroundResource(background);

                ProgressBar spinner = mCurrentView.findViewById(R.id.progressBar);
                spinner.setVisibility(showSpinner ? View.VISIBLE : View.GONE);

                ImageView statusIcon = mCurrentView.findViewById(R.id.imgStatusIcon);

                if (icon != null) {
                    statusIcon.setImageResource(icon);
                }

                statusIcon.setVisibility(showSpinner ? View.GONE : View.VISIBLE);

                TextView statusMessage = mCurrentView.findViewById(R.id.txtStatusMessage);
                statusMessage.setText(message);
            }
        });
    }

    @Override
    public void onConnectionStateChanged(DeviceConnectionStatus state) {
        if (state.state != DeviceConnectionStatus.ConnectionState.CONNECTING) {
            this.endBluetoothTimer();
        }

        if (state.state == DeviceConnectionStatus.ConnectionState.CONNECTED) {
            updateStatus(R.drawable.background_success, R.string.text_status_message_succeed, false, R.drawable.status_succeed);
            DelegateManager.getInstance().flowPassCompleted(true);
        } else if (state.state == DeviceConnectionStatus.ConnectionState.FAILED
                || state.state == DeviceConnectionStatus.ConnectionState.NOT_FOUND
                || (mLastConnectionState == DeviceConnectionStatus.ConnectionState.CONNECTING && state.state == DeviceConnectionStatus.ConnectionState.DISCONNECTED)) {

            onBluetoothConnectionFailed(false);
        }

        mLastConnectionState = state.state;
    }

}
