//
//  BLEScanConfiguration.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 28.02.2021.
//

import Foundation

public struct BLEScanConfiguration {
    var deviceList:         Dictionary<String, DeviceConnectionInfo>
    var dataUserId:         String
    var dataDirection:      Int
    var deviceNumber:       Int
    var relayNumber:        Int
    
    init(devices: [ResponseAccessPointItemDeviceInfo], userId: String, direction: Int, deviceNumber: Int, relayNumber: Int) {
        self.dataUserId     = userId
        self.dataDirection  = direction
        self.deviceNumber   = deviceNumber
        self.relayNumber    = relayNumber
        
        self.deviceList = [:]
        
        for device in devices {
            self.deviceList[device.id] = DeviceConnectionInfo(deviceId: device.id, publicKey: device.publicKey ?? "", hardwareId: device.hardwareId ?? "")
        }
    }
}
