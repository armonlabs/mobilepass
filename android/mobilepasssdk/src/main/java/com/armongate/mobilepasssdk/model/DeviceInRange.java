package com.armongate.mobilepasssdk.model;

import android.bluetooth.BluetoothDevice;

public class DeviceInRange {

    public String           serviceUUID;
    public BluetoothDevice  device;

    public DeviceInRange(String serviceUUID, BluetoothDevice bluetoothDevice) {
        this.serviceUUID    = serviceUUID;
        this.device         = bluetoothDevice;
    }

}
