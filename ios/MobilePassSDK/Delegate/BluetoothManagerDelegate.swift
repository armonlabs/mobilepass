//
//  BluetoothManagerDelegate.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 9.02.2021.
//

public protocol BluetoothManagerDelegate {
    func onConnectionStateChanged(state: DeviceConnectionStatus)
    func onBLEStateChanged(state: DeviceCapability)
}
