package com.armongate.mobilepasssdk.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.armongate.mobilepasssdk.model.response.ResponseAccessPointItemDeviceInfo;

import java.util.HashMap;
import java.util.Map;

public class DeviceConnection {

    public BluetoothDevice      peripheral;
    public BluetoothGatt        connection;
    public String               serviceUUID;
    public Map<String, BluetoothGattCharacteristic> characteristics;


    public DeviceConnection(BluetoothDevice device, String serviceUUID, BluetoothGatt connection) {
        this.peripheral         = device;
        this.serviceUUID        = serviceUUID;
        this.connection         = connection;
        this.characteristics    = new HashMap<>();
    }

}
