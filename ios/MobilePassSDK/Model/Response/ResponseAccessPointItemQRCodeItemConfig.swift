//
//  ResponseAccessPointItemQRCodeConfig.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct ResponseAccessPointItemQRCodeItemConfig: Codable {
    var direction:      Direction
    var deviceNumber:   Int
    var relayNumber:    Int
    var trigger:        ResponseAccessPointItemQRCodeItemTrigger
}
