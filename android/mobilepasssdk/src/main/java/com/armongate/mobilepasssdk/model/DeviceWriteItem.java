package com.armongate.mobilepasssdk.model;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

public class DeviceWriteItem {

    public DeviceWriteItem(byte[] data, BluetoothGattCharacteristic characteristic, BluetoothGatt connection) {
        this.message        = data;
        this.connection     = connection;
        this.characteristic = characteristic;
        this.useDescriptor  = false;
    }

    public DeviceWriteItem(boolean enableNotification, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, BluetoothGatt connection) {
        this.notifyEnable   = enableNotification;
        this.connection     = connection;
        this.characteristic = characteristic;
        this.descriptor     = descriptor;
        this.useDescriptor  = true;
    }


    public byte[] message;

    public boolean notifyEnable;

    public BluetoothGatt connection;

    public BluetoothGattCharacteristic characteristic;

    public BluetoothGattDescriptor descriptor;

    public boolean useDescriptor;
}
