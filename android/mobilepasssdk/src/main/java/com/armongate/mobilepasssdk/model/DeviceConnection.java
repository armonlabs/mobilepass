package com.armongate.mobilepasssdk.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.HashMap;
import java.util.Map;

public class DeviceConnection {

    public BluetoothDevice  peripheral;
    public BluetoothGatt    connection;
    public String           deviceId;
    public Map<String, BluetoothGattCharacteristic> characteristics;


    public DeviceConnection(BluetoothDevice device, BluetoothGatt connection) {
        this.peripheral         = device;
        this.connection         = connection;
        this.deviceId           = null;
        this.characteristics    = new HashMap<>();
    }

}
