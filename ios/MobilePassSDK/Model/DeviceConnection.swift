//
//  DeviceConnection.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 28.02.2021.
//

import Foundation
import CoreBluetooth

struct DeviceConnection {
    var peripheral:         CBPeripheral;
    var characteristics:    Dictionary<String, CBCharacteristic> = [:];
    var deviceId:           String?;
    
    init(peripheral: CBPeripheral) {
        self.peripheral = peripheral
    }
}
