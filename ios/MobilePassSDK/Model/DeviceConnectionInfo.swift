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
    
    init(deviceId: String, publicKey: String) {
        self.deviceId   = deviceId
        self.publicKey  = publicKey
    }
}
