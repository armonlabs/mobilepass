package com.armongate.mobilepasssdk.delegate;

import com.armongate.mobilepasssdk.model.DeviceCapability;
import com.armongate.mobilepasssdk.model.DeviceConnectionStatus;
import com.armongate.mobilepasssdk.model.DeviceInRange;
import com.armongate.mobilepasssdk.model.DeviceSignalInfo;

public interface BluetoothManagerDelegate {
    void onConnectionStateChanged(DeviceConnectionStatus state);
}
