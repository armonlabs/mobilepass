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
    var dataUserBarcode:    String
    var qrCodeId:           String
    var dataDirection:      Int
    var hardwareId:         String
    var deviceNumber:       Int
    var relayNumber:        Int
    var language:           Language
    
    init(devices: [ResponseAccessPointListTerminal], userId: String, userBarcode: String, qrCodeId: String, direction: Int, hardwareId: String, relayNumber: Int, language: Language) {
        self.dataUserId         = userId
        self.dataUserBarcode    = userBarcode
        self.dataDirection      = direction
        self.qrCodeId           = qrCodeId
        self.hardwareId         = hardwareId
        self.deviceNumber       = 0 // Default value
        self.relayNumber        = relayNumber
        self.language           = language
        
        self.deviceList = [:]
        
        for device in devices {
            if (device.i != nil && device.p != nil && !device.i!.isEmpty && !device.p!.isEmpty) {
                self.deviceList[device.i!.lowercased()] = DeviceConnectionInfo(deviceId: device.i!, publicKey: device.p!)
            }
        }
    }
}
