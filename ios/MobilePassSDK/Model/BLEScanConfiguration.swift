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
    var hardwareId:         String
    var deviceNumber:       Int
    var relayNumber:        Int
    
    init(devices: [ResponseAccessPointListTerminal], userId: String, direction: Int, hardwareId: String, relayNumber: Int) {
        self.dataUserId     = userId
        self.dataDirection  = direction
        self.hardwareId     = hardwareId
        self.deviceNumber   = 0 // Default value
        self.relayNumber    = relayNumber
        
        self.deviceList = [:]
        
        for device in devices {
            if (device.i != nil && device.p != nil && !device.i!.isEmpty && !device.p!.isEmpty) {
                self.deviceList[device.i!.lowercased()] = DeviceConnectionInfo(deviceId: device.i!, publicKey: device.p!)
            }
        }
    }
}
