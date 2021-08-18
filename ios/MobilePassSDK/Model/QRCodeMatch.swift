//
//  QRCodeMatch.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 18.08.2021.
//

import Foundation

struct QRCodeMatch: Codable {
    var accessPointId:  String
    var qrCode:         ResponseAccessPointListQRCode
}
