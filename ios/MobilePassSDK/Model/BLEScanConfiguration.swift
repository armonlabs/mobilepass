//
//  BLEScanConfiguration.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 28.02.2021.
//

import Foundation

public struct BLEScanConfiguration: Codable {
    var uuidFilter:         [String]
    var dataUserId:         String
    var dataHardwareId:     String
    var dataDirection:      Int
    var devicePublicKey:    String
    var deviceNumber:       Int
    var relayNumber:        Int
}
