//
//  ResponseAccessPointItemQRCodeItemTrigger.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct ResponseAccessPointItemQRCodeItemTrigger: Codable {
    var type:                   QRTriggerType
    var validateGeoLocation:    Bool?
    var userCanChangeTrigger:   Bool?
}
