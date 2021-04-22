//
//  DeviceInRange.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 28.02.2021.
//

import Foundation
import CoreBluetooth

struct DeviceInRange {
    var serviceUUID: String
    var device:      CBPeripheral
    
    init(serviceUUID: String, bluetoothDevice: CBPeripheral) {
        self.serviceUUID    = serviceUUID
        self.device         = bluetoothDevice
    }
}
