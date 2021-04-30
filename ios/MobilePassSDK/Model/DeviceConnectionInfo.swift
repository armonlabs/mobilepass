//
//  DeviceConnectionInfo.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 22.04.2021.
//

import Foundation

struct DeviceConnectionInfo {
    var deviceId:   String;
    var publicKey:  String;
    var hardwareId: String;
    
    init(deviceId: String, publicKey: String, hardwareId: String) {
        self.deviceId   = deviceId
        self.publicKey  = publicKey
        self.hardwareId = hardwareId
    }
}
