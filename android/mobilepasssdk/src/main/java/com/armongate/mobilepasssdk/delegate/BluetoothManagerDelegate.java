package com.armongate.mobilepasssdk.delegate;

import com.armongate.mobilepasssdk.model.DeviceCapability;
import com.armongate.mobilepasssdk.model.DeviceConnectionStatus;

public interface BluetoothManagerDelegate {
    void onConnectionStateChanged(DeviceConnectionStatus state);
    void onBLEStateChanged(DeviceCapability state);
}
