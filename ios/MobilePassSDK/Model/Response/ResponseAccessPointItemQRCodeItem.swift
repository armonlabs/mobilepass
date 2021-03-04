//
//  ResponseAccessPointItemQRCodeItem.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 17.02.2021.
//

import Foundation

struct ResponseAccessPointItemQRCodeItem: Codable {
    var qrCodeData:     String
    var accessPointId:  String?
    var config:         ResponseAccessPointItemQRCodeItemConfig
}
